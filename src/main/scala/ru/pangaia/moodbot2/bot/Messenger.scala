package ru.pangaia.moodbot2.bot

import com.pengrad.telegrambot.{Callback, TelegramBot}
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.response.BaseResponse

import scala.util.Try

trait Messenger(config: Conf, bot: TelegramBot):
  def execute[T <: BaseRequest[T, R], R <: BaseResponse](req: T, callback: Callback[T, R]): Unit =
    bot.execute(req, callback)

  def execute[T <: BaseRequest[T, R], R <: BaseResponse](request: T): Try[R] =
    Try(bot.execute(request))
    