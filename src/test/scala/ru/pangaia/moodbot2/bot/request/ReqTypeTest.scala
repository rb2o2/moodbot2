package ru.pangaia.moodbot2.bot.request

import org.scalatest.flatspec.AnyFlatSpec
import ru.pangaia.moodbot2.bot.{Conf, Config}

class ReqTypeTest extends AnyFlatSpec :
  implicit val config: Conf = Config("test")

  "График все" should "be PlotFromPeriod" in {
    ReqType("График все") match
      case PlotFromPeriod(a, b) => succeed
      case x => fail("wrong! " + x)
  }
  "График 01.01.2001" should "be PlotFromDate" in {
    ReqType("График 01.01.2001") match
      case PlotFrom(a, b) => succeed
      case x => fail("wrong! " + x)
  }
  "График 12.01.2010 14.01.2010" should "be PlotFromTo" in {
    ReqType("График 12.01.2010 14.01.2010") match
      case PlotFromTo(a, b, c) => succeed
      case x => fail("wrong! " + x)
  }
  "История месяц" should "be HistoryFromPeriod" in {
    ReqType("История месяц") match
      case HistoryFromPeriod(a, b) => succeed
      case x => fail("wrong! " + x)
  }
  "История 21.12.2010" should "be HistoryFromDate" in {
    val v = ReqType("История 21.12.2010")
    v match
      case HistoryFrom(a,b) => succeed
      case x =>
        fail("wrong! " + x)
  }
  "История 11.01.2013 13.02.2013" should "be HistoryFromTo" in {
    ReqType("История 11.01.2013 13.02.2013") match
      case HistoryFromTo(a, b, c) => succeed
      case x => fail("wrong! " + x)
  }
  "% message to record" should "be ReattachCommandMessage" in {
    ReqType("% message to record3") match
      case ReattachCommandMessage(a, b) => succeed
      case x =>
        fail("wrong! " + x)
  }
  "% 21.12.1994 12-00" should "be ReattachDateTime" in {
    ReqType("% 21.12.1994 12-00") match
      case ReattachDateTime(a, b) => succeed
      case x =>
        fail("wrong! " + x)
  }
  "%" should "be ReattachCommand" in {
    ReqType("%") match
      case ReattachCommand(a) => succeed
      case x =>
        fail("wrong! + x")
  }
  "smiley" should " be RecMark" in {
    assert(ReqType("☺️").isInstanceOf[RecMark])
    assert(ReqType("\ud83d\ude01").isInstanceOf[RecMark])
    assert(ReqType("\ud83d\ude10").isInstanceOf[RecMark])
    assert(ReqType("\ud83d\ude15").isInstanceOf[RecMark])
    assert(ReqType("\ud83d\ude29").isInstanceOf[RecMark])
    assert(ReqType(":)")  .isInstanceOf[RecMark])
    assert(ReqType(":))") .isInstanceOf[RecMark])
    assert(ReqType(":(")  .isInstanceOf[RecMark])
    assert(ReqType(":((") .isInstanceOf[RecMark])
    assert(ReqType(":|")  .isInstanceOf[RecMark])
    assert(ReqType(":-)") .isInstanceOf[RecMark])
    assert(ReqType(":-))").isInstanceOf[RecMark])
    assert(ReqType(":-((").isInstanceOf[RecMark])
    assert(ReqType(":-(") .isInstanceOf[RecMark])
    assert(ReqType(":-|") .isInstanceOf[RecMark])
  }
  "12.01.2002" should "be DateReq" in {
    ReqType("21.01.2002") match
      case DateReq(date) => succeed
      case x => fail("wrong! " + x)
  }
  "21.12.2001 12-14" should "be DateTimeReq" in {
    ReqType("21.12.2001 12-14") match
      case DateTimeReq(dateTime) => succeed
      case x => fail("wrong! " + x)
  }
  "12.02.2010 16.03.2010" should "be DateFromToReq" in {
    ReqType("12.02.2010 16.03.2010") match
      case DateFromToReq(from, to) => succeed
      case x => fail("wrong! " + x)
  }
  "Day" should "be Period" in {
    ReqType("Day") match
      case Period(period) => succeed
      case x => fail("wrong! " + x)
  }
