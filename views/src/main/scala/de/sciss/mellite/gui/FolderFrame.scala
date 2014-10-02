/*
 *  DocumentElementsFrame.scala
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

package de.sciss
package mellite
package gui

import de.sciss.synth.proc.Folder
import impl.document.{FolderFrameImpl => Impl}
import lucre.stm
import de.sciss.lucre.synth.Sys

object FolderFrame {
  /** Creates a new frame for document elements.
    *
    * @param workspace        the workspace whose root to display
    * @param name             optional window name
    * @param isWorkspaceRoot  if `true`, closes the workspace when the window closes; if `false` does nothing
    *                         upon closing the window
    */
  def apply[S <: Sys[S], S1 <: Sys[S1]](name: ExprView[S1#Tx, Option[String]], isWorkspaceRoot: Boolean)
                        (implicit tx: S#Tx, workspace: Workspace[S], cursor: stm.Cursor[S],
                         bridge: S#Tx => S1#Tx): FolderFrame[S] =
    Impl(nameObs = name, folder = workspace.root(), isWorkspaceRoot = isWorkspaceRoot)

  def apply[S <: Sys[S], S1 <: Sys[S1]](name: ExprView[S1#Tx, Option[String]], folder: Folder[S])
                                       (implicit tx: S#Tx, workspace: Workspace[S], cursor: stm.Cursor[S],
                                        bridge: S#Tx => S1#Tx): FolderFrame[S] = {
    Impl(nameObs = name, folder = folder, isWorkspaceRoot = false)
  }
}

trait FolderFrame[S <: Sys[S]] extends lucre.swing.Window[S] {
  // def view: FolderView[S]
}