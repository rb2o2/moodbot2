package ru.pangaia.moodbot2.bot

enum ConversationState {
  case Base
  case SentMessage
  case SentHistoryRequest
  case SentPlotRequest
  case SentReattachRequest
}
