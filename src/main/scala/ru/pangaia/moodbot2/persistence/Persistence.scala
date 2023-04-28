package ru.pangaia.moodbot2.persistence

import ru.pangaia.moodbot2.bot.Config

import java.sql.Timestamp
class Persistence(config: Config) {
  def saveMessage(chatId: Long, user: String, text: String): Unit = {}
  def saveMark(chatId: Long, user: String, mark: Int): Unit = {}
  def getHistory(chatId: Long, userId: String, from: Timestamp, to: Timestamp) : List[Message] = {}
  def getMarks(chatId: Long, userId: String, from: Timestamp, to: Timestamp): List[Mark] = {}
}
