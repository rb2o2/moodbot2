package ru.pangaia.moodbot2.bot.state

import ru.pangaia.moodbot2.TimeUtils
import ru.pangaia.moodbot2.bot.IO
import ru.pangaia.moodbot2.bot.request.*

case class AskReattachDateTime(msg: String) extends ChatState:
  override def transition(chatId: Long, userId: String, req: ReqType): (ChatState, IO => Unit) = req match
    case DateTimeReq(dateTime) => (Base,
      io =>
        val ts = TimeUtils.parseDateTime(dateTime, io.tz)
        io.db.saveMessageToDateTime(chatId, userId, msg, ts.get)
    )
    case Menu(msg) => (Base,
      io => io.mainMenu(chatId, userId)      
    )
    case Cancel(msg) => (Base,
      io => io.cancel(chatId, userId)
    )
    case x => (AskReattachDateTime(msg), io =>
      io.send(chatId, "неизвестные дата/время")
    )
