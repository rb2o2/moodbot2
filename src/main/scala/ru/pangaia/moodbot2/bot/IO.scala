package ru.pangaia.moodbot2.bot

import ru.pangaia.moodbot2.TimeUtils
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.{SendMessage, SendPhoto}

import java.sql.Timestamp
import java.time.Instant
import scala.util.Try

class IO(config: Conf, bot: TelegramBot) extends Plotting(config) with Persisting(config) with Messenger(config, bot):
  val tz: Int = Try(config("zone.offset.default").toInt).getOrElse(3)
  def processMark(chatId: Long, userId: String, msg: String): Unit =
    val markNum = msg match
      case "\ud83d\ude01" => 2
      case "☺️" => 1
      case "\ud83d\ude10" => 0
      case "\ud83d\ude15" => -1
      case "\ud83d\ude29" => -2
      case _ =>
        execute(new SendMessage(chatId, "оценка не распознана :("))
        -100
    db.saveMark(chatId, userId, markNum)
    
  def send(chatId: Long, introText: String): Unit =
    val menuMsg = new SendMessage(chatId, introText)
    execute(menuMsg)

  def mainMenu(chatId: Long, userId: String): Unit =
    execute(new SendMessage(chatId, config("menu.text")))

  def cancel(chatId: Long, userId: String): Unit =
    execute(new SendMessage(chatId, config("cancel.text")))
    
  def showPlot(chatId: Long, userId: String, ts: Timestamp): Unit =
    val now = Timestamp.from(Instant.now())
    showPlot(chatId, userId, ts, now)
  
  def showPlot(chatId: Long, userId: String, from: Timestamp, to: Timestamp): Unit =
    val marks = db.getMarks(chatId, userId, from, to)
    val imageFilePath = plotPNGToFile(marks)
    val req = new SendPhoto(chatId, new java.io.File(imageFilePath))
    execute(req)
    
  def showHistory(chatId: Long, userId: String, from: Timestamp, to: Timestamp): Unit =
    val messages = db.getHistory(chatId, userId, from, to)
    messages.foreach(m =>
      val timeFormatted = TimeUtils.formatTime(m)
      val msg = new SendMessage(chatId, s"$timeFormatted \n${m.messageText}")
      execute(msg)
    )
  
  def showHistory(chtId: Long, userId: String, from: Timestamp): Unit =
    val now = Timestamp.from(Instant.now())
    showHistory(chtId, userId, from, now)
    