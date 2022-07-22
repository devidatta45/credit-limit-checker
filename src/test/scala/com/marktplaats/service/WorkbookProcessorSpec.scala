package com.marktplaats.service

import akka.actor.ActorSystem
import com.marktplaats.config.ConfigProvider
import org.scalatest.Suite
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should

class WorkbookProcessorSpec extends AsyncFlatSpec with Suite with should.Matchers {

  implicit val actorSystem = ActorSystem("WorkbookProcessorSpec")
  val configProvider = new ConfigProvider(actorSystem.settings.config)
  val workbookProcessor = new WorkbookProcessor(configProvider)

  behavior of "WorkbookProcessor"

  it should "process csv file correctly" in {
    val workbooks = workbookProcessor.readWorkbooks(Seq("Workbook2.csv"))

    workbooks map { processedWorkbooks =>
      processedWorkbooks.map(_.map(_.name)) shouldBe Seq(Right("Workbook2.csv"))
    }
  }

  it should "process prn file correctly" in {
    val workbooks = workbookProcessor.readWorkbooks(Seq("Workbook2.prn"))

    workbooks map { processedWorkbooks =>
      processedWorkbooks.map(_.map(_.name)) shouldBe Seq(Right("Workbook2.prn"))
    }
  }

  it should "process both prn and csv file correctly" in {
    val workbooks = workbookProcessor.readWorkbooks(Seq("Workbook2.csv", "Workbook2.prn"))

    workbooks map { processedWorkbooks =>
      processedWorkbooks.map(_.map(_.name)) shouldBe Seq(Right("Workbook2.csv"), Right("Workbook2.prn"))
    }
  }

  it should "does not fail in case of file does not exist" in {
    val workbooks = workbookProcessor.readWorkbooks(Seq("Workbook3.csv"))

    workbooks map { processedWorkbooks =>
      processedWorkbooks shouldBe Seq.empty
    }
  }
}
