package ru.pangaia.moodbot2.bot

import org.jfree.chart.renderer.xy.DeviationRenderer.State

enum State {
  case Base
  case SentMessage
  case SentMark
  case SentHistoryRequest
  case SentHistoryDates
  case SentPlotRequest
  
}
