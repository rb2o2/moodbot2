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

import java.io.{BufferedReader, IOException, InputStreamReader, Reader}
import java.lang.reflect.Constructor
import java.sql.{SQLException, Timestamp}
import java.time.*
import java.time.format.DateTimeFormatter
import java.util
import java.util.TimeZone
import java.util.concurrent.{CompletableFuture, SynchronousQueue}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try, Using}

type Conf = String => String

object MoodBot:
  def main(args: Array[String]): Unit =
    val profile = Try(args(0)).getOrElse("dev")
    val config = Config(profile)
    val bot = new MoodBot(config)
    val listener = new MoodUpdatesListener(config, bot)
    bot.setUpdatesListener(listener)
    bot.init()
    bot.awaitTermination()

class MoodBot(config: Conf) extends TelegramBot(config("token")):
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
    System.exit(0)
