package ru.pangaia.moodbot2.bot

import ru.pangaia.moodbot2.data.Mark
import ru.pangaia.moodbot2.plot.Plotter

trait Plotting(config: Conf):
  private val plotter: Plotter = Plotter(config)

  def plotPNGToFile: List[Mark] => String = plotter.plotPNGToFile
