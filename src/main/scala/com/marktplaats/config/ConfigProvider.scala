package com.marktplaats.config

import com.marktplaats.models.{Columns, ColumnMetadata}
import com.typesafe.config.Config

class ConfigProvider(config: Config) {

  val csvWorkbook = config.getString("available-workbook.csv")
  val prnWorkbook = config.getString("available-workbook.prn")
  val csvDateFormatter = config.getString("date-format.csv")
  val prnDateFormatter = config.getString("date-format.prn")

  private def getMetaData(column: String): ColumnMetadata = {
    ColumnMetadata(
      config.getString(s"$column.label"),
      config.getInt(s"$column.index")
    )
  }

  def getColumns: Columns = {
    Columns(
      getMetaData("columns.name"),
      getMetaData("columns.address"),
      getMetaData("columns.postCode"),
      getMetaData("columns.phone"),
      getMetaData("columns.creditLimit"),
      getMetaData("columns.birthday")
    )
  }
}
