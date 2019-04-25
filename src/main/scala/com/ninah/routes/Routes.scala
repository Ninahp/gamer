package com.ninah.routes

import akka.http.scaladsl.server.Directives._
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives.{complete, extractRequest, formFields, path, pathEndOrSingleSlash}
import akka.stream.ActorMaterializer
import com.ninah.app.VoiceApp.{home, logEventData, logFinalVoiceData, logVoiceData, sayResponse}

import com.ninah.xml.Xml._

import scala.concurrent.ExecutionContext

trait RoutesT {

  protected implicit val system: ActorSystem
  protected implicit val executor: ExecutionContext
  protected implicit val materializer: ActorMaterializer

  def route  =
    path("voice") {
      extractRequest { _: HttpRequest ⇒
        formFields('destinationNumber, 'sessionId, 'isActive.as[Int], 'callerNumber,
          'callerCarrierName, 'clientRequestId.?, 'dtmfDigits.?, 'clientDialedNumber.?, 'currencyCode.?, 'amount.?,
          'callSessionState.?, 'callStartTime.?, 'direction.?, 'durationInSeconds.?, 'callerCountryCode.?) {
          (destinationNumber, sessionId, active, callerNumber, callerCarrierName,
           clientRequestId, dtmf, clientDialedNumber, currencyCode, amount, callSessionState, callStartTime,
           direction, durationInSeconds, callerCountryCode) ⇒

            logVoiceData(destinationNumber, sessionId, active, callerNumber, callerCarrierName, clientRequestId, dtmf, clientDialedNumber)

            val response = new StringBuilder()

            // based on certain logic
            if callerNumber == "jdjdjhd" {
              response.append(sipDialTest(callerNumber))
            }
            else {
              response.append(sayResponse)
            }

            complete(OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, response.toString()))
        }
      }
    } ~
      path("events") {
        extractRequest{ _: HttpRequest ⇒
          formFields('sessionId, 'isActive.as[Int], 'callerNumber, 'callerCarrierName, 'callTransferHangupCause.?, 'hangupCause.?) { (sessionId, active, callerNumber,
                                                                                                                                      callerCarrierName, callTransferHangupCause, hangupCause) ⇒
            logEventData(sessionId, active, callerNumber, callerCarrierName, callTransferHangupCause, hangupCause)
            complete(OK)
          }
        }
      } ~
      path("redirect") {
        extractRequest{ _: HttpRequest ⇒
          formFields('sessionId, 'isActive.as[Int], 'callerNumber, 'callerCarrierName, 'callTransferHangupCause.?, 'hangupCause.?) { (sessionId, active, callerNumber,
                                                                                                                                      callerCarrierName, callTransferHangupCause, hangupCause) ⇒
            logEventData(sessionId, active, callerNumber, callerCarrierName, callTransferHangupCause, hangupCause)
            val response = new StringBuilder()

            complete(OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, response.toString()))            }
        }
      } ~
      pathEndOrSingleSlash {
        complete(
          HttpEntity(ContentTypes.`text/html(UTF-8)`, home)
        )
      }

}
