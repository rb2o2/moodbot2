package ru.pangaia.moodbot2.bot

import com.pengrad.telegrambot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.{InlineKeyboardButton, InlineKeyboardMarkup}
import com.pengrad.telegrambot.request.{AbstractSendRequest, BaseRequest, SendMessage, SendPhoto}
import com.pengrad.telegrambot.response.{BaseResponse, MessagesResponse, SendResponse}
import com.pengrad.telegrambot.{Callback, TelegramBot, UpdatesListener}
import com.typesafe.scalalogging.Logger
import ru.pangaia.moodbot2.bot.Config
import ru.pangaia.moodbot2.data.{Mark, Message, Persistence}
import ru.pangaia.moodbot2.plot.Plotter

import java.io.{BufferedReader, InputStreamReader, Reader}
import java.sql.Timestamp
import java.time.*
import java.time.format.DateTimeFormatter
import java.util
import java.util.TimeZone
import java.util.concurrent.{CompletableFuture, SynchronousQueue}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}

type Conf = String => String

@main
def main(args: String*): Unit =
  val profile = Try(args(0)).getOrElse("dev")
  val config = Config(profile)
  val bot = new MoodBot(config)
  val handler = new MoodUpdatesListener(config, bot)
  bot.setUpdatesListener(handler)
  bot.init()
  bot.awaitTermination()

class MoodBot(config: Conf) extends TelegramBot(config("token"))
  with ChatState
  with Plotting(config)
  with Persisting(config):
  def init(): Unit =
    System.out.println("Press enter to shutdown...")

  def awaitTermination(): Unit =
    Using.Manager {
      using =>
        val isr = using(new InputStreamReader(System.in))
        val br = using(new BufferedReader(isr))
        br.readLine()
    }
    System.out.println("Stopping...")
    shutdown()

class MoodUpdatesListener(config: Conf, bot: MoodBot) extends UpdatesListener
  with ChatState
  with Plotting(config)
  with Persisting(config)
  with Messenger(config, bot):
  private val logger = Logger(getClass.getName)
  private val failedUpdates: util.concurrent.SynchronousQueue[Update] = new SynchronousQueue()

  override def process(updates: util.List[Update]): Int =
    updates.asScala.map(processSingle)
    UpdatesListener.CONFIRMED_UPDATES_ALL

  def record(chatId: Long, userId: String, text: String): Unit =
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
    execute(response)
    state((chatId, userId)) = ConversationState.SentMessage

  def showPlotPre(chatId: Long, userId: String): Unit =
    val req = new SendMessage(chatId, "введите даты (начала и конца) в формате 'дд.мм.гггг дд.мм.гггг'")
    execute(req)
    state((chatId, userId)) = ConversationState.SentPlotRequest

  def showPlot(chatId: Long, userId: String, from: Timestamp, to: Timestamp): Unit =
    val marks = db.getMarks(chatId, userId, from, to)
    val imageFilePath = plotPNGToFile(marks)
    val req = new SendPhoto(chatId, new java.io.File(imageFilePath))
    execute(req)

  def showHistoryPre(chatId: Long, userId: String): Unit =
    val req = new SendMessage(chatId, "введите даты (начала и конца) в формате 'дд.мм.гггг дд.мм.гггг'")
    execute(req)
    state((chatId, userId)) = ConversationState.SentHistoryRequest

  def showHistory(chatId: Long, userId: String, dateFrom: Timestamp, dateTo: Timestamp): Unit =
    val messages = db.getHistory(chatId, userId, dateFrom, dateTo)
    messages.foreach(m =>
      val timeFormatted = formatTime(m)
      val msg = new SendMessage(chatId, s"$timeFormatted \n${m.messageText}")
      execute(msg)
    )
    state((chatId, userId)) = ConversationState.Base

  private def formatTime(m: Message): String =
    val time = if m.reportedTime != null then m.reportedTime else m.creationTimestamp
    DateTimeFormatter.BASIC_ISO_DATE.toFormat().format(time)

  def recordOnSpecificTimePre(chatId: Long, userId: String, text: String): Unit =
    ???

  def recordOnSpecificTime(l: Long, str: String, str1: String): Unit =
    ???


  def menu(chatId: Long, userId: String, introText: String): Unit =
    val menuMsg = new SendMessage(chatId, introText)
    execute(menuMsg)
    state((chatId, userId)) = ConversationState.Base

  def recordMark(chatId: Long, userId: String, mark: String): Unit =
    if state((chatId, userId)) == ConversationState.SentMessage then
      val markNum = mark match
        case "\ud83d\ude01" => 2
        case "☺️" => 1
        case "\ud83d\ude10" => 0
        case "\ud83d\ude15" => -1
        case "\ud83d\ude01" => -2
      db.saveMark(chatId, userId, markNum)
      state((chatId, userId)) = ConversationState.Base


  def guessCommand(text: String): (Long, String) => Unit =
    val firstWord = text.split(" +")(0).trim().toLowerCase().replace("/", "")
    firstWord match
      case mark: ("\ud83d\ude01" | "☺️" | "\ud83d\ude10" | "\ud83d\ude15" | "\ud83d\ude29") =>
        (chatId: Long, userId: String)
        => recordMark(chatId, userId, mark)
      case "start" | "старт" => (chatId: Long, userId: String)
        => menu(chatId, userId, config("menu.text"))
      case "cancel" | "отмена" => (chatId: Long, userId: String)
        => menu(chatId, userId, config("cancel.text"))
      case "график" | "plot" => (chatId: Long, userId: String)
        => showPlotPre(chatId, userId)
      case "история" | "history" => (chatId: Long, userId: String)
        => showHistoryPre(chatId, userId)
      case "%" => (chatId: Long, userId: String)
        => recordOnSpecificTimePre(chatId, userId, text)
      case _ => (chatId: Long, userId: String)
        =>
        state((chatId, userId)) match
          case ConversationState.SentReattachRequest => recordOnSpecificTime(chatId, userId, text)
          case ConversationState.SentPlotRequest => showPlot(chatId, userId, parseFrom(text), parseTo(text))
          case ConversationState.Base => record(chatId, userId, text)
          case ConversationState.SentHistoryRequest => showHistory(chatId, userId, parseFrom(text), parseTo(text))


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

  private def processSingle(update: Update): Int = // this is atomic
    val userId: Try[String] = Try(update.message().chat().username())
    val chatId: Try[Long] = Try(update.message().chat().id())
    val command = guessCommand(update.message().text())
    try
      command(chatId.get, userId.get)
    catch
      case x: Throwable =>
        logger.error(s"Exception occured processing update ${update.updateId()}: ${x.getMessage}", x)
        dropState(userId = userId.get, chatId = chatId.get)
        UpdatesListener.CONFIRMED_UPDATES_NONE
    UpdatesListener.CONFIRMED_UPDATES_ALL

