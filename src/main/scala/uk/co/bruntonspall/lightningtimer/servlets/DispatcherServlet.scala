package uk.co.bruntonspall.lightningtimer.servlets

import org.scalatra.ScalatraServlet
import uk.co.bruntonspall.lightningtimer.scalatra.TwirlSupport
import uk.co.bruntonspall.lightningtimer.model.{ Event, Session }
import uk.co.bruntonspall.lightningtimer.util.Ofy
import com.google.appengine.api.channel.{ ChannelMessage, ChannelServiceFactory }
import com.googlecode.objectify.ObjectifyService
import java.util.UUID
import com.google.appengine.api.memcache.MemcacheServiceFactory
import scala.collection.JavaConversions._

case class Msg(state: String, count: Int, warning: Int) {
  def asJson =
    """
      |{
      |"state":"%s",
      |"count":%d,
      |"warning":%d
      |}
    """.format(state, count, warning).stripMargin
}

class DispatcherServlet extends ScalatraServlet with TwirlSupport {
  ObjectifyService.register(classOf[Event])
  ObjectifyService.register(classOf[Session])

  get("/") {
    html.index.render
  }

  get("/e/:event") {
    val ofy = ObjectifyService.factory.begin()
    val eventId = params("event")
    val event = Option(ofy.load.`type`(classOf[Event]).id(eventId).get()).getOrElse {
      val e = Event(eventId, "")
      ofy.save.entity(e)
      e
    }
    val eventSession = session.getOrElse("event_session", {
      val eventSession = Session(UUID.randomUUID.toString, event)
      ofy.save.entity(eventSession)
      eventSession
    }).asInstanceOf[Session]

    val channelService = ChannelServiceFactory.getChannelService
    val memcacheService = MemcacheServiceFactory.getMemcacheService
    val channelToken = Option(memcacheService.get(eventSession.id).asInstanceOf[String]).getOrElse {
      val token = channelService.createChannel(eventSession.id)
      memcacheService.put(eventSession.id, token)
      token
    }

    html.welcome.render(event, eventSession, channelToken)
  }

  get("/e/:event/admin") {
    val ofy = ObjectifyService.factory.begin()
    val eventId = params("event")
    var count = params.getOrElse("count", "30").toInt
    var warning = params.getOrElse("warning", "5").toInt
    val event = Option(ofy.load.`type`(classOf[Event]).id(eventId).get()).getOrElse {
      val e = Event(eventId, "")
      ofy.save.entity(e)
      e
    }
    html.admin.render(event, count, warning)
  }

  def sendMsgToEvent(eventId: String, msg: Msg) {
    val ofy = ObjectifyService.factory.begin()
    val event = Option(ofy.load.`type`(classOf[Event]).id(eventId).get()).get
    val channelService = ChannelServiceFactory.getChannelService
    ofy.load.`type`(classOf[Session]).ancestor(event).list().toList.foreach { s: Session =>
      channelService.sendMessage(new ChannelMessage(s.id, msg.asJson))
    }
  }

  post("/e/:event/start") {
    val eventId = params("event")
    val count = params.getOrElse("count", "30").toInt
    val warning = params.getOrElse("warning", "5").toInt
    sendMsgToEvent(eventId, Msg("start", count, warning))
    redirect("/e/%s/admin?count=%d&warning=%d".format(eventId, count, warning))
  }

  post("/e/:event/reset") {
    val eventId = params("event")
    val count = params.getOrElse("count", "30").toInt
    val warning = params.getOrElse("warning", "5").toInt
    sendMsgToEvent(eventId, Msg("reset", count, warning))
    redirect("/e/%s/admin?count=%d&warning=%d".format(eventId, count, warning))
  }
}
