/*

    Copyright (C) 2018 Mauricio Bustos (m@bustos.org)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package org.bustos.whowon

import java.io.{BufferedWriter, File, FileInputStream, FileWriter}
import java.nio.file.Paths

import com.google.cloud.vision.v1.Feature.Type
import com.google.cloud.vision.v1.{AnnotateImageRequest, Feature, Image, ImageAnnotatorClient}
import com.google.protobuf.ByteString
import org.bustos.whowon.WhoWonTables.Bet
import org.slf4j.LoggerFactory

import scala.util.Properties.envOrElse
import scala.collection.JavaConversions._
import scala.util.matching.Regex

class OcrAPI {

  val logger = LoggerFactory.getLogger("who-won")

  // Bet Types
  val UndOvr = ".* (-?[0-9]+) (UND|OVR) ([0-9]+\\.?[0-9]+).*".r
  val MLBodds = ".* ([0-9]+)/([0-9]+) [H|M]L[B|E].*".r
  val MLBml = ".* ([+|-][0-9]+) [H|M]L[B|E].*".r
  val SB = """.* PLB ([+-t]?[0-9]+)[\.|,]?[ ]?([0-9]+)? .*""".r
  val SBalt = """.* ([+-t]?[0-9]+)[\.|,]?[ ]?([0-9]+)? .* PLB""".r
  val SBalt2 = """.* ([+-t]?[0-9]+)[\.|,]?[ ]?([0-9]+)? PLB .*""".r
  val FirstHalf = """.*(1ST HALF COLLEGE BASKETBALL) ([0-9]+) .*""".r
  val FirstTo15 = """.*(1ST TO SCR 15 PTS) ([0-9]+) .*""".r
  val FirstTo15Alt = """.*(1ST TO 15 POINTS).+?([0-9]+).*""".r
  val GameId = ".*ROUND ([0-9]+) .*".r
  val GameIdAlt = ".* Payout: \\$[0-9]+\\.[0-9]+ ([0-9]+) .*".r

  val Cost = """.*Ticket [C|c|P][しoa]st:? (\$[0-9]+[\.|,] ?[0-9]+).*""".r
  val ToPay = ".*to pay (\\$[0-9]+[\\.|,][0-9]+).*".r
  val IdAndTeam = ".*ROUND ([0-9]+) .*? [-|\\+].*".r

  val credentials = envOrElse("GOOGLE_APPLICATION_CREDENTIALS_FILE", "")
  if (!credentials.isEmpty) {
    logger.info("Writing out GOOGLE_APPLICATION_CREDENTIALS to /tmp/google_application_credentials.json")
    val CredentialsFileName = "/tmp/google_application_credentials.json"
    val file = new File(CredentialsFileName)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(credentials)
    bw.close()
  }

  def checkRegex(text: String, check: String, regex: Regex) = {
    text match {
      case regex(entry) => {
        val repairedEntry = {
          if (check.contains("$")) entry.replace(",", ".")
          else entry
        }
        val noSpace = repairedEntry.filter(_ != ' ')
        if (repairedEntry == check || noSpace == check) {
        } else {
          throw new IllegalArgumentException(entry + " != " + check)
        }
      }
      case _ => {
        throw new IllegalArgumentException(check)
      }
    }
  }

  def imageOCR(filePath: String, textPath: String): String = {
    //val imgBytes = ByteString.readFrom(new FileInputStream(smallName))
    val imgBytes = ByteString.readFrom(new FileInputStream(filePath))
    val img = Image.newBuilder.setContent(imgBytes).build
    val feat = Feature.newBuilder.setType(Type.DOCUMENT_TEXT_DETECTION).build
    val requests = List(AnnotateImageRequest.newBuilder.addFeatures(feat).setImage(img).build)
    try {
      val client = ImageAnnotatorClient.create
      logger.info("Sending...")
      val response = client.batchAnnotateImages(requests)
      val responses = response.getResponsesList
      logger.info("Response")
      if (responses.size == 1) {
        val res = responses(0)
        if (res.hasError) {
          throw new IllegalArgumentException
        }
        val text = res.getFullTextAnnotation.getText.replace('\n', ' ')

        val directory = new File(Paths.get(textPath).getParent.toString)
        if (!directory.exists) {
          directory.mkdir
        }

        val bw = new BufferedWriter(new FileWriter(textPath))

        bw.write(text)
        bw.close()
        text
      } else {
        throw new IllegalArgumentException
      }
    } catch {
      case e: Throwable => {
        logger.error("Unable to communicate with GCloud Vision API\n" + e.getMessage)
        ""
      }
    }
  }

  def ticketText(filePath: String): String = {
    val smallName = filePath.replaceAll(".jpg", "_small.jpg").replaceAll(".png", "_small.png").replaceAll("original", "resized")
    val textName = new File(filePath.replaceAll(".jpg", ".txt").replaceAll(".png", ".txt").replaceAll("original", "original_text"))
    val resizedTextName = new File(filePath.replaceAll(".jpg", ".txt").replaceAll(".png", ".txt").replaceAll("original", "resized_text"))
    if (textName.exists) {
      scala.io.Source.fromFile(textName).mkString
    } else {
      //val newImage = resize(filePath, smallName, 0.75)
      val originalText = imageOCR(filePath, textName.getPath)
      //val resizedText = imageOCR(smallName, resizedTextName.getPath)
      //if (originalText != resizedText) {
      //  logger.error("Sized texts do not match")
      //  logger.error(originalText)
      //  logger.error(resizedText)
      //}
      originalText
    }
  }

  def checkGameId(text: String, bet: Bet): Bet = {
    val id =
      text match {
        case GameId(id) => {
          id.toInt
        }
        case GameIdAlt(id) => {
          id.toInt
        }
        case FirstHalf(name, id) => {
          id.toInt
        }
        case FirstTo15(name, id) => {
          id.toInt
        }
        case FirstTo15Alt(name, id) => {
          id.toInt
        }
        case default => {
          logger.error("No game id")
          0
        }
      }
    Bet(bet.userName, id.toInt, 0, bet.spread_ml, bet.amount, bet.betType, bet.timestamp)
  }

  def checkCost(text: String, bet: Bet): Bet = {
    val amount =
      text match {
        case Cost(amount) => {
          val cleanAmount = amount.replaceAll(" ", "").replaceAll("\\$", "")
          cleanAmount.toDouble
        }
        case default => {
          logger.error("No amount detected")
          0.0
        }
      }
    Bet(bet.userName, bet.bookId, 0, bet.spread_ml, amount, bet.betType, bet.timestamp)
  }

  def detectedBet(filePath: String): Bet = {
    val text = ticketText(filePath)
    logger.info(filePath)
    logger.info(text)
    val bet = checkCost(text, checkGameId(text, Bet("mauricio", 0, 0, 0.0, 0.0, "", null)))
    text match {
      case FirstHalf(name, id) =>
        logger.info("1st Half")
        text match {
          case MLBml(ml) =>
            logger.info("Moneyline")
            Bet(bet.userName, id.toInt, 0, ml.toDouble, bet.amount, "ML-1H", bet.timestamp)
          case default => {
            logger.error("No moneyline found")
            bet
          }
        }
      case FirstTo15Alt(name, id) =>
        logger.info("1st to 15 Alt")
        text match {
          case MLBml(ml) =>
            logger.info("Moneyline")
            Bet(bet.userName, id.toInt, 0, ml.toDouble, bet.amount, "ML-15", bet.timestamp)
          case default => {
            logger.error("No moneyline found")
            bet
          }
        }
      case FirstTo15(name, id) =>
        logger.info("1st to 15")
        text match {
          case MLBml(ml) =>
            logger.info("Moneyline")
            Bet(bet.userName, id.toInt, 0, ml.toDouble, bet.amount, "ML-15", bet.timestamp)
          case default => {
            logger.error("No moneyline found")
            bet
          }
        }
      case UndOvr(ml, ovrUnd, points) =>
        logger.info("Overunder")
        val overUnderString = if (ovrUnd == "OVR") "OV" else "UN"
        Bet(bet.userName, bet.bookId, 0, points.toDouble, bet.amount, "ML-" + overUnderString, bet.timestamp)
      case MLBodds(num, den) =>
        logger.info("Moneyline with odds")
        val ml =
          if (num > den) {
            num.toDouble / den.toDouble * 100.0
          } else {
            -1.0 * num.toDouble / den.toDouble * 100.0
          }
        Bet(bet.userName, bet.bookId, 0, ml, bet.amount, "ML", bet.timestamp)
      case MLBml(ml) =>
        logger.info("Moneyline")
        Bet(bet.userName, bet.bookId, 0, ml.toDouble, bet.amount, "ML", bet.timestamp)
      case SB(spread, halfPoint) =>
        logger.info("Straight Bet")
        val totalSpread = {
          val fixedSpread = spread.replaceAll("t", "")
          if (halfPoint != null) (fixedSpread + "." + halfPoint).toFloat
          else fixedSpread.toFloat
        }
        Bet(bet.userName, bet.bookId, 0, totalSpread, bet.amount, "ST", bet.timestamp)
      case SBalt(spread, halfPoint) =>
        logger.info("Straight Bet Alt")
        val totalSpread = {
          val fixedSpread = spread.replaceAll("t", "")
          if (halfPoint != null) (fixedSpread + "." + halfPoint).toFloat
          else fixedSpread.toFloat
        }
        Bet(bet.userName, bet.bookId, 0, totalSpread, bet.amount, "ST", bet.timestamp)
      case SBalt2(spread, halfPoint) =>
        logger.info("Straight Bet Alt")
        val totalSpread = {
          val fixedSpread = spread.replaceAll("t", "")
          if (halfPoint != null) (fixedSpread + "." + halfPoint).toFloat
          else fixedSpread.toFloat
        }
        Bet(bet.userName, bet.bookId, 0, totalSpread, bet.amount, "ST", bet.timestamp)
      case default => {
        logger.error("NO MATCH")
        Bet(bet.userName, bet.bookId, 0, bet.spread_ml, bet.amount, "UNKNOWN", bet.timestamp)
      }
    }
  }

}