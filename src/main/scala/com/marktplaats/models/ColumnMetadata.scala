package com.marktplaats.models

final case class ColumnMetadata(label: String, index: Int)

final case class Columns(name: ColumnMetadata,
                         address: ColumnMetadata,
                         postCode: ColumnMetadata,
                         phone: ColumnMetadata,
                         creditLimit: ColumnMetadata,
                         birthday: ColumnMetadata)
