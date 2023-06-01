package ru.pangaia.moodbot2.bot

import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.{InlineKeyboardButton, InlineKeyboardMarkup, KeyboardButton, ReplyKeyboardMarkup}
import com.pengrad.telegrambot.request.{SendMessage, SendPhoto}
import com.pengrad.telegrambot.{BotUtils, TelegramBot, UpdatesListener}
import com.typesafe.scalalogging.Logger
import ru.pangaia.moodbot2.TimeUtils
import ru.pangaia.moodbot2.bot.request.ReqType
import ru.pangaia.moodbot2.bot.state.*
import ru.pangaia.moodbot2.data.Message
import ru.pangaia.moodbot2.plot.NoDataInPlotRangeException

import java.io.IOException
import java.sql.{SQLException, Timestamp}
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.NoSuchElementException
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import java.{sql, util}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

class MoodUpdatesListener(config: Conf, bot: TelegramBot) extends UpdatesListener
  with Stateful:

  private val logger = Logger(getClass.getName)
  logger.debug("Logger initialized")
  private val failedUpdates: BlockingQueue[(Update, Exception)] = new LinkedBlockingQueue()
  private val tz: Int = Try(config("zone.offset.default").toInt).getOrElse(3)
  private val io = new IO(config, bot)

  override def process(updates: util.List[Update]): Int =
    updates.asScala.map(processSingle(_)(config)).last

  def processSingle(update: Update)(implicit config: Conf): Int =
    val userId: Try[String] = Try(update.message().chat().username())
    val chatId: Try[Long] = Try(update.message().chat().id())
    try
      val req: ReqType = ReqType(update.message().text())
      processRequest(chatId.get, userId.get, req)
    catch
      case x: Exception =>
        logger.error(s"Exception occurred while processing update ${update.updateId()}: ${x.getMessage}", x)
        failedUpdates.put((update, x))
    update.updateId()

  def processRequest(chatId: Long, userId: String, req: ReqType): Unit =
    val chatStatePrev: ChatState = Try(stateHistory(chatId, userId)).getOrElse(List(Base)).head
    val (chatStateNew, ioEffect): (ChatState, IO => Unit) = chatStatePrev.transition(chatId, userId, req)
//---------------//I/O here
    ioEffect(io)
//---------------//
    chatStateNew match
      case Base => dropState((chatId, userId))
      case x if x.getClass == chatStatePrev.getClass => x.repeat()
      case x => changeState((chatId, userId))(x)
