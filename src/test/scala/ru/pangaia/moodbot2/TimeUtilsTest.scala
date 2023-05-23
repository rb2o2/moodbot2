package ru.pangaia.moodbot2

import org.scalatest.flatspec.AnyFlatSpec
import ru.pangaia.moodbot2.bot.{Conf, Config}
import ru.pangaia.moodbot2.data.Message

import java.sql.Timestamp
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.util.{Failure, Success, Try}

class TimeUtilsTest extends AnyFlatSpec {
  val config: Conf = Config("test")
  val zoneHrs: Int = config("zone.offset.default").toInt

  "formatTime" should "properly format message time && return reattach ts when it is present" in {

    val reattachTime = Timestamp.from(
      LocalDateTime.of(LocalDate.of(2022, 1, 1),
        LocalTime.of(20, 2, 12, 157))
        .toInstant(ZoneOffset.ofHours(zoneHrs)))
    val simpleTime = Timestamp.from(Instant.now().minus(2L, ChronoUnit.DAYS))

    val messageOne = Message(UUID.randomUUID(), UUID.randomUUID(), "test", simpleTime, Some(reattachTime))
    val formatted =TimeUtils.formatTime(messageOne)
    assert(formatted === "2022-01-01 20:02", s"$formatted <> '2022-01-01 20:02'")
  }

  "parseDateTime" should "parse proper ts when argument is formatted properly and vice versa" in {
    val proper = "11.12.1998 12-01"
    val improper = "12312h 912 mk"

    assert(TimeUtils.parseDateTime(proper, zoneHrs) match {
      case Success(_) => true
      case _ => false
    }, s"$proper unsuccessfully parsed")
    assert(TimeUtils.parseDateTime(improper, zoneHrs) match {
      case Success(_) => false
      case _ => true
    }, s"$improper succcessfully parsed")
  }

  "parseFrom & parseTo" should "parse proper timestamp when argument is formatted properly and vice versa" in {
    val proper = "12.11.2020 14.11.2020"
    val improper = Seq(
      "41.90/2020202 1001.11.2000",
      "47.12.00 12.19.1999",
      "000.000.00 61.11.1999"
    )
    assert(TimeUtils.parseFrom(proper, zoneHrs) match {case Success(_) => true case _ => false},
      s"$proper not parsed")
    assert(TimeUtils.parseTo(proper, zoneHrs) match {case Success(_) => true case _ => false},
      s"$proper not parsed")
    for (i <- improper) {
      assert(TimeUtils.parseFrom(i, zoneHrs) match {case Failure(_) => true case _ => false},
      s"$i parsed successfully")
      assert(TimeUtils.parseTo(i, zoneHrs) match {case Failure(_) => true case _ => false},
        s"$i parsed successfully")
    }
  }
}
