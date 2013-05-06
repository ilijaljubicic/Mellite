package de.sciss
package mellite
package gui
package impl

import de.sciss.span.Span
import de.sciss.synth.expr.{ExprImplicits, Longs, Ints, Spans}
import de.sciss.synth.proc.{ProcGroup, Sys, graph, Scan, Grapheme, Attribute, Proc}
import de.sciss.lucre.bitemp.{BiGroup, BiExpr}
import de.sciss.synth.SynthGraph

object DropAudioRegionAction {
  def apply[S <: Sys[S]](group: BiGroup.Modifiable[S, Proc[S], Proc.Update[S]], drop: AudioFileDnD.Drop,
                         data: AudioFileDnD.Data[S])(implicit tx: S#Tx) {
    val expr    = ExprImplicits[S]
    import expr._

    // val elem    = data.source()
    // val elemG = elem.entity
    val time    = drop.frame
    val sel     = data.drag.selection
    val spanV   = Span(time, time + sel.length)
    val span    = Spans.newVar[S](spanV)
    val proc    = Proc[S]
    // proc.name_=(elem.name)
    val attr    = proc.attributes
    val track   = drop.y / 32
    attr.put(ProcKeys.track, Attribute.Int(Ints.newVar(track)))
    val scanw   = proc.scans.add(TimelineView.AudioGraphemeKey)
    // val scand   = proc.scans.add("dur")
    val grw     = Grapheme.Modifiable[S]
    // val grd     = Grapheme.Modifiable[S]

    // we preserve data.source(), i.e. the original audio file offset
    // ; therefore the grapheme element must start `selection.start` frames
    // before the insertion position `drop.frame`
    val gStart  = Longs.newVar(time - sel.start)  // wooopa, could even be a bin op at some point
    val gElem   = data.source().entity  // could there be a Grapheme.Element.Var?
    val bi: Grapheme.TimedElem[S] = BiExpr(gStart, gElem)
    grw.add(bi)
    // val gv = Grapheme.Value.Curve
    // val crv = gv(dur -> stepShape)
    // grd.add(time -> crv)
    scanw.source_=(Some(Scan.Link.Grapheme(grw)))
    // scand.source_=(Some(Scan.Link.Grapheme(grd)))
    val sg = SynthGraph {
      import synth._
      import ugen._
      val sig   = graph.scan("sig").ar(0)
      // val env   = EnvGen.ar(Env.linen(0.2, (duri - 0.4).max(0), 0.2))
      Out.ar(0, sig /* * env */)
    }
    proc.graph_=(sg)
    group.add(span, proc)
  }
}