package com.marktplaats.utils

import com.marktplaats.models.{EmptyColumnError, EmptyFieldError, InvalidDateFormat}
import org.scalatest.Suite
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FileValidatorSpec extends AnyFlatSpec with Suite with should.Matchers {

  behavior of "FileValidator"

  it should "give proper error in case the column is empty" in {
    val columnValues = Seq("Name", "Address")
    val validatedEmptyColumn = FileValidator.validateEmptyColumn(columnValues, 3)

    validatedEmptyColumn shouldBe Left(EmptyColumnError(3, "Failed because of index: 3"))
  }

  it should "give column name correctly" in {
    val columnValues = Seq("Name", "Address")
    val validatedEmptyColumn = FileValidator.validateEmptyColumn(columnValues, 1)

    validatedEmptyColumn shouldBe Right("Address")
  }

  it should "give proper error in case the field is not present" in {
    val csvValues = Map("Name" -> "Alex", "Address" -> "Buckingham")
    val validatedEmptyColumn = FileValidator.validateEmptyField(csvValues, "CreditLimit")

    validatedEmptyColumn shouldBe Left(EmptyFieldError("CreditLimit"))
  }

  it should "give field trimmed value correctly" in {
    val csvValues = Map("Name" -> "Alex", "Address" -> " Buckingham ")
    val validatedEmptyColumn = FileValidator.validateEmptyField(csvValues, "Address")

    validatedEmptyColumn shouldBe Right("Buckingham")
  }

  it should "give proper error in case the date is not proper" in {
    val date = "19920512"
    val validatedEmptyColumn = FileValidator.parseDate(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    validatedEmptyColumn shouldBe Left(InvalidDateFormat("Text '19920512' could not be parsed at index 2"))
  }

  it should "give correct date in case the date format is proper" in {
    val date = "19920512"
    val validatedEmptyColumn = FileValidator.parseDate(date, DateTimeFormatter.ofPattern("yyyyMMdd"))

    validatedEmptyColumn shouldBe Right(LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd")))
  }
}
