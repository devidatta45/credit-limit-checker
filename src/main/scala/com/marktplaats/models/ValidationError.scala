package com.marktplaats.models


sealed trait ValidationError {
  def message: String
}

final case class InvalidDateFormat(message: String) extends ValidationError

final case class EmptyColumnError(index: Int, rawError: String) extends ValidationError {
  override def message: String = s"Index number $index is empty, raw error: $rawError"
}

final case class EmptyFieldError(field: String) extends ValidationError {
  override def message: String = s"Field $field is empty"
}

final case class EmptyCsvFile(fileName: String) extends ValidationError {
  override def message: String = s"File: $fileName is empty"
}

final case class PRNParsingError(error: Throwable) extends ValidationError {
  override def message: String = s"Failed while parsing prn file with error: ${error.getMessage}"
}

case object EmptyWorkbookInput extends ValidationError {
  override def message: String = "No workbook was specified. Access workbooks by adding " +
    "query parameters to the URL. For example '?workbook=Workbook2.csv'"
}