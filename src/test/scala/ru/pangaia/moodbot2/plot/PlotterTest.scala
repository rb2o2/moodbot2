package ru.pangaia.moodbot2.plot

import org.scalatest.flatspec.AnyFlatSpec
import ru.pangaia.moodbot2.bot.{Conf, Config}
import ru.pangaia.moodbot2.data.Mark

import java.io.File
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class PlotterTest extends AnyFlatSpec {
  val config: Conf = Config("test")
  val plotter: Plotter = new Plotter(config)

  "plotter.plotPNGToFile" should "not write plot if there are no data" in {
    assertThrows[NoDataInPlotRangeException](plotter.plotPNGToFile(Seq.empty), "does not throw " +
      "NoDataInPlotRangeException")
  }

  "plotter.plotPNGToFile" should "write plot to file when data is present" in {
    val data = Seq(Mark(UUID.randomUUID(), UUID.randomUUID(), 2, Timestamp.from(Instant.now())))
    val pathname = plotter.plotPNGToFile(data)
    assert(new File(pathname).exists(), s"file $pathname not found")
  }
}
