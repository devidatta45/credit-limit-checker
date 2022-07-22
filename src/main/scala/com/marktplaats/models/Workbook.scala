package com.marktplaats.models

import java.time.LocalDate

final case class Workbook(name: String, entries: Seq[WorkbookEntry])

final case class WorkbookEntry(
                                name: String,
                                address: String,
                                postcode: String,
                                phone: String,
                                creditLimit: Double,
                                birthday: LocalDate
                              )
