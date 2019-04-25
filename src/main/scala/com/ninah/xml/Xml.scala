package com.ninah.xml

object Xml {

  def sipDialTest(number: String) =
    <Response>
      <Dial phoneNumbers={ number } record="true"/>
    </Response>

  val sayResponse =
    <Response>
      <Say voice="woman" playBeep="false" >Hello, this is just a test call, good bye</Say>
    </Response>

}
