import java.sql.DriverManager
import java.time._
import java.util.Date
import java.util.Calendar

import biweekly.ICalendar
import biweekly.component.VEvent
import doobie._
import doobie.implicits._
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import cats.syntax.functor._, cats.syntax.flatMap._
import com.typesafe.config.ConfigFactory
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.EntityEncoder._
import scala.concurrent.ExecutionContext.Implicits.global
import fs2.Stream
import scala.language.higherKinds
import scala.util.Try
import org.http4s.server.blaze._
import scala.concurrent.duration._

object FeedBuilder extends IOApp {

  def query(year: Int): Query0[(Date, Int)] = sql"SELECT (week, studyweek) FROM studyweeks WHERE year >= '$year'".query[(Date, Int)]

  def getStudyWeeks(date: Date): Stream[ConnectionIO, (Date, Int)] = {
    val calendar = Calendar.getInstance()
    calendar.setTime(date)
    val year = calendar.get(Calendar.YEAR)
    query(year).stream
  }

  def createStudyWeekEvent(date: Date, weekNbr: Int, currentDate: Date): VEvent = {
    val event = new VEvent()
    event.setDateStart(date, false)
    event.setSummary(s"Läsvecka $weekNbr")
    event.setCreated(currentDate)
    event.setOrganizer("rootm@dsek.se")
    event.addComment("Är den här veckan fel? Kontakta rootm@dsek.se")
    event
  }

  // Encapsulates the non-referentially transparent ICalendar class into referentially transparent IO transformations
  def makeICal(datesAndWeekNumbers: Stream[IO, (Date, Int)], currentDate: Date): IO[String] = {
    val ical = new ICalendar()
    datesAndWeekNumbers
      .map{ case (date, week) => createStudyWeekEvent(date, week, currentDate) }
      .compile
      .fold(ical)((ical, event) => {ical.addEvent(event); ical})
      .map(ical => ical.write())
  }

  def service(xa: Transactor[IO])(implicit C: cats.effect.Clock[IO]): HttpApp[IO] = HttpApp.liftF[IO] {
    C.realTime(MILLISECONDS).flatMap(currentMillis => {
      val now = new Date(currentMillis)
      val datesAndWeekNumbers: Stream[IO, (Date, Int)] = getStudyWeeks(now).transact(xa)
      Ok(makeICal(datesAndWeekNumbers.take(20), now))
    })
  }

  lazy val config = ConfigFactory.load()
  Class.forName("com.mysql.cj.jdbc.Driver")
  lazy val xa = Transactor.fromConnection[IO] (
    DriverManager.getConnection(
      config.getString("mysql.url"),
      config.getString("mysql.user"),
      config.getString("mysql.password")
    ),
    scala.concurrent.ExecutionContext.global
  )

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(service(xa))
      .serve
      .compile
      .drain
      .flatMap(_ => IO.pure(ExitCode.Success))

}
//my $week_result = $mysql_con->query("SELECT studyweek FROM studyweeks WHERE year = '$year' AND week = '$week'");
