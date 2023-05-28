package ru.pangaia.moodbot2.bot
trait ChatState:
  case class StateSeqElem(state: ConversationState, message: String)
  type State = collection.mutable.Map[(Long, String), List[StateSeqElem]]
  val state: State =
    new collection.mutable.HashMap[(Long, String), List[StateSeqElem]]()

  def dropState(chatId: Long, userId: String): Unit =
    state --= Seq((chatId, userId))
    
  def prependOrCreateState(compositeId: (Long, String), newStateElem: StateSeqElem): Unit =
    if state.contains(compositeId) then
      state(compositeId) +:= newStateElem
    else
      state(compositeId) = List(newStateElem)  