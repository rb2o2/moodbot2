package ru.pangaia.moodbot2.data

import ru.pangaia.moodbot2.bot.{Conf, Config}
import scalikejdbc.*
import org.postgresql.Driver

import java.nio.charset.StandardCharsets
import java.sql.Timestamp
import java.util.{Base64, UUID}
class Persistence(config: Conf) {
  Class.forName("org.postgresql.Driver")
  ConnectionPool.singleton(config("db.url"), config("db.user"), config("db.pw"))

  private def sanitize(str: String): String =
    Base64.getEncoder.encodeToString(str.getBytes(StandardCharsets.UTF_8))

  private def unsane(base: String): String =
    new String(Base64.getDecoder.decode(base), StandardCharsets.UTF_8)

  def saveMessage(chatId: Long, user: String, text: String): Unit =
    saveUserIfNotExists(user)
    val id = UUID.randomUUID()
    val userSane = sanitize(user)
    val textSane = sanitize(text)
    DB.autoCommit {
      implicit session: DBSession =>
        sql"""insert into message (id, user_id, message_text, creation_timestamp) values
             ($id::uuid,
              (select users.id from users where users.username = $userSane),
              $textSane,
              current_timestamp);"""
          .update
          .apply()
    }

  def saveMessageToDateTime(chatId: Long, user: String, text: String, ts: Timestamp): Unit =
    saveUserIfNotExists(user)
    val id = UUID.randomUUID()
    val userSane = sanitize(user)
    val textSane = sanitize(text)
    DB.autoCommit {
      implicit session: DBSession =>
        sql"""insert into message (id, user_id, message_text, creation_timestamp, reported_time) values 
              ($id::uuid,
               (select users.id from users where users.username = $userSane),
               $textSane,
               current_timestamp,
               $ts);"""
          .update.apply()
    }
    
  def getUserId(username: String): Option[UUID] =
    val userSane = sanitize(username)
    DB.readOnly {
      implicit session: DBSession =>
        sql"select id from users where users.username = $userSane;"
          .map(rs => UUID.fromString(rs.string("id")))
          .single
          .apply()
    }

  def saveMark(chatId: Long, user: String, mark: Int): Unit =
    val userSane = sanitize(user)
    val id = UUID.randomUUID()
    DB.autoCommit {
      implicit session: DBSession =>
        sql"""insert into mark (id, user_id, mark_value, creation_timestamp) values
             ($id::uuid,
              select id from users where users.username = $userSane,
              $mark,
              timestamp());"""
          .update
          .apply()
    }
  def getHistory(chatId: Long, username: String, from: Timestamp, to: Timestamp) : List[Message] =
    val userSane = sanitize(username)
    DB.readOnly {
      implicit session: DBSession =>
        sql"""select * from message
             where message.user_id = (select id from users where users.username = $userSane) and
                   message.creation_timestamp <= $to and creation_timestamp >= $from;"""
          .map(rs => Message(
            UUID.fromString(rs.string("id")),
            UUID.fromString(rs.string("user_id")),
            unsane(rs.string("message_text")),
            rs.timestamp("creation_timestamp"),
            Option(rs.timestamp("reported_time"))))
          .list
          .apply()
    }

  def getMarks(chatId: Long, userId: String, from: Timestamp, to: Timestamp): List[Mark] =
    val userSane = sanitize(userId)
    DB.readOnly {
      implicit session: DBSession =>
        sql"""select * from mark
             where mark.user_id = (select id from users where users.username = $userSane) and
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

  private def saveUserIfNotExists(userNameInsane: String): Unit =
    val userSane = sanitize(userNameInsane)
    val id = UUID.randomUUID()
    DB.autoCommit {
      implicit session: DBSession =>
        sql"""insert into users (id, username) values
             ($id, $userSane) on conflict do nothing;"""
          .update
          .apply()
    }
}
