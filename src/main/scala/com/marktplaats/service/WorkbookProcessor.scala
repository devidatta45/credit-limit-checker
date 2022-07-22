package com.marktplaats.service

import akka.stream.Materializer
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.{FileIO, Framing, Sink}
import akka.util.ByteString
import cats.implicits._
import com.marktplaats.config.ConfigProvider
import com.marktplaats.models._
import com.marktplaats.service.dsdsd.StateWithTimestamp
import com.marktplaats.utils.FileValidator._
import org.apache.commons.io.FilenameUtils

import java.nio.file.{Files, Paths}
import java.time.format.DateTimeFormatter
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class WorkbookProcessor(configProvider: ConfigProvider) {
  val availableWorkbooks = List(configProvider.csvWorkbook, configProvider.prnWorkbook)

  val csvDateFormatter = DateTimeFormatter.ofPattern(configProvider.csvDateFormatter)
  val prnDateFormatter = DateTimeFormatter.ofPattern(configProvider.prnDateFormatter)

  val columns = configProvider.getColumns

  def readWorkbooks(workbooks: Seq[String])(implicit ec: ExecutionContext, mat: Materializer): Future[Seq[Either[ValidationError, Workbook]]] = {
    workbooks.filter { workbookName =>
      availableWorkbooks.contains(workbookName) && Files.exists(Paths.get(workbookName))
    }.traverse { workbookName =>
      val futureEntries = FilenameUtils.getExtension(workbookName) match {
        case "csv" => readCSV(workbookName)
        case "prn" => readPRN(workbookName)
        case _ => Future.successful(Seq.empty)
      }

      futureEntries.map { entries =>
        entries.sequence.map(Workbook(workbookName, _))
      }
    }
  }

  private def readCSV(fileName: String)(implicit mat: Materializer): Future[Seq[Either[ValidationError, WorkbookEntry]]] = {
    FileIO.fromPath(Paths.get(fileName))
      .via(CsvParsing.lineScanner())
      .via(CsvToMap.toMapAsStrings())
      .map { csvLineAsMap =>
        for {
          birthDayValue <- validateEmptyField(csvLineAsMap, columns.birthday.label)
          birthday <- parseDate(birthDayValue, csvDateFormatter)
          name <- validateEmptyField(csvLineAsMap, columns.name.label)
          address <- validateEmptyField(csvLineAsMap, columns.address.label)
          postcode <- validateEmptyField(csvLineAsMap, columns.postCode.label)
          phone <- validateEmptyField(csvLineAsMap, columns.phone.label)
          creditLimit <- validateEmptyField(csvLineAsMap, columns.creditLimit.label)
        } yield WorkbookEntry(
          name = name,
          address = address,
          postcode = postcode,
          phone = phone,
          creditLimit = creditLimit.toDouble,
          birthday = birthday
        )
      }
      .runWith(Sink.seq)
  }

  private def readPRN(fileName: String)(implicit ec: ExecutionContext, mat: Materializer): Future[Seq[Either[ValidationError, WorkbookEntry]]] = {
    FileIO.fromPath(Paths.get(fileName))
      .via(Framing.delimiter(ByteString("\n"), 1024, allowTruncation = true))
      .map(_.utf8String)
      .runWith(Sink.seq)
      .map { lines =>
        val workBookEntries = for {
          header <- Either.fromOption(lines.headOption, EmptyCsvFile(fileName))
          columnLengths <- Either.catchNonFatal(getPRNColumnLengths(header)).leftMap(PRNParsingError)
          entries = lines.drop(1).map(validateAndGenerateWorkbookEntries(_, columnLengths))
        } yield entries
        workBookEntries match {
          case Right(entries) => entries
          case Left(error) => Seq(error.asLeft)
        }
      }
  }

  private def validateAndGenerateWorkbookEntries(line: String, columnLengths: Seq[Int]): Either[ValidationError, WorkbookEntry] = {
    @tailrec
    def getColumnValues(columnLengths: Seq[Int], remainingLine: String, columnValues: Seq[String] = Seq.empty): Seq[String] = {
      if (columnLengths.isEmpty) columnValues
      else {
        val colLength = columnLengths.head
        val colValue = remainingLine.take(colLength)
        getColumnValues(columnLengths.tail, remainingLine.drop(colLength), columnValues :+ colValue.trim)
      }
    }

    val columnValues: Seq[String] = getColumnValues(columnLengths, line)

    for {
      birthDayValue <- validateEmptyColumn(columnValues, columns.birthday.index)
      birthday <- parseDate(birthDayValue, prnDateFormatter)
      name <- validateEmptyColumn(columnValues, columns.name.index)
      address <- validateEmptyColumn(columnValues, columns.address.index)
      postcode <- validateEmptyColumn(columnValues, columns.postCode.index)
      phone <- validateEmptyColumn(columnValues, columns.phone.index)
      creditLimit <- validateEmptyColumn(columnValues, columns.creditLimit.index)
    } yield WorkbookEntry(
      name = name,
      address = address,
      postcode = postcode,
      phone = phone,
      creditLimit = creditLimit.toDouble,
      birthday = birthday
    )
  }

  @tailrec
  private def getPRNColumnLengths(headerLine: String, colLengths: Seq[Int] = Seq.empty): Seq[Int] = {
    if (headerLine.isEmpty) colLengths
    else {
      val line1 = headerLine.takeWhile(!_.isWhitespace)

      val line2 = if (line1 == "Credit") {
        " Limit" + headerLine.drop(columns.creditLimit.label.length).takeWhile(_.isWhitespace)
      } else {
        headerLine.drop(line1.length).takeWhile(_.isWhitespace)
      }

      val columnLen = line1.length + line2.length
      val remainder = headerLine.drop(columnLen)

      getPRNColumnLengths(remainder, colLengths :+ columnLen)
    }
  }
}

object dsdsd extends App {
  case class State(id: String, own_state: String, derived_state: String, check_states: Map[String, String], depends_on: Vector[String])

  case class StateWithTimestamp(state: State, latestTimestamp: Option[Int] = None)

  case class Event(timestamp: String, component: String, check_state: String, state: String)

  val app = State("app", "no-data", "no-data", Map("CPU load" -> "no-data", "RAM usage" -> "no-data"), Vector("db"))
  val db = State("db", "no-data", "no-data", Map("CPU load" -> "no-data", "RAM usage" -> "no-data"), Vector("app"))
  val data = Vector(app, db)

  def saveGraph(states: Vector[State],
                addAllToStorageFunc: Vector[(String, StateWithTimestamp)] => Unit): Map[String, StateWithTimestamp] = {
    val stateList = states.map(state => state.id -> StateWithTimestamp(state))
    addAllToStorageFunc(stateList)
    Storage.storedMap
  }

  val event1 = Event("1", "db", "CPU load", "warning")
  val event2 = Event("2", "app", "CPU load", "clear")

  val events = Vector(event1, event2)

  @tailrec
  def applyEvents(events: Vector[Event],
                  stateMap: Map[String, StateWithTimestamp],
                  addToStorageFunc: (String, StateWithTimestamp) => Unit,
                  addAllToStorageFunc: Vector[(String, StateWithTimestamp)] => Unit): Either[ValidationError, Map[String, StateWithTimestamp]] = {
    if (events.isEmpty) {
      stateMap.asRight
    } else {
      val eitherEvent = applyEvent(events.head, stateMap, addToStorageFunc, addAllToStorageFunc)
      eitherEvent match {
        case Right(value) => applyEvents(events.tail, value, addToStorageFunc, addAllToStorageFunc)
        case Left(value) => value.asLeft
      }
    }
  }

  def applyEvent(event: Event,
                 currentStateMap: Map[String, StateWithTimestamp],
                 addToStorageFunc: (String, StateWithTimestamp) => Unit,
                 addAllToStorageFunc: Vector[(String, StateWithTimestamp)] => Unit): Either[ValidationError, Map[String, StateWithTimestamp]] = {
    for {
      retrievedStateWithTimestamp <- Either.fromOption(currentStateMap.get(event.component), InvalidComponent("the component is not valid"))
      finalState = retrievedStateWithTimestamp.latestTimestamp match {
        case Some(timestamp) if timestamp > event.timestamp.toInt => None
        case _ =>
          val changedMap = retrievedStateWithTimestamp.state.check_states + (event.check_state -> event.state)
          val changedState = resolveOwnState(retrievedStateWithTimestamp, event)
          val derivedState = resolveDerivedState(retrievedStateWithTimestamp, changedState)
          val finalState = retrievedStateWithTimestamp.state.copy(check_states = changedMap, own_state = changedState, derived_state = derivedState)
          addToStorageFunc(finalState.id, StateWithTimestamp(finalState, Some(event.timestamp.toInt)))
          Some(finalState)
      }
      _ <- finalState match {
        case Some(state) => resolveDependents(retrievedStateWithTimestamp.state.depends_on, currentStateMap, state, event, addAllToStorageFunc)
        case None => ().asRight
      }
    } yield Storage.storedMap
  }

  sealed trait ValidationError {
    def message: String
  }

  case class InvalidComponent(override val message: String) extends ValidationError

  def resolveOwnState(stateWithTimestamp: StateWithTimestamp, incomingEvent: Event): String = {
    if (stateWithTimestamp.state.own_state == "no-data") {
      incomingEvent.state
    } else if (stateWithTimestamp.state.own_state == "clear") {
      if (incomingEvent.state == "no-data") stateWithTimestamp.state.own_state else incomingEvent.state
    } else if (stateWithTimestamp.state.own_state == "warning") {
      if (incomingEvent.state == "alert") incomingEvent.state else stateWithTimestamp.state.own_state
    } else {
      stateWithTimestamp.state.own_state
    }
  }

  def resolveDerivedState(stateWithTimestamp: StateWithTimestamp, currentOwnState: String): String = {
    if (currentOwnState == "no-data") {
      stateWithTimestamp.state.derived_state
    } else if (currentOwnState == "clear") {
      if (stateWithTimestamp.state.derived_state == "no-data") currentOwnState else stateWithTimestamp.state.derived_state
    } else if (currentOwnState == "warning") {
      if (stateWithTimestamp.state.derived_state == "alert") stateWithTimestamp.state.derived_state else currentOwnState
    } else {
      currentOwnState
    }
  }

  def resolveDependentDerivedState(stateWithTimestamp: StateWithTimestamp, currentState: State): String = {
    if (stateWithTimestamp.state.own_state == "alert") {
      stateWithTimestamp.state.own_state
    } else if (stateWithTimestamp.state.own_state == "warning") {
      if (currentState.derived_state == "alert") currentState.derived_state else stateWithTimestamp.state.own_state
    } else if (stateWithTimestamp.state.own_state == "clear") {
      if (currentState.derived_state == "no-data") stateWithTimestamp.state.own_state else currentState.derived_state
    } else {
      currentState.derived_state
    }
  }

  def resolveDependents(dependents: Vector[String],
                        currentStateMap: Map[String, StateWithTimestamp],
                        changedState: State,
                        incomingEvent: Event,
                        addAllToStorageFunc: Vector[(String, StateWithTimestamp)] => Unit): Either[ValidationError, Unit] = {
    dependents.traverse { d =>
      for {
        dependState <- Either.fromOption(currentStateMap.get(d), InvalidComponent("the dependent component is not valid"))
        result = dependState.latestTimestamp match {
          case Some(timestamp) if timestamp > incomingEvent.timestamp.toInt => None
          case _ =>
            val derivedState = resolveDependentDerivedState(dependState, changedState)
            Some((d, StateWithTimestamp(dependState.state.copy(derived_state = derivedState), Some(incomingEvent.timestamp.toInt))))
        }
      } yield result
    }.map(_.flatten).map(addAllToStorageFunc)
  }

  def saveToStorage(id: String, stateWithTimestamp: StateWithTimestamp): Unit = {
    Storage.storedMap = Storage.storedMap + ((id, stateWithTimestamp))
  }

  def saveAllToStorage(values: Vector[(String, StateWithTimestamp)]): Unit = {
    Storage.storedMap = Storage.storedMap ++ values
  }

  val initialStates = saveGraph(data, saveAllToStorage)
  println(initialStates)
  val Right(changedStates) = applyEvents(events, initialStates, saveToStorage, saveAllToStorage)
  println(changedStates)
  val event3 = Event("1", "db", "CPU load", "alert")

  val Right(newStates) = applyEvents(Vector(event3), changedStates, saveToStorage, saveAllToStorage)
  println(newStates)

  val event4 = Event("3", "db", "CPU load", "alert")

  val Right(anotherStates) = applyEvents(Vector(event4), newStates, saveToStorage, saveAllToStorage)
  println(anotherStates)

  val event5 = Event("3", "pela", "CPU load", "alert")

  val Left(failed) = applyEvents(Vector(event5), anotherStates, saveToStorage, saveAllToStorage)
  println(failed.message)


}

object Storage {
  var storedMap = Map.empty[String, StateWithTimestamp]
}