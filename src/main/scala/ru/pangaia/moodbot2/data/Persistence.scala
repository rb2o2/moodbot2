package ru.pangaia.moodbot2.data

import ru.pangaia.moodbot2.bot.{Conf, Config}
import scalikejdbc.{DB, DBSession, scalikejdbcSQLInterpolationImplicitDef}

import java.sql.Timestamp
import java.util.UUID
class Persistence(config: Conf) {
  private def sanitizeUsername(str: String): String =
    //todo stub
    str

  private def sanitizeText(str: String): String =
    //todo stub
    str

  def saveMessage(chatId: Long, user: String, text: String): Unit =
    val id = UUID.randomUUID()
    val userSane = sanitizeUsername(user)
    val textSane = sanitizeText(text)
    DB.autoCommit {
      implicit session: DBSession =>
        sql"""insert into message (id, user_id, message_text, creation_timestamp) values
             ($id,
              select id from users where users.username = '$userSane',
              '$textSane',
              timestamp());"""
          .execute
          .apply()
    }
  def getUserId(username: String): Option[UUID] =
    val userSane = sanitizeUsername(username)
    DB.readOnly {
      implicit session: DBSession =>
        sql"select id from users where users.username = '$userSane';"
          .map(rs => UUID.fromString(rs.string("id")))
          .single
          .apply()
    }

  def saveMark(chatId: Long, user: String, mark: Int): Unit =
    val userSane = sanitizeUsername(user)
    val id = UUID.randomUUID()
    DB.autoCommit {
      implicit session: DBSession =>
        sql"""insert into mark (id, user_id, mark_value, creation_timestamp) values
             ($id,
              select id from users where users.username = '$userSane',
              $mark,
              timestamp());"""
          .execute
          .apply()
    }
  def getHistory(chatId: Long, username: String, from: Timestamp, to: Timestamp) : List[Message] =
    val userSane = sanitizeUsername(username)
    DB.readOnly {
      implicit session: DBSession =>
        sql"""select id, user_id, message_text, creation_timestamp, reported_time from message
             where message.user_id = (select id from users where users.username = '$userSane') and
                   message.creation_timestamp <= $to and creation_timestamp >= $from; """
          .map(rs => Message(
            UUID.fromString(rs.string("id")),
            UUID.fromString(rs.string("user_id")),
            rs.string("message_text"),
            rs.timestamp("creation_timestamp"),
            Option(rs.timestamp("reported_time"))))
          .list
          .apply()
    }


  def getMarks(chatId: Long, userId: String, from: Timestamp, to: Timestamp): List[Mark] =
    val userSane = sanitizeUsername(userId)
    DB.readOnly {
      implicit session: DBSession =>
        sql"""select id, user_id, mark_value, creation_timestamp from mark
             where mark.user_id = (select id from users where users.username = '$userSane') and
                   mark.creation_timestamp <= $to and mark.creation_timestamp >= $from;"""
          .map(rs => Mark(
            UUID.fromString(rs.string("id")),
            UUID.fromString(rs.string("user_id")),
            rs.int("mark_value"),
            rs.timestamp("creation_timestamp")
          ))
          .list
          .apply()
    }
}
