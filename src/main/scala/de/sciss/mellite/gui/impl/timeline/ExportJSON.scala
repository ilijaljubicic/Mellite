package de.sciss.mellite
package gui
package impl.timeline

import de.sciss.span.Span
import de.sciss.synth.Curve
import de.sciss.synth.proc.{Attribute, Grapheme, Scan, ProcKeys, FadeSpec, Sys}
import play.api.libs.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue}

import scala.collection.breakOut

object ExportJSON {
  def apply[S <: Sys[S]](view: TimelineView[S])(implicit tx: S#Tx): JsValue = {
    // these casts are so we don't need to touch binary compatibility
    // and cause confusion with the 0.3.x semantic versioning.
    val viewImpl    = view.asInstanceOf[TimelineViewImpl.Impl[S]]
    val globalImpl  = viewImpl.globalView.asInstanceOf[GlobalProcsViewImpl.Impl[S]]
    val views       = globalImpl.procSeq ++ viewImpl.procViews.iterator.toIndexedSeq

    var idCount     = 0
    def mkID(): Int = {
      val res = idCount
      idCount += 1
      res
    }

    val audioElems = views.flatMap { procView =>
      if (procView.audio.isEmpty) None else {
        val proc        = procView.proc
        val scan        = proc.scans.get(ProcKeys.graphAudio).get
        val regionStart = procView.span.asInstanceOf[Span.HasStart].start

        val timed = scan.sources.map {
          case Scan.Link.Grapheme(g) => g.at(regionStart).get
          case _ => sys.error("not a grapheme")
        } .toList.head

        // val graphemeStart = timed.time.value
        val audioElem = timed.mag.asInstanceOf[Grapheme.Elem.Audio[S]]
        Some(audioElem)
      }
    } .distinct

    val locs        = audioElems.map(_.artifact.location).distinct
    val locsIdx     = locs.map { l => (l, mkID()) }
    val locsIDs     = locsIdx.toMap

    val locsJSON    = locsIdx.map { case (loc, lIdx) =>
      JsObject(Seq(
        "id" -> JsNumber(lIdx),
        "directory" -> JsString(loc.directory.getPath)
      ))
    }

    val audioMap    = audioElems.map { elem => elem.value -> (elem.artifact.location, elem.artifact.child) } .toMap
    val audioVals   = audioMap.toIndexedSeq.sortBy(_._2._2.path)
    val audioIDs    = audioMap.keySet.map { a => (a, mkID()) } .toMap

    val audioJSON   = audioVals.map { case (audioVal, (loc, child)) =>
      val aFields = Vector.newBuilder[(String, JsValue)]
      val aIdx = audioIDs(audioVal)
      aFields                             += "id"     -> JsNumber(aIdx)
      aFields                             += "locRef" -> JsNumber(locsIDs(loc))
      aFields                             += "file"   -> JsString(child.path)
      if (audioVal.offset != 0L ) aFields += "offset" -> JsNumber(audioVal.offset)
      if (audioVal.gain   != 1.0) aFields += "gain"   -> JsNumber(audioVal.gain)
      // audioVal.spec
      JsObject(aFields.result())
    }

    val viewsIdx    = views.map { p => (p, mkID()) }
    val viewIDs     = viewsIdx.toMap

    val viewsJSON   = viewsIdx.map { case (procView, pIdx) =>
      import procView._

      def mkFade(spec: FadeSpec.Value): JsValue = {
        require(spec.floor == 0f, "ExportJSON - FadeSpec floor not yet supported")
        var fFields = List.empty[(String, JsValue)]
        spec.curve match {
          case Curve.parametric(c) => fFields ::= "curve" -> JsNumber(c.toDouble)
          case Curve.linear =>
          case other => sys.error(s"ExportJSON - Curve type $other not yet supported")
        }
        fFields ::= "length" -> JsNumber(spec.numFrames)
        JsObject(fFields)
      }

      def mkLinks(links: ProcView.LinkMap[S]): JsValue = {
        val entries: Seq[(String, JsValue)] = links.map { case (key, seq) =>
          val targets = seq.map { link =>
            JsObject(Seq("idRef" -> JsNumber(viewIDs(link.target)), "key" -> JsString(link.targetKey)))
          }
          key -> JsArray(targets)
        } (breakOut)
        JsObject(entries)
      }

      val fields = Vector.newBuilder[(String, JsValue)]
      fields                             += "id"      -> JsNumber(pIdx)
      nameOption.foreach(value => fields += "name"    -> JsString(value))
      span match {
        case hs: Span.HasStart => fields += "start"   -> JsNumber(hs.start)
        case _ =>
      }
      span match {
        case hs: Span.HasStop  => fields += "stop"    -> JsNumber(hs.stop)
        case _ =>
      }
      busOption.foreach(value =>  fields += "bus"     -> JsNumber(value))
      if (gain != 1.0)            fields += "gain"    -> JsNumber(gain)
      if (muted)                  fields += "muted"   -> JsBoolean(muted)
      fields                             += "track"   -> JsNumber(track)
      if (fadeIn .numFrames > 0)  fields += "fadeIn"  -> mkFade(fadeIn)
      if (fadeOut.numFrames > 0)  fields += "fadeOut" -> mkFade(fadeOut)

      if (inputs .nonEmpty)       fields += "inputs"  -> mkLinks(inputs)
      if (outputs.nonEmpty)       fields += "outputs" -> mkLinks(outputs)

      audio.foreach { segm =>
        // N.B. span.start refers to timeline, i.e. audio-offset is
        // actually region.span.start - segm.span.start

        // println(s"name = $name - span $span - audio-span ${segm.span}")
        val off     = span.asInstanceOf[Span.HasStart].start - segm.span.start
        var aFields = List.empty[(String, JsValue)]
        if (off != 0L) aFields ::= "offset" -> JsNumber(off)
        aFields   ::= "idRef" -> JsNumber(audioIDs(segm.value))
        fields     += "audio" -> JsObject(aFields)
      }

//      proc.attributes.apply[Attribute.String](ProcKeys.attrGraphSource).foreach { source =>
//        println("-------- SOURCE")
//        println(source)
//      }

      JsObject(fields.result())
    }

    JsObject(Seq(
      "locations"   -> JsArray(locsJSON),
      "audio"       -> JsArray(audioJSON),
      "regions"     -> JsArray(viewsJSON)
    ))
  }
}
