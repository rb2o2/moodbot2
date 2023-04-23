package ru.pangaia.moodbot2.bot

import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.{TelegramBot, UpdatesListener}

import java.util
import java.util.concurrent.CompletableFuture

object MoodBot {
  val bot = new TelegramBot(Config.getToken)
  bot.setUpdatesListener(MoodUpdatesListener)
  Console.in.readLine()
  bot.shutdown()
}

object MoodUpdatesListener extends UpdatesListener {
  override def process(updates: util.List[Update]): Int = {
    filter(updates)
    batchProcess(updates)
  }

  private def filter(updates: util.List[Update]): util.List[Update] = {
    updates
  }
  private def batchProcess(updates: util.List[Update]): Int = {
    val volSize = updates.size / Config.maxBatches
    for {
      i <- 0 to Config.maxBatches
    } {
      val batch: util.List[Update] = new util.ArrayList[Update]()
      batch.addAll(updates.subList(i * volSize, Math.min((i + 1) * volSize, updates.size)))
      processInThread(batch)
    }
  }

  private def processInThread(batch: util.List[Update]): CompletableFuture[Int] = synchronized {

  }
}
