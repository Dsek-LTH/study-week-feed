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

  def query(year: Int, week: Int): Query0[(Int, String, Int)] = sql"""
    SELECT week, studyweek, year
    FROM studyweeks
    WHERE year > $year
      OR year = $year AND week >= $week
    ORDER BY year, week;""".query[(Int, String, Int)]

  def getStudyWeeks(date: Date): Stream[ConnectionIO, (Date, String)] = {
    val calendar = Calendar.getInstance()
    calendar.setTime(date)
    val currentYear = calendar.get(Calendar.YEAR)
    val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
    query(currentYear, currentWeek).stream
      .map {
        case (weekNbr, studyWeek, year) => (getMondayOfWeek(year, weekNbr), studyWeek)
      }
  }

  def getMondayOfWeek(year: Int, weekNbr: Int): Date = {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.WEEK_OF_YEAR, weekNbr)
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    cal.getTime()
  }

  // Unfortunately returns mutable object, breaking referential transparency :(
  def createStudyWeekEvent(date: Date, weekDescriptor: String, currentDate: Date): VEvent = {
    val event = new VEvent()
    event.setDateStart(date, false)
    event.setSummary(s"$weekDescriptor")
    event.setCreated(currentDate)
    event.setOrganizer("rootm@dsek.se")
    event.addComment("Är den här veckan fel? Kontakta rootm@dsek.se")
    event
  }

  // Encapsulates the non-referentially transparent ICalendar class into referentially transparent IO transformations
  def makeICal(datesAndWeekNumbers: Stream[IO, (Date, String)], currentDate: Date): IO[String] = {
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
      val datesAndWeekNumbers: Stream[IO, (Date, String)] = getStudyWeeks(now).transact(xa)
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
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(service(xa))
      .serve
      .compile
      .drain
      .flatMap(_ => IO.pure(ExitCode.Success))

}
//my $week_result = $mysql_con->query("SELECT studyweek FROM studyweeks WHERE year = '$year' AND week = '$week'");
