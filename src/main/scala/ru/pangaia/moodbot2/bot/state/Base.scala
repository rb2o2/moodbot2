package ru.pangaia.moodbot2.bot.state

import ru.pangaia.moodbot2.TimeUtils
import ru.pangaia.moodbot2.bot.IO
import ru.pangaia.moodbot2.bot.request.*

case object Base extends ChatState:
  override def transition(chatId: Long, userId: String, req: ReqType): (ChatState, IO => Unit) = req match
    case RegularMessage(msg) => (AskMark(msg),
      io =>
        io.db.saveMessage(chatId, userId, msg)
        io.send(chatId, "Внесите оценку")
    )
    case Menu(msg) => (Base,
      io => io.mainMenu(chatId, userId)
    )
    case ReattachCommand(command) => (AskReattachDateTimeMessage(command),
      io =>
        io.send(chatId, "Введите дату/время в формате 'дд.мм.гггг чч-мм'")
    )
    case ReattachCommandMessage(command, msg) => (AskReattachDateTime(msg), 
      io =>
        io.send(chatId, "Введите дату/время в формате 'дд.мм.гггг чч-мм'")
    )
    case ReattachDateTime(command, dateTime) => (AskReattachMessage(dateTime),
      io =>
        io.send(chatId, s"Ваше сообщение на $dateTime:")
    )
    case Plot(command) => (AskPlotPeriod(command),
      io =>
        io.send(chatId, "Введите период")
    )
    case PlotFrom(comPlotFrom, plotFromDate) => (Base,
      io => 
        val from = TimeUtils.parseFrom(plotFromDate, io.tz)
        io.showPlot(chatId, userId, from.get)
    )
    case PlotFromTo(commandPlotFromTo, dateFrom, dateTo) => (Base,
      io =>
        val from = TimeUtils.parseFrom(dateFrom, io.tz)
        val to = TimeUtils.parseFrom(dateTo, io.tz)
        io.showPlot(chatId, userId, from.get, to.get)
    )
    case PlotFromPeriod(command, datePeriod) => (Base,
      io => 
        val from = TimeUtils.parsePeriod(datePeriod)
        io.showPlot(chatId, userId, from.get)
    )
    case History(command) => (AskHistoryPeriod(command),
      io =>
        io.send(chatId, "Введите период")        
    )
    case HistoryFrom(comHistFrom, histFromDate) => (Base,
      io =>
        val from = TimeUtils.parseFrom(histFromDate, io.tz)
        io.showHistory(chatId, userId, from.get)
    )
    case HistoryFromTo(commandHistFromTo, from, to) => (Base,
      io =>
        val dfrom = TimeUtils.parseFrom(from, io.tz)
        val dto = TimeUtils.parseFrom(to, io.tz)
        io.showHistory(chatId, userId, dfrom.get, dto.get)
    )
    case HistoryFromPeriod(command, datePeriod) => (Base,
      io =>
        val from = TimeUtils.parsePeriod(datePeriod)
        io.showHistory(chatId, userId, from.get)
    )
    case RecMark(msg) => (Base,
      io => io.processMark(chatId, userId, msg)
    )
    case x => (Base, _ => ())
