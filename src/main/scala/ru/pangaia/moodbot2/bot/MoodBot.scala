package ru.pangaia.moodbot2.bot

import com.pengrad.telegrambot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.{TelegramBot, UpdatesListener}
import com.typesafe.scalalogging.Logger

import java.util
import java.util.concurrent.{CompletableFuture, SynchronousQueue}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

@main
def main(args: Array[String]): Unit = {
  MoodBot.init()
}

object MoodBot {
  val bot = new TelegramBot(Config.getToken)
  def init(): Unit = {
    bot.setUpdatesListener(MoodUpdatesListener)
    Console.in.readLine()
    bot.shutdown()
  }
}

object MoodUpdatesListener extends UpdatesListener {
  val bot: TelegramBot = MoodBot.bot
  private val logger = Logger(getClass.getName)
  private val failedUpdates: util.concurrent.SynchronousQueue[Int] = new SynchronousQueue[Int]()
  override def process(updates: util.List[Update]): Int = {
    updates.asScala.map(processSingle)
    UpdatesListener.CONFIRMED_UPDATES_ALL
  }

  def record(chatId: Long, userId: String, text: String): Unit = {

  }
  def showData(chatId: Long, userId: String): Unit = {

  }

  def showHistory(chatId: Long, userId: String) : Unit = {

  }

  def reattach(cahtId: Long, userId: String, text: String): Unit = {
    ???
  }

  def guessCommand(text: String): (Long, String) => Unit = {
    val firstWord = text.split(" +" )(0).trim().toLowerCase()
    firstWord match {
      case "график"|"plot" => (chatId: Long, userId: String) => showData(chatId, userId)
      case "история"|"history" => (chatId: Long, userId: String) => showHistory(chatId, userId)
      case "%" => (chatId: Long, userId: String) => reattach(chatId, userId, text)
      case _ => (chatId: Long, userId: String) => record(chatId, userId, text)
    }
  }

  def processSingle(update: Update): Int = { // this is atomic
    try {
      val com = guessCommand(update.message().text())
      val userId = update.message().chat().username()
      val chatId = update.message().chat().id()
      com(chatId, userId)
    } catch {
      case x: Exception =>
        logger.error(s"Exception occured processing update: ${update.updateId()}: ${x.getMessage}", x)
        failedUpdates.add(update.updateId())
        UpdatesListener.CONFIRMED_UPDATES_NONE

    }
    UpdatesListener.CONFIRMED_UPDATES_ALL
  }
}
