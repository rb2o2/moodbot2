package ru.pangaia.moodbot2.bot.state

import ru.pangaia.moodbot2.bot.IO
import ru.pangaia.moodbot2.bot.request.*

case class AskReattachDateTimeMessage(msg: String) extends ChatState:
  override def transition(chatId: Long, userId: String, req: ReqType): (ChatState, IO => Unit) = req match
    case DateTimeReq(dateTime) => (AskReattachMessage(dateTime),
      io => io.send(chatId, s"Ваше сообщение на $dateTime:")
    )
    case Cancel(command) => (Base,
      io => io.cancel(chatId, userId)
    )
    case Menu(com) => (Base, 
      io => io.mainMenu(chatId, userId)
    )
    case _ => (AskReattachDateTimeMessage(msg),
      io => io.send(chatId, "Неизвестные дата/время")
    )
