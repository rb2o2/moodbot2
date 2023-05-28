package ru.pangaia.moodbot2.bot

import com.pengrad.telegrambot.{BotUtils, TelegramBot, UpdatesListener}
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.{InlineKeyboardButton, InlineKeyboardMarkup, KeyboardButton, ReplyKeyboardMarkup}
import com.pengrad.telegrambot.request.{SendMessage, SendPhoto}
import com.typesafe.scalalogging.Logger
import ru.pangaia.moodbot2.TimeUtils
import ru.pangaia.moodbot2.data.Message
import ru.pangaia.moodbot2.plot.NoDataInPlotRangeException

import java.io.IOException
import java.sql.{SQLException, Timestamp}
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.{sql, util}
import java.util.NoSuchElementException
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

class MoodUpdatesListener(config: Conf, bot: TelegramBot) extends UpdatesListener
  with ChatState
  with Plotting(config)
  with Persisting(config)
  with Messenger(config, bot):
  private val logger = Logger(getClass.getName)
  logger.debug("Logger initialized")
  private val failedUpdates: BlockingQueue[(Update, Exception)] = new LinkedBlockingQueue()
  private val tz: Int = Try(config("zone.offset.default").toInt).getOrElse(3)

  override def process(updates: util.List[Update]): Int =
    updates.asScala.map(processSingle).last

  def processSingle(update: Update): Int =
    val userId: Try[String] = Try(update.message().chat().username())
    val chatId: Try[Long] = Try(update.message().chat().id())
    try
      val command = guessCommand(update.message().text())
      command(chatId.get, userId.get)
    catch
      case x: Exception =>
        logger.error(s"Exception occurred while processing update ${update.updateId()}: ${x.getMessage}", x)
        failedUpdates.put((update, x))
    update.updateId()

  private def guessCommand(text: String): (Long, String) => Unit =
    val firstWord = text.split(" +")(0).trim().replace("/", "")
    val textRest = text.stripPrefix(firstWord).trim()
    logger.info(s"Text rest = $textRest")
    firstWord.toLowerCase() match
      case ("start" | "старт" | "menu" | "меню") =>
        (chatId: Long, userId: String)
        =>
        menu(chatId, userId, config("menu.text"))
      case "cancel" | "отмена" =>
        (chatId: Long, userId: String)
        =>
        menu(chatId, userId, config("cancel.text"))
      case "график" | "plot" => (chatId: Long, userId: String)
        => {
          val now = Timestamp.from(Instant.now())
          textRest.toLowerCase() match
            case ("все" | "all" | "всё") =>
              val longAgo = new Timestamp(0)
              showPlot(chatId, userId, Try(longAgo), Try(now))
            case ("month" | "месяц") =>
              val monthAgo = Timestamp.from(Instant.now().minus(30L, ChronoUnit.DAYS))
              showPlot(chatId, userId, Try(monthAgo), Try(now))
            case ("week" | "неделя") =>
              val weekAgo = Timestamp.from(Instant.now().minus(7L, ChronoUnit.DAYS))
              showPlot(chatId, userId, Try(weekAgo), Try(now))
            case ("year" | "год") =>
              val yearAgo = Timestamp.from(Instant.now().minus(365L, ChronoUnit.DAYS))
              showPlot(chatId, userId, Try(yearAgo), Try(now))
            case _ =>
              showPlotAskPeriod(chatId, userId)
        }
      case "история" | "history" => (chatId: Long, userId: String)
        =>
        {
          val now = Timestamp.from(Instant.now())
          textRest.toLowerCase() match
            case ("все" | "all" | "всё") =>
              val longAgo = new Timestamp(0)
              showHistory(chatId, userId, Try(longAgo), Try(now))
            case ("month" | "месяц") =>
              val monthAgo = Timestamp.from(Instant.now().minus(30L, ChronoUnit.DAYS))
              showHistory(chatId, userId, Try(monthAgo), Try(now))
            case ("week" | "неделя") =>
              val weekAgo = Timestamp.from(Instant.now().minus(7L, ChronoUnit.DAYS))
              showHistory(chatId, userId, Try(weekAgo), Try(now))
            case ("year" | "год") =>
              val yearAgo = Timestamp.from(Instant.now().minus(365L, ChronoUnit.DAYS))
              showHistory(chatId, userId, Try(yearAgo), Try(now))
            case _ =>
              showHistoryAskPeriod(chatId, userId)
        }
      case "%" => (chatId: Long, userId: String)
        =>
        val message = textRest
        recordOnSpecificAskDateTime(chatId, userId, message)
      case _ => (chatId: Long, userId: String)
        =>
        Try(state((chatId, userId))) match
          case Success(StateSeqElem(ConversationState.SentReattachRequest, lastMsg) :: _) =>
            recordOnSpecificTime(chatId, userId, lastMsg,
              TimeUtils.parseDateTime(text, tz))
          case Success(StateSeqElem(ConversationState.SentPlotRequest, lastMsg) :: _) =>
            showPlot(chatId, userId,
              TimeUtils.parseFrom(text, tz),
              TimeUtils.parseTo(text, tz))
          case Failure(_) =>
            record(chatId, userId, text)
          case Success(StateSeqElem(ConversationState.SentHistoryRequest, lastMsg) :: _) =>
            showHistory(chatId, userId,
              TimeUtils.parseFrom(text, tz),
              TimeUtils.parseTo(text, tz))
          case Success(StateSeqElem(ConversationState.SentMessage, lastMsg) :: _) =>
            recordMark(chatId, userId, firstWord)
          case x@_ =>
            logger.warn(s"Unknown chat #($chatId, $userId) state: $x")
            dropState(chatId, userId)

  private def record(chatId: Long, userId: String, text: String): Unit =
    db.saveMessage(chatId, userId, text)
    val response = new SendMessage(chatId, "оцените настроение")
    val keyboard = new ReplyKeyboardMarkup(Array(
      new KeyboardButton("\ud83d\ude01"),
      new KeyboardButton("☺️"),
      new KeyboardButton("\ud83d\ude10"),
      new KeyboardButton("\ud83d\ude15"),
      new KeyboardButton("\ud83d\ude29")
    )).oneTimeKeyboard(true)
    response.replyMarkup(keyboard)
    execute(response)
    prependOrCreateState((chatId, userId), StateSeqElem(ConversationState.SentMessage, text))

  private def showPlotAskPeriod(chatId: Long, userId: String): Unit =
    val req = new SendMessage(chatId, "введите период в формате 'дд.мм.гггг дд.мм.гггг'")
    execute(req)
    prependOrCreateState((chatId, userId), StateSeqElem(ConversationState.SentPlotRequest, ""))

  private def showPlot(chatId: Long, userId: String, from: Try[Timestamp], to: Try[Timestamp]): Unit =
    try
      val marks = db.getMarks(chatId, userId, from.get, to.get)
      val imageFilePath = plotPNGToFile(marks)
      val req = new SendPhoto(chatId, new java.io.File(imageFilePath))
      execute(req)
      dropState(chatId, userId)
    catch
      case _ : NoDataInPlotRangeException =>
        logger.warn(s"Data range ${from.get} - ${to.get} is empty")
        dropState(chatId, userId)
        val msg = new SendMessage(chatId, "За этот период нет оценок")
        execute(msg)
      case x : (ArrayIndexOutOfBoundsException | DateTimeException) =>
        logger.error(s"Error parsing dates in chat#$chatId: ${x.getMessage}", x)
        val errMsg = new SendMessage(chatId, "Неверный формат дат")
        execute(errMsg)
        showPlotAskPeriod(chatId, userId)
      case x : IOException =>
        logger.error(s"IO error while plotting in chat#$chatId: ${x.getMessage}", x)
        val errMsg = new SendMessage(chatId, "Не могу построить график")
        dropState(chatId, userId)
        execute(errMsg)
      case x : Exception =>
        logger.error(s"Unknown error: ${x.getMessage}", x)
        val errMsg = new SendMessage(chatId, "Ошибка! :(")
        dropState(chatId, userId)
        execute(errMsg)

  private def showHistoryAskPeriod(chatId: Long, userId: String): Unit =
    val req = new SendMessage(chatId, "введите период в формате 'дд.мм.гггг дд.мм.гггг'")
    execute(req)
    prependOrCreateState((chatId, userId), StateSeqElem(ConversationState.SentHistoryRequest, ""))

  private def showHistory(chatId: Long, userId: String, dateFrom: Try[Timestamp], dateTo: Try[Timestamp]): Unit =
    try
      val messages = db.getHistory(chatId, userId, dateFrom.get, dateTo.get)
      messages.foreach(m =>
        val timeFormatted = TimeUtils.formatTime(m)
        val msg = new SendMessage(chatId, s"$timeFormatted \n${m.messageText}")
        execute(msg)
      )
      dropState(chatId, userId)
    catch
      case x: (ArrayIndexOutOfBoundsException | DateTimeException) =>
        logger.error(s"Error parsing input date. ${x.getMessage}", x)
        val errMsg = new SendMessage(chatId, "Неверный формат дат")
        execute(errMsg)
        showHistoryAskPeriod(chatId, userId)
      case x: Exception =>
        logger.error(x.getMessage, x)
        dropState(chatId, userId)

  private def recordOnSpecificAskDateTime(chatId: Long, userId: String, text: String): Unit =
    val req = new SendMessage(chatId, "На какую дату и время перенести? Введите в формате дд.мм.гггг чч-мм")
    execute(req)
    prependOrCreateState((chatId, userId), StateSeqElem(ConversationState.SentReattachRequest, text))

  private def recordOnSpecificTime(chatId: Long, userId: String, msg: String, ts: Try[Timestamp]): Unit =
    try
      db.saveMessageToDateTime(chatId, userId, msg, ts.get)
      dropState(chatId, userId)
    catch
      case x : DateTimeException =>
        logger.error(s"Error parsing input date. ${x.getMessage}", x)
        val errMsg = new SendMessage(chatId, "Неверный формат дат")
        execute(errMsg)
        recordOnSpecificAskDateTime(chatId, userId, msg)

  private def menu(chatId: Long, userId: String, introText: String): Unit =
    val menuMsg = new SendMessage(chatId, introText)
    execute(menuMsg)
    dropState(chatId, userId)

  private def recordMark(chatId: Long, userId: String, mark: String): Unit =
    val markNum = mark match
      case "\ud83d\ude01" => 2
      case "☺️" => 1
      case "\ud83d\ude10" => 0
      case "\ud83d\ude15" => -1
      case "\ud83d\ude29" => -2
      case _ =>
        execute(new SendMessage(chatId, "оценка не распознана :("))
        -100
      db.saveMark(chatId, userId, markNum)
      dropState(chatId, userId)
