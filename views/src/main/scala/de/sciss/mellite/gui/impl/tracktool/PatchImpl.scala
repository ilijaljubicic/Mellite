/*
 *  PatchImpl.scala
 *  (Mellite)
 *
 *  Copyright (c) 2012-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.mellite
package gui
package impl
package tracktool

import java.awt.{Color, RenderingHints, Point, Toolkit}
import de.sciss.synth.proc.{Obj, Proc}
import de.sciss.mellite.gui.TimelineProcCanvas
import de.sciss.mellite.gui.TrackTool
import de.sciss.lucre.expr.Expr
import de.sciss.span.SpanLike
import java.awt.image.BufferedImage
import java.awt.geom.{Ellipse2D, Area}
import javax.swing.ImageIcon
import java.awt.event.MouseEvent
import de.sciss.mellite.gui.impl.timeline.ProcView
import de.sciss.lucre.synth.Sys
import de.sciss.mellite.gui.impl.component.PaddedIcon
import scala.swing.Insets

object PatchImpl {
  private lazy val image: BufferedImage = {
    val img   = new BufferedImage(17, 17, BufferedImage.TYPE_INT_ARGB)
    val g     = img.createGraphics()
    val shp1  =   new Area(new Ellipse2D.Float(0, 0, 17, 17))
    shp1 subtract new Area(new Ellipse2D.Float(5, 5,  7,  7))
    val shp2  =   new Area(new Ellipse2D.Float(1, 1, 15, 15))
    shp2 subtract new Area(new Ellipse2D.Float(4, 4,  9,  9))
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setColor(Color.white)
    g.fill(shp1)
    g.setColor(Color.black)
    g.fill(shp2)
    g.dispose()
    img
  }

  private lazy val cursor =
    Toolkit.getDefaultToolkit.createCustomCursor(image, new Point(8, 8), "patch")
}
final class PatchImpl[S <: Sys[S]](protected val canvas: TimelineProcCanvas[S])
  extends RegionImpl[S, TrackTool.Patch[S]] with Dragging[S, TrackTool.Patch[S]] {

  import TrackTool._

  def defaultCursor = PatchImpl.cursor
  val name          = "Patch"
  val icon          = new PaddedIcon(new ImageIcon(PatchImpl.image), new Insets(1, 1, 2, 2)) // make it 20x20

  protected type Initial = TimelineObjView[S]

  protected def dragToParam(d: Drag): Patch[S] = {
    val pos   = d.currentPos
    val sink  = canvas.findRegion(frame = pos, hitTrack = d.currentTrack) match {
      case Some(r: ProcView[S]) if r != d.initial /* && r.inputs.nonEmpty */ =>  // region.inpus only carries linked ones!
        Patch.Linked(r)
      case _ =>
        Patch.Unlinked(frame = pos, y = d.currentEvent.getY)
    }
    ??? // Patch(d.initial, sink)
  }

  protected def handleSelect(e: MouseEvent, hitTrack: Int, pos: Long, region: TimelineObjView[S]): Unit =
    /* if (region.outputs.nonEmpty) */ new Drag(e, hitTrack, pos, region)  // region.outputs only carries linked ones!

  protected def commitObj(drag: Patch[S])(span: Expr[S, SpanLike], out: Obj[S])(implicit tx: S#Tx): Unit =
    (drag.sink, out) match {
      case (Patch.Linked(view), Proc.Obj(procObj)) =>
        val in = view.obj()
        ProcActions.linkOrUnlink(procObj, in)

      case _ =>
    }
}