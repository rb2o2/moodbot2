package ru.pangaia.moodbot2.bot.state

import ru.pangaia.moodbot2.bot.IO
import ru.pangaia.moodbot2.bot.request.ReqType

trait ChatState:
  def transition(chatId: Long, userId: String, req: ReqType): (ChatState, IO => Unit)
  def repeat(): Unit = ()

