package ru.pangaia.moodbot2.bot.state

import ru.pangaia.moodbot2.TimeUtils
import ru.pangaia.moodbot2.bot.IO
import ru.pangaia.moodbot2.bot.request.*

case class AskReattachMessage(msg: String) extends ChatState:
  override def transition(chatId: Long, userId: String, req: ReqType): (ChatState, IO => Unit) = req match
    case Menu(msg) => (Base, io => io.mainMenu(chatId, userId))
    case Cancel(msg) => (Base, io => io.cancel(chatId, userId))
    case x => (Base,
      iO =>
        val ts = TimeUtils.parseDateTime(msg, iO.tz)
        iO.db.saveMessageToDateTime(chatId, userId, x.text(), ts.get)
    )
