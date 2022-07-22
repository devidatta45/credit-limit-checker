package com.marktplaats.utils

import cats.implicits._
import com.marktplaats.models.{EmptyColumnError, EmptyFieldError, InvalidDateFormat, ValidationError}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object FileValidator {
  def validateEmptyColumn(columnValues: Seq[String], index: Int): Either[ValidationError, String] = {
    Either.catchNonFatal {
      columnValues(index)
    }.leftMap { error =>
      EmptyColumnError(index, s"Failed because of index: ${error.getMessage}")
    }
  }

  def validateEmptyField(csvValues: Map[String, String], field: String): Either[ValidationError, String] = {
    Either.fromOption(csvValues.get(field).map(_.trim), EmptyFieldError(field))
  }

  def parseDate(dateValue: String, dateTimeFormatter: DateTimeFormatter): Either[ValidationError, LocalDate] = {
    Either.catchNonFatal {
      LocalDate.parse(dateValue, dateTimeFormatter)
    }.leftMap { error =>
      InvalidDateFormat(error.getMessage)
    }
  }
}
