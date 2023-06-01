package ru.pangaia.moodbot2.bot.state

import ru.pangaia.moodbot2.TimeUtils
import ru.pangaia.moodbot2.bot.IO
import ru.pangaia.moodbot2.bot.request.*

case class AskPlotPeriod(msg: String) extends ChatState:
  override def transition(chatId: Long, userId: String, req: ReqType): (ChatState, IO => Unit) = req match
    case DateReq(date) => (Base,
      io => 
        val ts = TimeUtils.parseFrom(date, io.tz)
        io.showPlot(chatId, userId, ts.get)
    )
    case Period(period) => (Base,
      io =>
        val ts = TimeUtils.parsePeriod(period)
        io.showPlot(chatId, userId, ts.get)
    )
    case DateFromToReq(from, to) => (Base,
      io => 
      val tsFrom = TimeUtils.parseFrom(from, io.tz)
      val tsTo = TimeUtils.parseFrom(to, io.tz)
      io.showPlot(chatId, userId, tsFrom.get, tsTo.get)
    )
    case Menu(msg) => (Base,
      io => io.mainMenu(chatId, userId)
    )
    case Cancel(msg) => (Base,
      io => io.cancel(chatId, userId)
    )
    case _ => (AskPlotPeriod(msg), 
      io => io.send(chatId, "Период не распознан")
    )
