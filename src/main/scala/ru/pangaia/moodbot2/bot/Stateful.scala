package ru.pangaia.moodbot2.bot

import ru.pangaia.moodbot2.bot.Messenger
import ru.pangaia.moodbot2.bot.state.{Base, ChatState}
import ru.pangaia.moodbot2.data.Persistence
trait Stateful:
  val stateHistory: collection.mutable.Map[(Long, String), List[ChatState]] =
    new collection.mutable.HashMap[(Long, String), List[ChatState]]()
  def dropState(compositeId: (Long,String)): Unit =
    stateHistory --= Seq(compositeId)
  def changeState(compositeId: (Long, String))(newStateElem: ChatState): Unit =
    if stateHistory.contains(compositeId) then
      newStateElem match
        case Base => dropState(compositeId)
        case _ => stateHistory(compositeId) +:= newStateElem
    else
      newStateElem match
        case Base => ()
        case _ =>  stateHistory(compositeId) = List(newStateElem)
