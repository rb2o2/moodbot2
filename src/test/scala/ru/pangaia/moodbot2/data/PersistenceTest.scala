package ru.pangaia.moodbot2.data

import org.scalatest.flatspec.AnyFlatSpec
import ru.pangaia.moodbot2.bot.{Conf, Config}
import scalikejdbc.ConnectionPool

import java.nio.charset.StandardCharsets
import java.sql.{DriverManager, Timestamp}
import java.time.Instant

class PersistenceTest extends AnyFlatSpec {
  Class.forName("org.postgresql.Driver")
  val config: Conf = Config("test")
  ConnectionPool.singleton(config("db.url"), config("user"), config("pw"))
  val persistence = new Persistence(config)
  
  "getHistory" should "contain message saved previously" in {
    val pre = Timestamp.from(Instant.now())
    val mText = "Привет, у меня замечательно всё, все тесты зеленые!"
    persistence.saveMessage(0L, "testUser",
      mText)
    val history = persistence.getHistory(
      0L,
      username = "testUser",
      from = pre,
      to = Timestamp.from(Instant.now()))
    assert(history.nonEmpty)
    println(history.head.messageText)
  }
}
