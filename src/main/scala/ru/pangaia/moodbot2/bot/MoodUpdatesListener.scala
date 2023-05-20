package ru.pangaia.moodbot2.bot

import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.{InlineKeyboardButton, InlineKeyboardMarkup}
import com.pengrad.telegrambot.request.{SendMessage, SendPhoto}
import com.typesafe.scalalogging.Logger
import ru.pangaia.moodbot2.data.Message

import java.io.IOException
import java.sql.{SQLException, Timestamp}
import java.time.format.DateTimeFormatter
import java.time.*
import java.util
import java.util.NoSuchElementException
import java.util.concurrent.SynchronousQueue
import scala.util.{Failure, Success, Try}
import scala.jdk.CollectionConverters.*

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

  private def processSingle(update: Update): Int =
    val userId: Try[String] = Try(update.message().chat().username())
    val chatId: Try[Long] = Try(update.message().chat().id())
    val command = guessCommand(update.message().text())
    try
      command(chatId.get, userId.get)
    catch
      case x: Exception =>
        logger.error(s"Exception occurred while processing update ${update.updateId()}: ${x.getMessage}", x)
        failedUpdates.put(update)
        UpdatesListener.CONFIRMED_UPDATES_NONE
    UpdatesListener.CONFIRMED_UPDATES_ALL

  private def guessCommand(text: String): (Long, String) => Unit =
    val firstWord = text.split(" +")(0).trim().toLowerCase().replace("/", "")
    try
      firstWord match
        case mark: ("\ud83d\ude01" | "☺️" | "\ud83d\ude10" | "\ud83d\ude15" | "\ud83d\ude29") =>
          (chatId: Long, userId: String)
          =>
          recordMark(chatId, userId, mark)
          dropState(userId, chatId)
        case "start" | "старт" =>
          (chatId: Long, userId: String)
          =>
          menu(chatId, userId, config("menu.text"))
          dropState(userId, chatId)
        case "cancel" | "отмена" =>
          (chatId: Long, userId: String)
          =>
          menu(chatId, userId, config("cancel.text"))
          dropState(userId, chatId)
        case "график" | "plot" => (chatId: Long, userId: String)
          => showPlotAskPeriod(chatId, userId)
        case "история" | "history" => (chatId: Long, userId: String)
          => showHistoryAskPeriod(chatId, userId)
        case "%" => (chatId: Long, userId: String)
          =>
          val message = text.stripPrefix("%").trim()
          recordOnSpecificAskDateTime(chatId, userId, message)
        case _ => (chatId: Long, userId: String)
          =>
          Try(state((chatId, userId))) match
            case Success((ConversationState.SentReattachRequest, head :: _)) =>
              recordOnSpecificTime(chatId, userId, head.toString, parseDateTime(text))
            case Success((ConversationState.SentPlotRequest, _)) =>
              showPlot(chatId, userId, parseFrom(text), parseTo(text))
            case Success((ConversationState.Base, _)) | Failure(_) =>
              record(chatId, userId, text)
            case Success((ConversationState.SentHistoryRequest, _)) =>
              showHistory(chatId, userId, parseFrom(text), parseTo(text))
              dropState(userId, chatId)
            case x@_ =>
              logger.warn(s"Unknown chat #($chatId) state: $x")
    catch
      case x: RuntimeException =>
        (chatId: Long, userId: String)
        =>
        dropState(userId, chatId)
        logger.error(x.getMessage, x)

  def record(chatId: Long, userId: String, text: String): Unit =
    db.saveMessage(chatId, userId, text)
    val response = new SendMessage(chatId, "оцените настроение")
    val keyboard = new InlineKeyboardMarkup(Array(
      new InlineKeyboardButton("\ud83d\ude01"),
      new InlineKeyboardButton("☺️"),
      new InlineKeyboardButton("\ud83d\ude10"),
      new InlineKeyboardButton("\ud83d\ude15"),
      new InlineKeyboardButton("\ud83d\ude29")
    ))
    response.replyMarkup(keyboard)
    execute(response)
    state((chatId, userId)) = (ConversationState.SentMessage, List())

  private def showPlotAskPeriod(chatId: Long, userId: String): Unit =
    val req = new SendMessage(chatId, "введите период в формате 'дд.мм.гггг дд.мм.гггг'")
    execute(req)
    state((chatId, userId)) = (ConversationState.SentPlotRequest, List())

  private def showPlot(chatId: Long, userId: String, from: Try[Timestamp], to: Try[Timestamp]): Unit =
    try
      val marks = db.getMarks(chatId, userId, from.get, to.get)
      val imageFilePath = plotPNGToFile(marks)
      val req = new SendPhoto(chatId, new java.io.File(imageFilePath))
      execute(req)
      dropState(userId, chatId)
    catch
      case x : (ArrayIndexOutOfBoundsException | DateTimeException) =>
        logger.error(s"Error parsing dates in chat#$chatId: ${x.getMessage}", x)
        val errMsg = new SendMessage(chatId, "Неверный формат дат")
        execute(errMsg)
        showPlotAskPeriod(chatId, userId)
      case x : IOException =>
        logger.error(s"IO error while plotting in chat#$chatId: ${x.getMessage}", x)
        val errMsg = new SendMessage(chatId, "Не могу построить график")
        dropState(userId, chatId)
        execute(errMsg)
      case x : Exception =>
        logger.error(s"Unknown error: ${x.getMessage}", x)
        val errMsg = new SendMessage(chatId, "Ошибка! :(")
        dropState(userId, chatId)
        execute(errMsg)

  private def showHistoryAskPeriod(chatId: Long, userId: String): Unit =
    val req = new SendMessage(chatId, "введите период в формате 'дд.мм.гггг дд.мм.гггг'")
    execute(req)
    state((chatId, userId)) = (ConversationState.SentHistoryRequest, List())

  private def showHistory(chatId: Long, userId: String, dateFrom: Try[Timestamp], dateTo: Try[Timestamp]): Unit =
    try
      val messages = db.getHistory(chatId, userId, dateFrom.get, dateTo.get)
      messages.foreach(m =>
        val timeFormatted = formatTime(m)
        val msg = new SendMessage(chatId, s"$timeFormatted \n${m.messageText}")
        execute(msg)
      )
    catch
      case x: (ArrayIndexOutOfBoundsException | DateTimeException) =>
        logger.error(s"Error parsing input date. ${x.getMessage}", x)
        val errMsg = new SendMessage(chatId, "Неверный формат дат")
        execute(errMsg)
        showHistoryAskPeriod(chatId, userId)
      case x: Exception =>
        logger.error(x.getMessage, x)
        dropState(userId, chatId)

  private def recordOnSpecificAskDateTime(chatId: Long, userId: String, text: String): Unit =
    val req = new SendMessage(chatId, "На какую дату и время перенести? Введите в формате дд.мм.гггг чч-мм")
    execute(req)
    state((chatId, userId)) = (ConversationState.SentReattachRequest, List(text))

  private def recordOnSpecificTime(chatId: Long, userId: String, msg: String, ts: Try[Timestamp]): Unit =
    try
      db.saveMessageToDateTime(chatId, userId, msg, ts.get)
    catch
      case x : Exception =>
        dropState(userId, chatId)
        logger.error(x.getMessage, x)

  private def menu(chatId: Long, userId: String, introText: String): Unit =
    val menuMsg = new SendMessage(chatId, introText)
    execute(menuMsg)

  private def recordMark(chatId: Long, userId: String, mark: String): Unit =
    if state((chatId, userId)) == (ConversationState.SentMessage, List()) then
      val markNum = mark match
        case "\ud83d\ude01" => 2
        case "☺️" => 1
        case "\ud83d\ude10" => 0
        case "\ud83d\ude15" => -1
        case "\ud83d\ude01" => -2
        case _ =>
          execute(new SendMessage(chatId, "оценка не распознана :("))
          -100
      db.saveMark(chatId, userId, markNum)
      state((chatId, userId)) = (ConversationState.Base, List())

  private def formatTime(m: Message): String =
    val time = if m.reportedTime != null then m.reportedTime else m.creationTimestamp
    DateTimeFormatter.BASIC_ISO_DATE.toFormat().format(time)

  private def parseDateTime(dt: String): Try[Timestamp] =
    Try(Timestamp.from(
      LocalDateTime.from(DateTimeFormatter.ofPattern("dd.MM.yyyy HH-mm").parse(dt))
        .toInstant(ZoneOffset.ofHours(config("zone.offset.default").toInt))))

  private def parseFrom(text: String): Try[Timestamp] =
    val fromText = text.split(" +")(0)
    Try(dateToTimestamp(fromText))

  private def parseTo(text: String): Try[Timestamp] =
    val toText = Try(text.split(" +")(1))
    toText.map(dateToTimestamp)

  private def dateToTimestamp(d: String): Timestamp =
    val toText = d.split("\\.")
    val toDayOfMonth = toText(0).toInt
    val toMonth = toText(1).toInt
    val toYear = toText(2).toInt
    val date = LocalDate.of(toYear, toMonth, toDayOfMonth)
    val instant = LocalDateTime.of(date, LocalTime.MIDNIGHT)
      .toInstant(ZoneOffset.ofHours(config("zone.offset.default").toInt))
    Timestamp.from(instant)
