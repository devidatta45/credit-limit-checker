package com.marktplaats

import akka.actor.ActorSystem
import akka.http.scaladsl.Http

import scala.concurrent.duration._
import scala.util.Failure

object CreditLimitApp extends AppContext with App {

  override implicit val system = ActorSystem("Credit-Limit-App")

  Http()
    .newServerAt(config.getString("http.interface"), config.getInt("http.port"))
    .bindFlow(route)
    .map { binding =>
      println(
        "HTTP service listening on: " +
          s"http://${binding.localAddress.getHostName}:${binding.localAddress.getPort}/"
      )

      sys.addShutdownHook {
        binding
          .terminate(hardDeadline = 30.seconds)
          .flatMap(_ => system.terminate())
          .onComplete { _ =>
            println("Termination completed")
          }
        println("Received termination signal")
      }
    }.onComplete {
    case Failure(ex) =>
      println("server binding error:", ex)
      system.terminate()
      sys.exit(1)
    case _ =>
  }
}
