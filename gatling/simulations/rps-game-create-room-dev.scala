
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import io.gatling.core.session.Expression
import scala.util.Random

class RpsGameCreateRoomSimulation extends Simulation {

  before {
    println("***** Rps game - Create room simultation *****")
  }

  after {
    println("***** Create room simultatio has ended! ******")
  }

  val random = new Random
  val gameModes = Seq("VS_MACHINE", "VS_FRIEND", "VS_RANDOM_PLAYER")

  def getRandomElement(list: Seq[String], random: Random): String = list(random.nextInt(list.length))

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .inferHtmlResources()
    .acceptHeader("*/*")
    .acceptLanguageHeader("en-US,en;q=0.9")

  val headers = Map("authorization" -> "Basic cnBzcGxheWVyOnBhc3N3b3Jk", "Content-Type" -> "application/json")

  val scn = scenario("RpsGameCreateRoomSimulation")
    .feed(Iterator.continually(Map(
      "RANDOM_PLAYER_NAME" -> java.util.UUID.randomUUID.toString
    )))
    .feed(Iterator.continually(Map(
      "RANDOM_GAME_MODE" -> getRandomElement(gameModes, random)
    )))
    .exec(http("CreateRoom")
      .post(session => "/api/1.0/rooms")
      .queryParam("firstPlayerName", "${RANDOM_PLAYER_NAME}")
      .queryParam("gameMode", "${RANDOM_GAME_MODE}")
      .asJson
      .headers(headers)
      .check(status.is(201))
      .check(responseTimeInMillis.saveAs("responseTimeInMillis"))
    )
    .exec(session => {
      session
    })

  setUp(scn.inject(
    constantConcurrentUsers(10) during (2 minutes)
  ).protocols(httpProtocol))
}