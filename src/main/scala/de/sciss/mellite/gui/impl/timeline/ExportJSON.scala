package de.sciss.mellite
package gui
package impl.timeline

import de.sciss.file.File
import de.sciss.synth.proc.Sys

object ExportJSON {
  def apply[S <: Sys[S]](view: TimelineView[S], f: File): Unit = {
    val viewImpl  = view.asInstanceOf[TimelineViewImpl.Impl[S]]
    val views     = viewImpl.procViews
    views.iterator.foreach { procView =>
      /*
      procView.nameOption
      procView.span
      procView.isGlobal
      procView.audio
      procView.busOption  // is this used except for isGlobal?
      procView.fadeIn
      procView.fadeOut
      procView.gain
      procView.muted
      procView.track
      procView.inputs   // Map[String, Vec[ProcView.Link[S]]]
      procView.outputs
      */

      import procView._

      println(s"name = $name - inputs ${inputs.keys.mkString(", ")} - outputs ${outputs.keys.mkString(", ")}")
    }
  }
}
