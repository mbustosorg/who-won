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

import java.io.{BufferedReader, File, InputStreamReader}

import com.github.tototoshi.csv.CSVReader
import org.bustos.whowon.WhoWonData.s3
import org.scalatest.{Matchers, WordSpec}

class OCRtest extends WordSpec with Matchers {

  val bucket = "who-won-test-cases"
  val ocrApi = new OcrAPI

  "The OCR system " should {

    val inputReader = new BufferedReader(new InputStreamReader(s3.getObject(bucket, "ticketLabels.csv").getObjectContent))
    val reader = CSVReader.open(inputReader)
    reader.allWithHeaders.filter(fields => fields("defaced") != "YES").foreach(fields => {
      fields("year") + " " + fields("filename") + " be able to validate OCF " in {
        val spread = if (fields("spread").length == 0) fields("moneyline").toFloat else fields("spread").toFloat
        val betType = if (fields("overunder").length == 0) fields("betType") else fields("betType") + "-" + fields("overunder")
        val trainBet = WhoWonTables.Bet("mauricio", fields("id").toInt, 0, spread, fields("amount").replace("$", "").toFloat, betType, null)
        import com.amazonaws.services.s3.model.GetObjectRequest
        s3.getObject(new GetObjectRequest(bucket, "data/" + fields("year") + "/original/" + fields("filename")), new File("/tmp/" + fields("year") + "_" + fields("filename")))
        val bet = ocrApi.detectedBet("/tmp/" + fields("year") + "_" + fields("filename"))._1
        assert(bet.bookId == trainBet.bookId)
        assert(bet.amount == trainBet.amount)
        assert(bet.betType == trainBet.betType)
        assert(bet.spread_ml == trainBet.spread_ml)
      }
    })
  }

}
