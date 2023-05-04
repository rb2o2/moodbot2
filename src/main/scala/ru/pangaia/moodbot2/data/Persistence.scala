package ru.pangaia.moodbot2.data

import ru.pangaia.moodbot2.bot.{Conf, Config}
import java.sql.Timestamp
class Persistence(config: Conf) {
  def saveMessage(chatId: Long, user: String, text: String): Unit = {}
  def saveMark(chatId: Long, user: String, mark: Int): Unit = {}
  def getHistory(chatId: Long, userId: String, from: Timestamp, to: Timestamp) : List[Message] = {}
  def getMarks(chatId: Long, userId: String, from: Timestamp, to: Timestamp): List[Mark] = {}
}
