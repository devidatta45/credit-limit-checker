package com.marktplaats.service

import akka.actor.ActorSystem
import com.marktplaats.config.ConfigProvider
import com.marktplaats.models.{EmptyWorkbookInput, Workbook}
import org.scalatest.{EitherValues, Suite}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class HTMLTextGeneratorSpec extends AnyFlatSpec with Suite with should.Matchers with EitherValues{

  implicit val actorSystem = ActorSystem("HTMLTextGeneratorSpec")
  val configProvider = new ConfigProvider(actorSystem.settings.config)
  val hTMLTextGenerator = new HTMLTextGenerator(configProvider)

  behavior of "HTMLTextGenerator"

  it should "fail in case of no workbooks" in {
    val validatedHtml = hTMLTextGenerator.makeHTMLTemplate(Seq.empty)
    validatedHtml shouldBe Left(EmptyWorkbookInput)
  }

  it should "not fail in case of workbooks" in {
    val validatedHtml = hTMLTextGenerator.makeHTMLTemplate(Seq(Workbook("Workbook2.csv", Seq.empty)))
    validatedHtml.isRight shouldBe true
  }
}
