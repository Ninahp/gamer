package com.ninah.app

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

object VoiceApp  extends App {

  val host = "0.0.0.0"
  val port = 9000

  implicit val system: ActorSystem = ActorSystem("voice")  //used to manage, creating and looking up the actors
  implicit val executor : ExecutionContext = system.dispatcher  //incharge of executing Future. it knows where to execute them eg. thread pool
  implicit val materializer: ActorMaterializer = ActorMaterializer() //used to run stream
  val logger =  Logging(system.eventStream, "Africa's Talking Call WebServer")

  val home =
    """
      <h2>Welcome to Africa's Talking Api</h2>
    """.stripMargin

  def sipDialTest(number: String) =
    <Response>
      <Dial phoneNumbers={ number } record="true"/>
    </Response>

  val sayResponse =
    <Response>
      <Say voice="woman" playBeep="false" >Thank you for registering</Say>
    </Response>

   val record =
      <Response>
        <Record finishOnKey="#" maxLength="10" trimSilence="true" playBeep="true" callbackUrl="https://lens.serveo.net/redirect">
          <Say>Hello, welcome to game village. Please tell us your name after the beep followed by the hash sign.</Say>
        </Record>
      </Response>

  val getDigits =
    <Response>
      <GetDigits timeout="30" finishOnKey="#" callbackUrl="https://lens.serveo.net/getDigits">
        <Say>Please enter your phone number followed by the hash sign</Say>
      </GetDigits>
    </Response>

  import akka.http.scaladsl.server.Directives._
  def route =
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

            if(active == 1 ) {
              response.append(record)
            } else {
              logFinalVoiceData(
                destinationNumber  = destinationNumber,
                sessionId          = sessionId,
                isActive           = active,
                callerNumber       = callerNumber,
                callerCarrierName  = callerCarrierName,
                clientRequestedId    = clientRequestId,
                dtmf               = dtmf,
                clientDialedNumber = clientDialedNumber,
                currencyCode       = currencyCode,
                amount             = amount,
                callSessionState   = callSessionState,
                callStartTime      = callStartTime,
                direction          = direction,
                durationInSeconds  = durationInSeconds,
                callerCountryCode  = callerCountryCode
              )
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
          formFields('sessionId, 'isActive.as[Int], 'callerNumber, 'callerCarrierName, 'callTransferHangupCause.?, 'hangupCause.?) {
            (sessionId, active, callerNumber,
             callerCarrierName, callTransferHangupCause, hangupCause) ⇒
            logEventData(sessionId, active, callerNumber, callerCarrierName, callTransferHangupCause, hangupCause)
            val response = new StringBuilder()
              response.append(getDigits)

            complete(OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, response.toString()))            }
        }
      } ~
      path("getDigits") {
        extractRequest{ _: HttpRequest ⇒
          formFields('sessionId, 'isActive.as[Int], 'callerNumber,'dtmfDigits, 'callerCarrierName, 'callTransferHangupCause.?, 'hangupCause.?) {
            (sessionId, active, callerNumber, dtmf,
             callerCarrierName, callTransferHangupCause, hangupCause) ⇒
              logEventData(sessionId, active, callerNumber, dtmf, callerCarrierName, callTransferHangupCause, hangupCause)
              val response = new StringBuilder()
              response.append(sayResponse)

              complete(OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, response.toString()))            }
        }
      } ~
      pathEndOrSingleSlash {
        complete(
          HttpEntity(ContentTypes.`text/html(UTF-8)`, home)
        )
      }

  val binding = Http().bindAndHandle(route,host,port)
  binding.onComplete {
    case Success(serverBinding) => println("Started Server... " + host + ":" + port)
    case Failure(error) => println("error")
  }

  def logVoiceData(
     destinationNumber: String,
     sessionId: String,
     isActive: Int,
     callerNumber: String,
     callerCarrierName: String,
     clientRequestedId: Option[String],
     dtmf: Option[String],
     clientDialedNumber: Option[String]
   ) = logger.info(
    s"\nDESTINATION NUM:$sessionId\n" +
      s"SESSION ID: $isActive\n"+
      s"ACTIVE: $isActive\n"+
      s"CALLER-NUMBER: $callerNumber\n"+
      s"CALLER-CARRIER-NAME: $callerCarrierName\n" +
      s"CLIENT-REQUEST-ID: $clientRequestedId\n"+
      s"CLIENT-DIALED-NUMBER: $clientDialedNumber\n"+
      s"DTMF: $dtmf"
  )

  def logEventData(
     sessionId: String,
     isActive: Int,
     callerNumber: String,
     callerCarrierName: String,
     callTransferHangupCause: Option[String],
     hangupCause: Option[String]
   ) = logger.info(
    s"\nSESSION ID:$sessionId\n" +
      s"ACTIVE: $isActive\n"+
      s"CALLER-NUMBER: $callerNumber\n"+
      s"CALLER-CARRIER-NAME:  $callerCarrierName\n" +
      s"CALL-TRANSFER-HANGUP: $callTransferHangupCause\n"+
      s"HANGUP-CAUSE: $hangupCause"
  )

  def logFinalVoiceData(
     destinationNumber: String,
     sessionId: String,
     isActive: Int,
     callerNumber: String,
     callerCarrierName: String,
     clientRequestedId: Option[String],
     dtmf: Option[String],
     clientDialedNumber: Option[String],
     currencyCode: Option[String],
     amount: Option[String],
     callSessionState: Option[String],
     callStartTime: Option[String],
     direction: Option[String],
     durationInSeconds: Option[String],
     callerCountryCode: Option[String],
   ) = logger.info(
    s"\nDESTINATION NUM:$destinationNumber\n" +
      s"SESSION ID: $sessionId\n"+
      s"ACTIVE: $isActive\n"+
      s"CALLER-NUMBER: $callerNumber\n"+
      s"CALLER-CARRIER-NAME: $callerCarrierName\n" +
      s"CLIENT-REQUEST-ID: $clientRequestedId\n"+
      s"DTMF: $dtmf\n"+
      s"CLIENT-DIALED-NUMBER: $clientDialedNumber\n"+
      s"CURRENCY-CODE: $currencyCode\n"+
      s"AMOUNT: $amount\n"+
      s"CALL-SESSION-STATE: $callSessionState\n"+
      s"CALL-START-TIME: $callStartTime\n"+
      s"DIRECTION: $direction\n"+
      s"DURATION-IN-SECONDS: $durationInSeconds\n"+
      s"CALLER-COUNTRY-CODE: $callerCountryCode\n"

  )
}
