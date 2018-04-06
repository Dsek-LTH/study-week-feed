import java.sql.DriverManager
import java.time._
import java.util.Date
import java.util.Calendar

import biweekly.{Biweekly, ICalendar}
import biweekly.component.VEvent
import biweekly.util.Duration
import doobie._
import doobie.implicits._
import cats._
import cats.data._
import cats.effect.IO
import cats.implicits._
import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try


object FeedBuilder {
  def query(year: Int, week: Int): Query0[Int] = sql"SELECT studyweek FROM studyweeks WHERE year = '$year' AND week = '$week'".query[Int]
  def getStudyWeek(date: Date): ConnectionIO[Option[Int]] = {
    val calendar = Calendar.getInstance()
    calendar.setTime(date)
    val year = calendar.get(Calendar.YEAR)
    val week = calendar.get(Calendar.WEEK_OF_YEAR)
    query(year, week).option
  }

  def createStudyWeekEvent(date: Date, weekNbr: Int): VEvent = {
    val event = new VEvent()
    event.setDateStart(date, false)
    event.setSummary(s"Läsvecka $weekNbr")
    event.setCreated(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant))
    event.setOrganizer("rootm@dsek.se")
    event.addComment("Är den här veckan fel? Kontakta rootm@dsek.se")
    event
  }

  lazy val mondays: Stream[Date] = {
    val today = Calendar.getInstance()
    today.setTime(Date.from(Instant.now()))
    val monday = Calendar.getInstance()
    monday.setTime(today.getTime)
    val daysAfterMonday = (today.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7 // SUNDAY - MONDAY == -1, -1 % 7 == -1, want 6
    monday.add(Calendar.DAY_OF_WEEK, -daysAfterMonday)
    Stream.continually({monday.add(Calendar.DAY_OF_WEEK, 7); monday.getTime})
  }

  def makeICal(datesAndWeekNumbers: Seq[(Date, Int)]): ICalendar = {
    val ical = new ICalendar()
    datesAndWeekNumbers.seq
      .map((createStudyWeekEvent(_,_)).tupled)
      .foreach(ical.addEvent)
    ical
  }



  def main(args: Array[String]): Unit = {

    val config = ConfigFactory.load()
    Class.forName("com.mysql.cj.jdbc.Driver")
    val xa = Transactor.fromConnection[IO] {
      DriverManager.getConnection(
        config.getString("mysql.url"),
        config.getString("mysql.user"),
        config.getString("mysql.password")
      )
    }

    lazy val weekNumbers = mondays.map(getStudyWeek)
      .map(_.transact(xa))
      .map(io =>
        Try(io.unsafeRunSync)
          .toOption.flatten
      )

    lazy val datesAndWeekNumbers = mondays.zip(weekNumbers)
      .take(20)
      .collect{case (date, Some(weekNbr)) => date -> weekNbr}

    val ical = makeICal(datesAndWeekNumbers)
    ical.write(System.out)
  }




}
//my $week_result = $mysql_con->query("SELECT studyweek FROM studyweeks WHERE year = '$year' AND week = '$week'");