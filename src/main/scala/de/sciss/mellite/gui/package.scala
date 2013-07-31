/*
 *  package.scala
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

import concurrent.stm.TxnLocal
import de.sciss.lucre.stm.Txn
import java.awt.EventQueue
import collection.immutable.{IndexedSeq => Vec}
import scala.swing.Swing

package object gui {
  private val guiCode = TxnLocal(init = Vec.empty[() => Unit], afterCommit = handleGUI)
  //   private lazy val primaryMod   = Toolkit.getDefaultToolkit.getMenuShortcutKeyMask

  private def handleGUI(seq: Vec[() => Unit]): Unit = {
    def exec(): Unit =
      seq.foreach { fun =>
        try {
          fun()
        } catch {
          case e: Throwable => e.printStackTrace()
        }
      }

    defer(exec())
  }

  def requireEDT(): Unit = require(EventQueue.isDispatchThread)

  def defer(thunk: => Unit): Unit =
    if (EventQueue.isDispatchThread) thunk else Swing.onEDT(thunk)

  def guiFromTx(body: => Unit)(implicit tx: Txn[_]): Unit =
    guiCode.transform(_ :+ (() => body))(tx.peer)

  private def wordWrap(s: String, margin: Int = 80): String = {
    if (s == null) return "" // fuck java
    val sz = s.length
    if (sz <= margin) return s
    var i = 0
    val sb = new StringBuilder
    while (i < sz) {
      val j = s.lastIndexOf(" ", i + margin)
      val found = j > i
      val k = if (found) j else i + margin
      sb.append(s.substring(i, math.min(sz, k)))
      i = if (found) k + 1 else k
      if (i < sz) sb.append('\n')
    }
    sb.toString()
  }

  def formatException(e: Throwable): String = {
    e.getClass.toString + " :\n" + wordWrap(e.getMessage) + "\n" +
      e.getStackTrace.take(10).map("   at " + _).mkString("\n")
  }

 //   def primaryMenuKey( ch: Char )  : KeyStroke = KeyStroke.getKeyStroke( ch, primaryMod )
  //   def primaryMenuKey( code: Int ) : KeyStroke = KeyStroke.getKeyStroke( code, primaryMod )
}