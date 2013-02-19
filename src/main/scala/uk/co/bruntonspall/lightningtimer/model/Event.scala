package uk.co.bruntonspall.lightningtimer.model

import uk.co.bruntonspall.lightningtimer.util.Annotations._
import com.googlecode.objectify.annotation.Entity

/**
 * Entities for lightning timer
 * We have a single event, these are identified by an ID, and are accessible on urls like
 * http://timer.brunton-spall.co.uk/e/myevent
 * Each event has a number of open sessions, a new session is created when a new user who hasn't
 * visited that event before visits the page.
 * Each event has an owner, a person who is allowed to sign-in and use the admin tools for that page
 *
 */
@Entity
case class Event(
    @Id var id: String,
    @Index var owner: String) {

  // Only for Objectify creation
  private def this() { this(null, null) }

}

@Entity
case class Session(
    @Id var id: String,
    @Parent var event: Event) {

  // Only for Objectify creation
  private def this() { this(null, null) }

}

