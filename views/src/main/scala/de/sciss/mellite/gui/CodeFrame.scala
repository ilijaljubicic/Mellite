/*
 *  CodeFrame.scala
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

package de.sciss
package mellite
package gui

import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.mellite.gui.impl.interpreter.{CodeFrameImpl => Impl}
import de.sciss.synth.proc.{Action, Code, Proc}

object CodeFrame {
  def apply[S <: Sys[S]](obj: Code.Obj[S], hasExecute: Boolean)
                        (implicit tx: S#Tx, workspace: Workspace[S], cursor: stm.Cursor[S],
                         compiler: Code.Compiler): CodeFrame[S] =
    Impl(obj, hasExecute = hasExecute)

  def proc[S <: Sys[S]](proc: Proc[S])
                        (implicit tx: S#Tx, workspace: Workspace[S], cursor: stm.Cursor[S],
                         compiler: Code.Compiler): CodeFrame[S] =
    Impl.proc(proc)

  def action[S <: Sys[S]](action: Action[S])
                         (implicit tx: S#Tx, workspace: Workspace[S], cursor: stm.Cursor[S],
                          compiler: Code.Compiler): CodeFrame[S] =
    Impl.action(action)
}

trait CodeFrame[S <: Sys[S]] extends lucre.swing.Window[S] {
  def codeView: CodeView[S]
}