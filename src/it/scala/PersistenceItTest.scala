import com.dimafeng.testcontainers.{ContainerDef, PostgreSQLContainer}
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{Args, Status}
import ru.pangaia.moodbot2.bot.{Conf, Config}
import ru.pangaia.moodbot2.data.{Mark, Message, Persistence}

import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class PersistenceItTest extends AnyFlatSpec with TestContainerForAll {
  val config: Conf = Config("ittest")
  val persistence: Persistence = new Persistence(config)

  override val containerDef: ContainerDef = PostgreSQLContainer.Def(databaseName = "moodbot_tc",
    dockerImageName = "postgres:15")

  override def startContainers(): containerDef.Container = super.startContainers()

  override def run(testName: Option[String], args: Args): Status = super.run(testName, args)

  override protected def runTest(testName: String, args: Args): Status = super.runTest(testName, args)

  val u: String = "anotherUser"
  val reattachedText: String = "reattached message"

  "getHistory" should "contain message saved previously" in {
    withContainers {
      _ =>
        val pre = Timestamp.from(Instant.now())
        val mText = "Привет, у меня замечательно всё, все тесты зеленые!"
        assert(persistence.getUserId("testUser").isEmpty, "user 'testUser' found before saving")
        persistence.saveMessage(0L, "testUser",
          mText)
        assert(persistence.getUserId("testUser").nonEmpty, "user 'testUser' not found")
        val history = persistence.getHistory(
          0L,
          username = "testUser",
          from = pre,
          to = Timestamp.from(Instant.now()))
        assert(history.size === 1, "getHistory returns not a single message")
    }
  }

  "getHistory" should "return messages ordered by both reported_time and creation_timestamp" in {
    withContainers {
      _ =>
        val m1 = "message 1"
        val m2 = "message 2"
        val m3 = "message 3"
        for (t <- Seq(m1,m2,m3)) {
          persistence.saveMessage(0L, u, t)
        }
        persistence.saveMessageToDateTime(0L, u, reattachedText,
          Timestamp.from(Instant.now().minus(2, ChronoUnit.HOURS)))
        val messages: List[Message] = persistence.getHistory(0L, u,
          Timestamp.from(Instant.now().minus(3, ChronoUnit.HOURS)),
          Timestamp.from(Instant.now()))
        import scala.math.Ordering.Implicits.infixOrderingOps
        assert(messages.size === 4, "there are not exactly 4 message")
        assert(messages.head.messageText === reattachedText, "first message is not reattached one")
        assert(messages(1).creationTimestamp < messages(2).creationTimestamp &&
          messages(2).creationTimestamp < messages(3).creationTimestamp,
          "tail messages are not ordered by insertion time")
    }
  }

  "getHistory" should "not return anything when toDate is earlier than fromDate" in {
    withContainers {
      _ =>
        val allMessages = persistence.getHistory(0L, u, Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS)),
          Timestamp.from(Instant.now()))
        assert(allMessages.nonEmpty, "there are no messages")
        val noMessages = persistence.getHistory(0L, u, Timestamp.from(Instant.now()),
          Timestamp.from(Instant.now().minus(6, ChronoUnit.DAYS)))
        assert(noMessages.isEmpty, "some messages from reversed time interval returned")
    }
  }

  "getHistory" should "return empty list for nonexistent user" in {
    withContainers {
      _ =>
        val existentUserHistory = persistence.getHistory(0L, u,
          Timestamp.from(Instant.now().minus(1000L, ChronoUnit.HOURS)),
          Timestamp.from(Instant.now()))
        assert(existentUserHistory.nonEmpty, "existent user's message history is empty")
        val nonexistentUserName = UUID.randomUUID().toString()
        val historyOfNonexistentUser = persistence.getHistory(0L, nonexistentUserName,
          Timestamp.from(Instant.now().minus(1000L, ChronoUnit.DAYS)),
          Timestamp.from(Instant.now()))
        assert(historyOfNonexistentUser.isEmpty, "nonexistent user has messages")
    }
  }

  "saveMarks" should "save Marks for both existing and new user, creating new user when absent" in {
    withContainers {
      _ =>
        val t1 = Timestamp.from(Instant.now())
        val mark1 = 2
        val mark2 = 0
        val newUserName = UUID.randomUUID().toString()
        persistence.saveMark(0L, u , mark1)
        val existentUserMarks = persistence.getMarks(0L, u, t1, Timestamp.from(Instant.now()))
        assert(existentUserMarks.nonEmpty, "saved mark not returned")
        assert(existentUserMarks.head.markValue === mark1, "wrong mark value")

        persistence.saveMark(0L, newUserName, mark2)
        val newUserMarks = persistence.getMarks(0L, newUserName, t1, Timestamp.from(Instant.now()))
        val newId = persistence.getUserId(newUserName)
        assert(newId.nonEmpty, "user not found")
        assert(newUserMarks.nonEmpty, "saved mark not returned")
        assert(newUserMarks.head.markValue === mark2, "wrong mark value")
    }
  }

  "getMarks" should "return empty list for nonexistent user" in {
    withContainers {
      _ =>
        val nonexistentUser = "someBodyOnceToldMe"
        val idOpt = persistence.getUserId(nonexistentUser)
        assert(idOpt.isEmpty)
        val emptyMarksList = persistence.getMarks(0L, nonexistentUser,
          Timestamp.from(Instant.now().minus(10000, ChronoUnit.HOURS)),
          Timestamp.from(Instant.now()))
        assert(emptyMarksList.isEmpty, "someMarks were returned for nonexistent user")
    }
  }

  "getMarks" should "return empty list for inverted time interval" in {
    withContainers {
      _ =>
        val t0 = Timestamp.from(Instant.now())
        val t1 = Timestamp.from(Instant.now().minus(10000L, ChronoUnit.HOURS))
        val regularList: List[Mark] = persistence.getMarks(0L, u, t1, t0)
        val invertedList: List[Mark] = persistence.getMarks(0L, u, t0, t1)
        assert(regularList.nonEmpty, "no marks saved")
        assert(invertedList.isEmpty, "marks in inverted interval")
    }
  }
}
