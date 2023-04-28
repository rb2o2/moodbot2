package ru.pangaia.moodbot2.bot

import com.pengrad.telegrambot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.{InlineKeyboardButton, InlineKeyboardMarkup}
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.{MessagesResponse, SendResponse}
import com.pengrad.telegrambot.{TelegramBot, UpdatesListener}
import com.typesafe.scalalogging.Logger
import ru.pangaia.moodbot2.persistence.Persistence
import ru.pangaia.moodbot2.plot.Plotter

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.util
import java.util.TimeZone
import java.util.concurrent.{CompletableFuture, SynchronousQueue}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

@main
def main(args: Array[String]): Unit = {
  MoodBot.init()
}

object MoodBot {
  val bot: TelegramBot = new TelegramBot(Config.getToken)
  val state: collection.mutable.Map[(Long,String), State] = new collection.mutable.HashMap[(Long,String), State]()
  def init(): Unit = {
    bot.setUpdatesListener(new MoodUpdatesListener)
    Console.in.readLine()
    bot.shutdown()
  }
}

class MoodUpdatesListener extends UpdatesListener {
  val db: Persistence = new Persistence(Config)
  val bot: TelegramBot = MoodBot.bot
  val state: collection.mutable.Map[(Long,String), State] = MoodBot.state
  val plotter: Plotter = Plotter
  private val logger = Logger(getClass.getName)
  private val failedUpdates: util.concurrent.SynchronousQueue[Int] = new SynchronousQueue[Int]()
  override def process(updates: util.List[Update]): Int = {
    updates.asScala.map(processSingle)
    UpdatesListener.CONFIRMED_UPDATES_ALL
  }

  def record(chatId: Long, userId: String, text: String): Unit = {
    db.saveMessage(chatId, userId, text)
    val response = new SendMessage(chatId, "введите оценку настроения")
    val keyboard = new InlineKeyboardMarkup(Array(
      new InlineKeyboardButton("\ud83d\ude01"),
      new InlineKeyboardButton("☺️"),
      new InlineKeyboardButton("\ud83d\ude10"),
      new InlineKeyboardButton("\ud83d\ude15"),
      new InlineKeyboardButton("\ud83d\ude29")
    ))
    response.replyMarkup(keyboard)
    state((chatId, userId)) = State.SentMessage
    bot.execute(response)
  }
  def showPlotPre(chatId: Long, userId: String): Unit = {
    val req = new SendMessage(chatId, "введите даты (начала и конца) в формате 'дд.мм.гггг дд.мм.гггг'")
    bot.execute(req)
    state((chatId, userId)) = State.SentPlotRequest
  }

  def showPlot(chatId: Long, userId: String, from: Timestamp, to: Timestamp): Unit = {
    val marks = db.getMarks(chatId, userId, from, to)
    plotter.plot(marks, from, to)
  }

  def showHistoryPre(chatId: Long, userId: String): Unit = {
    val req = new SendMessage(chatId, "введите даты (начала и конца) в формате 'дд.мм.гггг дд.мм.гггг'")
    bot.execute(req)
    state((chatId, userId)) = State.SentHistoryRequest
  }

  def showHistory(chatId: Long, userId: String, dateFrom: Timestamp, dateTo: Timestamp): Unit = {
    val messages = db.getHistory(chatId, userId, dateFrom, dateTo)
    messages.foreach(m =>
      val time = m.reportedTime != null ? m.reportedTime : m.creationTimestamp
      val timeFormatted = DateTimeFormatter.BASIC_ISO_DATE.toFormat().format(time)
      val msg = new SendMessage(chatId, s"$timeFormatted \n ${m.messageText}")
      bot.execute(msg)
    )
    state((chatId, userId)) = State.Base
  }

  def reattach(chatId: Long, userId: String, text: String): Unit = {
    ???
  }

  def menu(chatId: Long, userId: String): Unit = {
    val menuMsg = new SendMessage(chatId, Config.menuText)
    bot.execute(menuMsg)
    state((chatId, userId)) = State.Base
  }

  def recordMark(chatId: Long, userId: String, mark: String): Unit =
    if state((chatId, userId)) == State.SentMessage then {
    val markNum = mark match
      case "\ud83d\ude01" => 2
      case "☺️" => 1
      case "\ud83d\ude10" => 0
      case "\ud83d\ude15" => -1
      case "\ud83d\ude01" => -2
    db.saveMark(chatId, userId, markNum)
    state((chatId, userId)) = State.Base
  }

  def guessCommand(text: String): (Long, String) => Unit = {
    val firstWord = text.split(" +" )(0).trim().toLowerCase().replace("/","")
    firstWord match {
      case mark:("\ud83d\ude01"|"☺️"|"\ud83d\ude10"|"\ud83d\ude15"|"\ud83d\ude29") =>
        (chatId: Long, userId: String) => recordMark(chatId, userId, mark)
      case "start"|"старт" => (chatId: Long, userId: String) => menu(chatId,userId)
      case "график"|"plot" => (chatId: Long, userId: String) => showPlotPre(chatId, userId)
      case "история"|"history" => (chatId: Long, userId: String) => showHistoryPre(chatId, userId)
      case "%" => (chatId: Long, userId: String) => reattach(chatId, userId, text)
      case _ => (chatId: Long, userId: String) => state((chatId, userId)) match
        case State.SentPlotRequest => showPlot(chatId, userId, parseFrom(text), parseTo(text))
        case State.Base => record(chatId, userId, text)
        case State.SentHistoryRequest => showHistory(chatId, userId, parseFrom(text), parseTo(text))
    }
  }

  def parseFrom(text: String): Timestamp = {
    val fromText = text.split(" +")(0).split("\\.")
    val fromDayOfMonth = fromText(0).toInt
    val fromMonth = fromText(1).toInt
    val fromYear = fromText(2).toInt
    val date = LocalDate.of(fromYear, fromMonth, fromDayOfMonth)
    val instant = LocalDateTime.of(date, LocalTime.MIDNIGHT)
      .toInstant(ZoneOffset.ofHours(3))
    Timestamp.from(instant)
  }

  def parseTo(text: String): Timestamp = {
    val toText = text.split(" +")(1).split("\\.")
    val toDayOfMonth = toText(0).toInt
    val toMonth = toText(1).toInt
    val toYear = toText(2).toInt
    val date = LocalDate.of(toYear, toMonth, toDayOfMonth)
    val instant = LocalDateTime.of(date, LocalTime.MIDNIGHT)
      .toInstant(ZoneOffset.ofHours(3))
    Timestamp.from(instant)
  }

  def processSingle(update: Update): Int = { // this is atomic
    try {
      val com = guessCommand(update.message().text())
      val userId = update.message().chat().username()
      val chatId = update.message().chat().id()
      com(chatId, userId)
    } catch {
      case x: Exception =>
        logger.error(s"Exception occured processing update ${update.updateId()}: ${x.getMessage}", x)
        failedUpdates.add(update.updateId())
        UpdatesListener.CONFIRMED_UPDATES_NONE
    }
    UpdatesListener.CONFIRMED_UPDATES_ALL
  }
}
