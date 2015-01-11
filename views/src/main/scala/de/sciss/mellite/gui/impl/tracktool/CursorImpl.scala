/*
 *  CursorImpl.scala
 *  (Mellite)
 *
 *  Copyright (c) 2012-2015 Hanns Holger Rutz. All rights reserved.
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

import java.awt.Cursor
import javax.swing.undo.UndoableEdit
import de.sciss.lucre.stm
import de.sciss.mellite.gui.edit.Edits
import de.sciss.synth.proc.Obj
import java.awt.event.MouseEvent
import de.sciss.lucre.expr.{Expr, String => StringEx}
import de.sciss.span.{Span, SpanLike}
import de.sciss.desktop.OptionPane
import scala.swing.{Label, FlowPanel, TextField}
import de.sciss.lucre.synth.Sys

final class CursorImpl[S <: Sys[S]](val canvas: TimelineProcCanvas[S]) extends RegionImpl[S, TrackTool.Cursor] {
  def defaultCursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)
  def name          = "Cursor"
  val icon          = ToolsImpl.getIcon("text")

  private def renameName = "Rename Region"

  protected def handleSelect(e: MouseEvent, hitTrack: Int, pos: Long, region: TimelineObjView[S]): Unit =
    if (e.getClickCount == 2) {
      val ggText  = new TextField(region.name, 24)
      val panel   = new FlowPanel(new Label("Name:"), ggText)

      val pane    = OptionPane(panel, OptionPane.Options.OkCancel, OptionPane.Message.Question, focus = Some(ggText))
      pane.title  = renameName
      val res     = pane.show(None) // XXX TODO: search for window source
      if (res == OptionPane.Result.Ok && ggText.text != region.name) {
        val text    = ggText.text
        val nameOpt = if (text == "" || text == TimelineObjView.Unnamed) None else Some(text)
        dispatch(TrackTool.Adjust(TrackTool.Cursor(nameOpt)))
      }

    } else {
      canvas.timelineModel.modifiableOption.foreach { mod =>
        val it    = canvas.selectionModel.iterator
        val empty = Span.Void: Span.SpanOrVoid
        val all   = (empty /: it) { (res, pv) =>
          pv.spanValue match {
            case sp @ Span(_, _) => res.nonEmptyOption.fold(sp)(_ union sp)
          }
        }
        mod.selection = all
      }
    }

  protected def commitObj(drag: TrackTool.Cursor)(span: Expr[S, SpanLike], obj: Obj[S])
                          (implicit tx: S#Tx, cursor: stm.Cursor[S]): Option[UndoableEdit] =
    Some(Edits.setName(obj, drag.name.map(n => StringEx.newConst(n): Expr[S, String])))
}