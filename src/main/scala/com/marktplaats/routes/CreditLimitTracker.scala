package com.marktplaats.routes

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import cats.implicits._
import com.marktplaats.models.{ValidationError, Workbook}
import com.marktplaats.service.{HTMLTextGenerator, WorkbookProcessor}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class CreditLimitTracker(workbookProcessor: WorkbookProcessor, htmlTextGenerator: HTMLTextGenerator) {
  def route(implicit ec: ExecutionContext, mat: Materializer) = pathPrefix("credit-limit") {
    pathEnd {
      get {
        parameters(Symbol("workbook").*) { (workbooks: Iterable[String]) =>
          onComplete(workbookProcessor.readWorkbooks(workbooks.toSeq.reverse)) {
            case Success(workbooks: Seq[Either[ValidationError, Workbook]]) =>
              workbooks.sequence match {
                case Right(workbooks) =>
                  htmlTextGenerator.makeHTMLTemplate(workbooks) match {
                    case Right(htmlValue) => complete(
                      HttpEntity(
                        ContentTypes.`text/html(UTF-8)`,
                        htmlValue
                      )
                    )
                    case Left(validationError) => complete((StatusCodes.BadRequest, validationError.message))
                  }
                case Left(validationError) => complete((StatusCodes.BadRequest, validationError.message))
              }

            case Failure(ex) =>
              complete((StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}"))
          }
        }
      }
    }
  }

}
