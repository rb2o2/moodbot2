package ru.pangaia.moodbot2

import ru.pangaia.moodbot2.data.Message

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import scala.util.Try

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

  def dateToTimestamp(d: String, tzOffsetHours: Int): Timestamp =
    val toText = d.split("\\.")
    val toDayOfMonth = toText(0).toInt
    val toMonth = toText(1).toInt
    val toYear = toText(2).toInt
    val date = LocalDate.of(toYear, toMonth, toDayOfMonth)
    val instant = LocalDateTime.of(date, LocalTime.MIDNIGHT)
      .toInstant(ZoneOffset.ofHours(tzOffsetHours))
    Timestamp.from(instant)

}
