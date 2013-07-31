/*
 *  AudioFileViewJ.scala
 *  (Mellite)
 *
 *  Copyright (c) 2012-2013 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either
 *  version 2, june 1991 of the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License (gpl.txt) along with this software; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.mellite
package gui
package impl

import de.sciss.sonogram
import java.awt.{Color, Graphics2D}
import scala.util.Failure
import scala.util.Success
import scala.swing.{Swing, Component}
import Swing._

final class AudioFileViewJ(sono: sonogram.Overview, val timelineModel: TimelineModel)
  extends TimelineCanvasImpl {

  import TimelineCanvasImpl._

  // private val numChannels = sono.inputSpec.numChannels
  // private val minFreq     = sono.config.sonogram.minFreq
  // private val maxFreq     = sono.config.sonogram.maxFreq

  //  private val meters  = Vector.fill(numChannels) {
  //    val res   = new PeakMeterBar(javax.swing.SwingConstants.VERTICAL)
  //    res.ticks = 50
  //    res
  //  }

  object canvasComponent extends Component with sonogram.PaintController {
    private var paintFun: Graphics2D => Unit = paintChecker("Calculating...")

    override def paintComponent(g: Graphics2D): Unit = {
      paintFun(g)
      paintPosAndSelection(g, height)
    }

    @inline def width   = peer.getWidth
    @inline def height  = peer.getHeight

    preferredSize = {
      val b = GUI.maximumWindowBounds
      (b.width >> 1, b.height >> 1)
    }

    private def paintChecker(message: String)(g: Graphics2D): Unit = {
      g.setPaint(pntChecker)

      g.fillRect(0, 0, width, height)
      g.setColor(Color.white)
      g.drawString(message, 10, 20)
    }

    private def paintReady(g: Graphics2D): Unit = {
      val visi = timelineModel.visible
      sono.paint(spanStart = visi.start, spanStop = visi.stop, g, 0, 0, width, height, this)
    }

    def adjustGain(amp: Float, pos: Double) = amp

    def imageObserver = peer

    private def ready(): Unit = {
      paintFun = paintReady
      repaint()
    }

    private def failed(exception: Throwable): Unit = {
      val message = s"${exception.getClass.getName} - ${exception.getMessage}"
      paintFun    = paintChecker(message)
      repaint()
    }

    // ---- constructor ----

    sono.onComplete {
      case Success(_) => /* println("SUCCESS"); */ defer(ready())
      case Failure(e) => /* println("FAILURE"); */ defer(failed(e))
    }
  }

  //  private val meterPane   = new BoxPanel(Orientation.Vertical) {
  //    meters.foreach(m => contents += Component.wrap(m))
  //  }
}