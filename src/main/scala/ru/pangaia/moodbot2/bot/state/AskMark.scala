package ru.pangaia.moodbot2.bot.state

import ru.pangaia.moodbot2.bot.IO
import ru.pangaia.moodbot2.bot.request.*

case class AskMark(msg: String) extends ChatState:
  override def transition(chatId: Long, userId: String, req: ReqType): (ChatState, IO => Unit) = req match
    case RecMark(msg) => (Base,
      io => io.processMark(chatId, userId, msg)
    )
    case Menu(msg) => (Base,
      io =>
        io.mainMenu(chatId, userId)
    )
    case Cancel(msg) => (Base, _ => ())
    case x => (Base,
      io =>
        io.send(chatId, "оценка не распознана :(")
    )
