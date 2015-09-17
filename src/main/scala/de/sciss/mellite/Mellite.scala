/*
 *  Mellite.scala
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

package de.sciss
package mellite

import com.alee.laf.WebLookAndFeel
import de.sciss.desktop.{WindowHandler, Menu}
import de.sciss.desktop.impl.{WindowHandlerImpl2, SwingApplicationImpl}
import de.sciss.mellite.gui.{LogFrame, MainFrame, MenuBar}
import de.sciss.synth.proc.AuralSystem

object Mellite extends SwingApplicationImpl("Mellite") {
  type Document = mellite.Document[_]

  // lucre.event    .showLog = true
  // lucre.confluent.showLog = true
  // synth.proc.showAuralLog     = true
  // synth.proc.showLog          = true
  // synth.proc.showTransportLog = true
  // showLog                     = true

  protected lazy val menuFactory: Menu.Root = MenuBar()

  override lazy val windowHandler: WindowHandler = {
    WebLookAndFeel.install()  // tricky place
    new WindowHandlerImpl2(this, menuFactory)
  }

  private lazy val _aural = AuralSystem()

  implicit def auralSystem: AuralSystem = _aural

  override protected def init(): Unit = {
    LogFrame.instance
    new MainFrame
  }
}
