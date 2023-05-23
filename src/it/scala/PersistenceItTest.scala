import com.dimafeng.testcontainers.{ContainerDef, PostgreSQLContainer}
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{Args, Status}
import ru.pangaia.moodbot2.bot.{Conf, Config}
import ru.pangaia.moodbot2.data.Persistence

import java.sql.Timestamp
import java.time.Instant

class PersistenceItTest extends AnyFlatSpec with TestContainerForAll {
  val config: Conf = Config("ittest")
  val persistence: Persistence = new Persistence(config)

  override val containerDef: ContainerDef = PostgreSQLContainer.Def(databaseName = "moodbot_tc",
    dockerImageName = "postgres:15")

  override def startContainers(): containerDef.Container = super.startContainers()

  override def run(testName: Option[String], args: Args): Status = super.run(testName, args)

  override protected def runTest(testName: String, args: Args): Status = super.runTest(testName, args)

  "getHistory" should "contain message saved previously" in {
    withContainers {
      pgContainer =>
        val pre = Timestamp.from(Instant.now())
        val mText = "Привет, у меня замечательно всё, все тесты зеленые!"
        persistence.saveMessage(0L, "testUser",
          mText)
        val history = persistence.getHistory(
          0L,
          username = "testUser",
          from = pre,
          to = Timestamp.from(Instant.now()))
        assert(history.nonEmpty, "getHistory returns empty list")
        println(history.head.messageText)
    }
  }
}
