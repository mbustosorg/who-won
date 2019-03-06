/*

    Copyright (C) 2019 Mauricio Bustos (m@bustos.org)

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

import java.io.File

import com.github.tototoshi.csv.CSVReader
import org.bustos.whowon.WhoWonData.logger
import org.scalatest.{Matchers, WordSpec}

class OCRtest extends WordSpec with Matchers {

  val dataRoot = "/Users/mauricio/Google Drive/Projects/who-won/"
  val ocrApi = new OcrAPI

  "The OCR system " should {
    val reader = CSVReader.open(new File(dataRoot + "tickets/ticketLabels.csv"))
    reader.allWithHeaders.foreach(fields => {
      logger.info(fields.toString)
      fields("year") + " " + fields("filename") + " be able to read ticket image file " in {
        val spread = if (fields("spread").length == 0) fields("moneyline").toFloat else fields("spread").toFloat
        val betType = if (fields("overunder").length == 0) fields("betType") else fields("betType") + "-" + fields("overunder")
        val trainBet = WhoWonTables.Bet("mauricio", fields("id").toInt, 0, spread, fields("amount").replace("$", "").toFloat, betType, null)
        val bet = ocrApi.detectedBet(dataRoot + "tickets/data/" + fields("year") + "/original/" + fields("filename"))
        assert(bet.bookId == trainBet.bookId)
        assert(bet.amount == trainBet.amount)
        assert(bet.betType == trainBet.betType)
        assert(bet.spread_ml == trainBet.spread_ml)
      }
    })
  }

}
