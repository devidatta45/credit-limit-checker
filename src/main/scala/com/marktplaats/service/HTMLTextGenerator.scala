package com.marktplaats.service

import com.marktplaats.config.ConfigProvider
import com.marktplaats.models.{EmptyWorkbookInput, ValidationError, Workbook}

import java.time.format.DateTimeFormatter

class HTMLTextGenerator(configProvider: ConfigProvider) {
  def makeHTMLTemplate(workbooks: Seq[Workbook]): Either[ValidationError, String] = {
    Either.cond(workbooks.nonEmpty, "", EmptyWorkbookInput).map(generateHTML(_, workbooks))
  }

  private def generateHTML(noWorkbookMessage: String, workbooks: Seq[Workbook]): String = {
    import scalatags.Text.all._
    val columns = configProvider.getColumns
    html(
      head(),
      body(
        h1("Workbooks"),
        noWorkbookMessage,
        workbooks.map { workbook =>
          div(
            h3(workbook.name),
            table(
              thead(
                tr(
                  th(columns.name.label),
                  th(columns.address.label),
                  th(columns.postCode.label),
                  th(columns.phone.label),
                  th(columns.creditLimit.label),
                  th(columns.birthday.label)
                )
              ),
              tbody(
                workbook.entries.map { entry =>
                  val birthday = entry.birthday.format(DateTimeFormatter.ISO_DATE)
                  tr(
                    td(entry.name),
                    td(entry.address),
                    td(entry.postcode),
                    td(entry.phone),
                    td(textAlign := "right", String.format("%.2f", entry.creditLimit)),
                    td(birthday)
                  )
                }
              )
            )
          )
        }
      )
    ).toString()
  }
}
