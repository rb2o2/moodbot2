package ru.pangaia.moodbot2

import ru.pangaia.moodbot2.data.Message

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import scala.util.{Failure, Success, Try}

object TimeUtils {
  def formatTime(m: Message): String =
    val time = if m.reportedTime.nonEmpty then m.reportedTime.get else m.creationTimestamp
    SimpleDateFormat("yyyy-MM-dd HH:mm").format(time)

  def parseDateTime(dt: String, tzOffsetHours: Int): Try[Timestamp] =
    Try(Timestamp.from(
      LocalDateTime.from(DateTimeFormatter.ofPattern("dd.MM.yyyy HH-mm").parse(dt))
        .toInstant(ZoneOffset.ofHours(tzOffsetHours))))

  def parseFrom(text: String, tzOffsetHours: Int): Try[Timestamp] =
    val fromText = text.split(" +")(0)
    Try(dateToTimestamp(fromText, tzOffsetHours))

  def parseTo(text: String, tzOffsetHours: Int): Try[Timestamp] =
    val toText = Try(text.split(" +")(1))
    toText.map(dateToTimestamp(_, tzOffsetHours))

  private def dateToTimestamp(d: String, tzOffsetHours: Int): Timestamp =
    val toText = d.split("\\.")
    val toDayOfMonth = toText(0).toInt
    val toMonth = toText(1).toInt
    val toYear = toText(2).toInt
    val date = LocalDate.of(toYear, toMonth, toDayOfMonth)
    val instant = LocalDateTime.of(date, LocalTime.MIDNIGHT)
      .toInstant(ZoneOffset.ofHours(tzOffsetHours))
    Timestamp.from(instant)

  def parsePeriod(p: String): Try[Timestamp] =
    p.toLowerCase() match
      case "все"|"всё"|"all" => Success(new Timestamp(0))
      case "year"|"год" => Success(Timestamp.from(Instant.now().minus(365L, ChronoUnit.DAYS)))
      case "месяц"|"month" => Success(Timestamp.from(Instant.now().minus(30L, ChronoUnit.DAYS)))
      case "неделя"|"week" => Success(Timestamp.from(Instant.now().minus(7L, ChronoUnit.DAYS)))
      case "день"|"day" => Success(Timestamp.from(Instant.now().minus(24L, ChronoUnit.HOURS)))
      case _ => Failure(new IllegalArgumentException(s"illegal argument for period : $p"))
}
