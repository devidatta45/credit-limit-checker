package com.marktplaats

import akka.actor.ActorSystem
import akka.util.Timeout
import com.marktplaats.config.ConfigProvider
import com.marktplaats.routes.CreditLimitTracker
import com.marktplaats.service.{HTMLTextGenerator, WorkbookProcessor}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

trait AppContext {

  implicit def system: ActorSystem

  implicit def executionContext: ExecutionContext = system.dispatcher

  implicit def timeout: Timeout = Duration.fromNanos(100000)

  lazy val config = system.settings.config
  lazy val configProvider = new ConfigProvider(config)
  lazy val workbookProcessor = new WorkbookProcessor(configProvider)
  lazy val htmlGenerator = new HTMLTextGenerator(configProvider)
  lazy val route = new CreditLimitTracker(workbookProcessor, htmlGenerator).route
}
