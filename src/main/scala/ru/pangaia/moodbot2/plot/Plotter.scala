package ru.pangaia.moodbot2.plot

import org.jfree.chart.axis.{NumberTickUnit, TickUnits}
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.{ChartFactory, ChartUtils}
import org.jfree.data.time.{Second, TimeSeries, TimeSeriesCollection, TimeSeriesDataItem}
import ru.pangaia.moodbot2.bot.{Conf, Config}
import ru.pangaia.moodbot2.data.Mark

import java.awt.Color
import java.io.File
import java.sql.Timestamp
import java.util.{Date, UUID}
import scala.collection.Seq

class Plotter(config: Conf) {
  def plotPNGToFile(data: Seq[Mark]): String = {
    val tsc = new TimeSeriesCollection()
    val timeSeries = new TimeSeries(new Second())
    for (mark <- data) {
      timeSeries.add(new TimeSeriesDataItem(new Second(Date.from(mark.creationTimestamp.toInstant)), mark.markValue))
    }
    tsc.addSeries(timeSeries)
    val plot = ChartFactory.createTimeSeriesChart(
      "Настроение",
      "период",
      "оценка",
      tsc,
      false,
      false,
      false)
    val p: XYPlot = plot.getPlot.asInstanceOf[XYPlot]
    p.getRangeAxis.setRange(-2.5, 2.5)
    p.getRangeAxis.setMinorTickMarksVisible(false)
    p.getRangeAxis.setStandardTickUnits({
      val tu = new TickUnits(); tu.add(new NumberTickUnit(1.0)); tu
    })
    plot.setBackgroundPaint(Color.WHITE)
    plot.getPlot.setBackgroundPaint(Color.WHITE)
    val tmp = config("plot.filename.tmpdir").orElse(System.getenv("TEMP"))
    val filename = UUID.randomUUID().toString
    val file = new File(s"$tmp/$filename.png")
    ChartUtils.saveChartAsPNG(file, plot, 800, 300)

    s"$tmp/$filename.png"
  }
}
