package esw.ocs.impl

import java.time.Duration

import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import esw.ocs.impl.messages.HealthCheckMsg
import esw.ocs.impl.messages.HealthCheckMsg._

class HealthCheckActorProxy(actorRef: ActorRef[HealthCheckMsg], val heartbeatInterval: Duration)(
    implicit timeout: Timeout,
    scheduler: Scheduler
) {
  def sendHeartbeat(): Unit        = actorRef ! SendHeartbeat
  def startHealthCheck(): Unit = actorRef ! StartHealthCheck
}
