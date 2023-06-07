package ru.pangaia.moodbot2.bot.request

import ru.pangaia.moodbot2.bot.*

sealed trait ReqType:
  def text(): String

case class RegularMessage(msg: String) extends ReqType:
  override def text(): String = msg

case class RecMark(msg: String) extends ReqType:
  override def text(): String = msg

case class Cancel(msg: String) extends ReqType:
  override def text(): String = msg

case class Menu(msg: String) extends ReqType:
  override def text(): String = msg

case class Plot(command: String) extends ReqType:
  override def text(): String = command

case class PlotFrom(command: String, dateFrom: String) extends ReqType:
  override def text(): String = s"$command $dateFrom"

case class PlotFromPeriod(command: String, datePeriod: String) extends ReqType:
  override def text(): String = s"$command $datePeriod"

case class PlotFromTo(command: String, dateFrom: String, dateTo: String) extends ReqType:
  override def text(): String = s"$command $dateFrom $dateTo"

case class History(command: String) extends ReqType:
  override def text(): String = command

case class HistoryFrom(command: String, dateFrom: String) extends ReqType:
  override def text(): String = s"$command $dateFrom"

case class HistoryFromPeriod(command: String, datePeriod: String) extends ReqType:
  override def text(): String = s"$command $datePeriod"

case class HistoryFromTo(command: String, from: String, to: String) extends ReqType:
  override def text(): String = s"$command $from $to"

case class ReattachCommand(command: String) extends ReqType:
  override def text(): String = command

case class ReattachDateTime(command: String, dateTime: String) extends ReqType:
  override def text(): String = s"$command $dateTime"

case class ReattachCommandMessage(command: String, msg: String) extends ReqType:
  override def text(): String = s"$command $msg"

case class DateReq(date: String) extends ReqType:
  override def text(): String = date

case class Period(period: String) extends ReqType:
  override def text(): String = period

case class DateTimeReq(dateTime: String) extends ReqType:
  override def text(): String = dateTime

case class DateFromToReq(from: String, to: String) extends ReqType:
  override def text(): String = s"$from $to"


object ReqType:
  val markRegex: String =
    "\ud83d\ude01|☺️|\ud83d\ude10|\ud83d\ude15|\ud83d\ude29|" +
      ":\\)\\)|:\\(\\(|:\\(|:\\)|:-\\(|:-\\)|:-\\(\\(|:-\\)\\)|:-\\||:\\|"
  val cancelRegex: String = "cancel|отмена"
  val menuRegex: String = "start|menu|help|старт|меню|помощь"
  val plotCom: String = "plot|график"
  val historyCom: String = "history|история"
  val periodRegex: String = "год|year|неделя|week|месяц|month|день|day|все|всё|all"
  val regexDate: String = "([0-2][0-9]|31|30)\\.(0[1-9]|1[0-2])\\.[0-9]{4}"
  val regexTime: String = "[0-5][0-9]-[0-2][0-9]"
  def apply(text: String)(implicit config: Conf): ReqType =
    val reattach = config("message.timestamp.reattach.symbol")
    val tokens = text.stripPrefix("/").trim().split(" +").toList
    tokens.head.toLowerCase() match
      case d if d == reattach && tokens.size == 1 =>
        ReattachCommand(tokens.head)
      case d if d == reattach && tokens.size == 3 && tokens(1).matches(regexDate) && tokens(2).matches(regexTime) =>
        ReattachDateTime(reattach, s"${tokens(1)} ${tokens(2)}")
      case d if d == reattach && tokens.size > 1 && !tokens(1).matches(regexDate)
        => ReattachCommandMessage(reattach, text.stripPrefix(reattach).trim)
      case d if tokens.size == 1 && d.matches(regexDate) => DateReq(d)
      case d if tokens.size == 2 && d.matches(regexDate) && tokens(1).matches(regexDate) => DateFromToReq(d, tokens(1))
      case d if tokens.size == 2 && d.matches(regexDate) && tokens(1).matches(regexTime) => DateTimeReq(s"$d ${tokens(1)}")
      case d if d.toLowerCase.matches(plotCom) && tokens.size == 1 => Plot(d)
      case d if d.toLowerCase.matches(plotCom) && tokens.size == 2 && tokens(1).matches(periodRegex) =>
        PlotFromPeriod(d, tokens(1))
      case d if d.toLowerCase.matches(plotCom) && tokens.size == 2 && tokens(1).matches(regexDate) =>
        PlotFrom(d, tokens(1))
      case d if d.toLowerCase.matches(plotCom) && tokens.size == 3 && tokens(1).matches(regexDate) &&
        tokens(2).matches(regexDate) => PlotFromTo(d, tokens(1), tokens(2))
      case d if d.toLowerCase.matches(historyCom) && tokens.size == 1 => History(d)
      case d if d.toLowerCase.matches(historyCom) && tokens.size == 2 && tokens(1).matches(periodRegex) =>
        HistoryFromPeriod(d, tokens(1))
      case d if d.toLowerCase.matches(historyCom) && tokens.size == 2 && tokens(1).matches(regexDate) =>
        HistoryFrom(d, tokens(1))
      case d if d.toLowerCase.matches(historyCom) && tokens.size == 3 && tokens(1).matches(regexDate) &&
        tokens(2).matches(regexDate) => HistoryFromTo(d, tokens(1), tokens(2))
      case d if d.toLowerCase.matches(periodRegex) && tokens.size == 1 => Period(d)
      case d if tokens.size == 1 && d.toLowerCase().matches(menuRegex) => Menu(d)
      case d if d.toLowerCase().matches(cancelRegex) && tokens.size == 1 => Cancel(d)
      case d if d.matches(markRegex) && tokens.size == 1 => RecMark(d)
      case _ => RegularMessage(text)
