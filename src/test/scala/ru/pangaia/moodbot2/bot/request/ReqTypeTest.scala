package ru.pangaia.moodbot2.bot.request

import org.scalatest.flatspec.AnyFlatSpec
import ru.pangaia.moodbot2.bot.{Conf, Config}

class ReqTypeTest extends AnyFlatSpec {
  implicit val config: Conf = Config("test")

  "График все" should "be PlotFromPeriod" in {
    ReqType("График все") match
      case PlotFromPeriod(a, b) => succeed
      case _ => fail("wrong!")
  }
  "График 01.01.2001" should "be PlotFromDate" in {
    ReqType("График 01.01.2001") match
      case PlotFrom(a, b) => succeed
      case _ => fail("wrong!")
  }
  "График 12.01.2010 14.01.2010" should "be PlotFromTo" in {
    ReqType("График 12.01.2010 14.01.2010") match
      case PlotFromTo(a, b, c) => succeed
      case _ => fail("wrong!")
  }
  "История месяц" should "be HistoryFromPeriod" in {
    ReqType("История месяц") match
      case HistoryFromPeriod(a, b) => succeed
      case _ => fail("wrong!")
  }
  "История 21.12.2010" should "be HistoryFromDate" in {
    val v = ReqType("История 21.12.2010")
    v match
      case HistoryFrom(a,b) => succeed
      case _ =>
        println(v)
        fail("wrong!")
  }
  "История 11.01.2013 13.02.2013" should "be HistoryFromTo" in {
    ReqType("История 11.01.2013 13.02.2013") match
      case HistoryFromTo(a, b, c) => succeed
      case _ => fail("wrong!")
  }
  "% message to record" should "be ReattachCommandMessage" in {
    ReqType("% message to record3") match
      case ReattachCommandMessage(a, b) => succeed
      case x =>
        println(x)
        fail("wrong!")
  }
  "% 21.12.1994 12-00" should "be ReattachDateTime" in {
    ReqType("% 21.12.1994 12-00") match
      case ReattachDateTime(a, b) => succeed
      case x =>
        println(x)
        fail("wrong!")
  }
  "%" should "be ReattachCommand" in {
    ReqType("%") match
      case ReattachCommand(a) => succeed
      case x =>
        println(x)
        fail("wrong!")
  }

}
