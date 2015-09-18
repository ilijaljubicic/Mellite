/*
 *  CursorsFrameImpl.scala
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
package document

import scala.swing._
import collection.immutable.{IndexedSeq => Vec}
import de.sciss.synth.expr.ExprImplicits
import de.sciss.lucre.{confluent, stm}
import java.util.{Locale, Date}
import java.text.SimpleDateFormat
import de.sciss.file._
import de.sciss.treetable.{TreeTableCellRenderer, TreeColumnModel, TreeTable, AbstractTreeModel}
import de.sciss.synth.proc
import de.sciss.mellite._
import de.sciss.mellite.gui._
import de.sciss.mellite.gui.impl.ComponentHolder
import de.sciss.desktop
import de.sciss.treetable.TreeTableSelectionChanged
import de.sciss.model.Change

object CursorsFrameImpl {
  type S = proc.Confluent
  type D = S#D

  def apply(document: ConfluentDocument)(implicit tx: D#Tx): DocumentCursorsFrame = {
    val root      = document.cursors
    val rootView  = createView(document, parent = None, elem = root)
    val view      = new Impl(document, rootView)(tx.system)

    /* val obs = */ root.changed.react { implicit tx => upd =>
      log(s"DocumentCursorsFrame update $upd")
      view.elemUpdated(rootView, upd.changes)
    }
    // XXX TODO: remember obs for disposal

    guiFromTx {
      view.guiInit()
    }

    view.addChildren(rootView, root)

    view
  }

  private def createView(document: ConfluentDocument, parent: Option[CursorView], elem: Cursors[S, D])
                        (implicit tx: D#Tx): CursorView = {
    import document._
    val name    = elem.name.value
    val created = confluent.Sys.Acc.info(elem.seminal        ).timeStamp
    val updated = confluent.Sys.Acc.info(elem.cursor.position).timeStamp
    new CursorView(elem = elem, parent = parent, children = Vector.empty,
      name = name, created = created, updated = updated)
  }

  private final class CursorView(val elem: Cursors[S, D], val parent: Option[CursorView],
                                 var children: Vec[CursorView], var name: String,
                                 val created: Long, var updated: Long)

  private final class Impl(val document: ConfluentDocument, _root: CursorView)(implicit cursor: stm.Cursor[D])
    extends DocumentCursorsFrame with ComponentHolder[desktop.Window] {

    type Node = CursorView

    private var mapViews = Map.empty[Cursors[S, D], Node]

    private class ElementTreeModel extends AbstractTreeModel[Node] {
      lazy val root: Node = _root // ! must be lazy. suckers....

      def getChildCount(parent: Node): Int = parent.children.size
      def getChild(parent: Node, index: Int): Node = parent.children(index)
      def isLeaf(node: Node): Boolean = node.children.isEmpty
      def getIndexOfChild(parent: Node, child: Node): Int = parent.children.indexOf(child)
      def getParent(node: Node): Option[Node] = node.parent

      def valueForPathChanged(path: TreeTable.Path[Node], newValue: Node): Unit =
        println(s"valueForPathChanged($path, $newValue)")

      def elemAdded(parent: Node, idx: Int, view: Node): Unit = {
        // if (DEBUG) println(s"model.elemAdded($parent, $idx, $view)")
        require(idx >= 0 && idx <= parent.children.size)
        parent.children = parent.children.patch(idx, Vector(view), 0)
        fireNodesInserted(view)
      }

      def elemRemoved(parent: Node, idx: Int): Unit = {
        // if (DEBUG) println(s"model.elemRemoved($parent, $idx)")
        require(idx >= 0 && idx < parent.children.size)
        val v = parent.children(idx)
        // this is frickin insane. the tree UI still accesses the model based on the previous assumption
        // about the number of children, it seems. therefore, we must not update children before
        // returning from fireNodesRemoved.
        fireNodesRemoved(v)
        parent.children  = parent.children.patch(idx, Vector.empty, 1)
      }

      def elemUpdated(view: Node): Unit = fireNodesChanged(view)
    }

    private var _model: ElementTreeModel  = _
    private var t: TreeTable[Node, TreeColumnModel[Node]] = _

    private def actionAdd(parent: Node): Unit = {
      val format  = new SimpleDateFormat("yyyy MM dd MM | HH:mm:ss", Locale.US) // don't bother user with alpha characters
      val ggValue = new FormattedTextField(format)
      ggValue.peer.setValue(new Date(parent.updated))
      val nameOpt = GUI.keyValueDialog(value = ggValue, title = "Add New Cursor",
        defaultName = "branch", window = Some(comp))
      (nameOpt, ggValue.peer.getValue) match {
        case (Some(name), seminalDate: Date) =>
          val parentElem = parent.elem
          parentElem.cursor.step { implicit tx =>
            implicit val dtx = proc.Confluent.durable(tx)
            val seminal = tx.inputAccess.takeUntil(seminalDate.getTime)
            // lucre.event.showLog = true
            parentElem.addChild(seminal)
            // lucre.event.showLog = false
          }
        case _ =>
      }
    }

    private def elemRemoved(parent: Node, idx: Int, child: Cursors[S, D])(implicit tx: D#Tx): Unit =
      mapViews.get(child).foreach { cv =>
        // NOTE: parent.children is only updated on the GUI thread through the model.
        // no way we could verify the index here!!
        //
        // val idx1 = parent.children.indexOf(cv)
        // require(idx == idx1, s"elemRemoved: given idx is $idx, but should be $idx1")
        cv.children.zipWithIndex.reverse.foreach { case (cc, cci) =>
          elemRemoved(cv, cci, cc.elem)
        }
        mapViews -= child
        guiFromTx {
          _model.elemRemoved(parent, idx)
        }
      }

    def addChildren(parentView: Node, parent: Cursors[S, D])(implicit tx: D#Tx): Unit =
      parent.descendants.toList.zipWithIndex.foreach { case (c, ci) =>
        elemAdded(parent = parentView, idx = ci, child = c)
      }

    private def elemAdded(parent: Node, idx: Int, child: Cursors[S, D])(implicit tx: D#Tx): Unit = {
      val cv   = createView(document, parent = Some(parent), elem = child)
      // NOTE: parent.children is only updated on the GUI thread through the model.
      // no way we could verify the index here!!
      //
      // val idx1 = parent.children.size
      // require(idx == idx1, s"elemAdded: given idx is $idx, but should be $idx1")
      mapViews += child -> cv
      guiFromTx {
        _model.elemAdded(parent, idx, cv)
      }
      addChildren(cv, child)
    }

    def elemUpdated(v: Node, upd: Vec[Cursors.Change[S, D]])(implicit tx: D#Tx): Unit =
      upd.foreach {
        case Cursors.ChildAdded  (idx, child) => elemAdded  (v, idx, child)
        case Cursors.ChildRemoved(idx, child) => elemRemoved(v, idx, child)
        case Cursors.Renamed(Change(_, newName))  => guiFromTx {
          v.name = newName
          _model.elemUpdated(v)
        }
        case Cursors.ChildUpdate(Cursors.Update(source, childUpd)) => // recursion
          mapViews.get(source).foreach { cv =>
            elemUpdated(cv, childUpd)
          }
      }

    def guiInit(): Unit = {
      requireEDT()
      require(comp == null, "Initialization called twice")

      _model = new ElementTreeModel

      val colName = new TreeColumnModel.Column[Node, String]("Name") {
        def apply(node: Node): String = node.name

        def update(node: Node, value: String): Unit =
          if (value != node.name) {
            cursor.step { implicit tx =>
              val expr = ExprImplicits[D]
              import expr._
              node.elem.name_=(value)
            }
          }

        def isEditable(node: Node) = true
      }

      val colCreated = new TreeColumnModel.Column[Node, Date]("Origin") {
        def apply(node: Node): Date = new Date(node.created)
        def update(node: Node, value: Date) = ()
        def isEditable(node: Node) = false
      }

      val colUpdated = new TreeColumnModel.Column[Node, Date]("Updated") {
        def apply(node: Node): Date = new Date(node.updated)
        def update(node: Node, value: Date) = ()
        def isEditable(node: Node) = false
      }

      val tcm = new TreeColumnModel.Tuple3[Node, String, Date, Date](colName, colCreated, colUpdated) {
        def getParent(node: Node): Option[Node] = node.parent
      }

      t = new TreeTable(_model, tcm)
      t.showsRootHandles    = true
      t.autoCreateRowSorter = true  // XXX TODO: hmmm, not sufficient for sorters. what to do?
      t.renderer = new TreeTableCellRenderer {
        private val dateFormat = new SimpleDateFormat("E d MMM yy | HH:mm:ss", Locale.US)

        private val component = TreeTableCellRenderer.Default
        def getRendererComponent(treeTable: TreeTable[_, _], value: Any, row: Int, column: Int,
                                 state: TreeTableCellRenderer.State): Component = {
          val value1 = value match {
            case d: Date  => dateFormat.format(d)
            case _        => value
          }
          val res = component.getRendererComponent(treeTable, value1, row = row, column = column, state = state)
          res // component
        }
      }
      val tabCM = t.peer.getColumnModel
      tabCM.getColumn(0).setPreferredWidth(128)
      tabCM.getColumn(1).setPreferredWidth(184)
      tabCM.getColumn(2).setPreferredWidth(184)

      val ggAdd = Button("+") {
        t.selection.paths.headOption.foreach { path =>
          val v = path.last
          actionAdd(parent = v)
        }
      }
      ggAdd.peer.putClientProperty("JButton.buttonType", "roundRect")
      ggAdd.enabled = false

      val ggDelete: Button = Button("\u2212") {
        println("Delete")
      }
      ggDelete.enabled = false
      ggDelete.peer.putClientProperty("JButton.buttonType", "roundRect")

      lazy val ggView: Button = Button("View") {
        t.selection.paths.foreach { path =>
          val elem = path.last.elem
          implicit val cursor = elem.cursor
          cursor.step { implicit tx =>
            DocumentElementsFrame(document)
          }
        }
      }
      ggView.enabled = false
      ggView.peer.putClientProperty("JButton.buttonType", "roundRect")

      t.listenTo(t.selection)
      t.reactions += {
        case e: TreeTableSelectionChanged[_, _] =>  // this crappy untyped event doesn't help us at all
          val selSize = t.selection.paths.size
          ggAdd   .enabled  = selSize == 1
          // ggDelete.enabled  = selSize > 0
          ggView  .enabled  = selSize == 1 // > 0
      }

      lazy val folderButPanel = new FlowPanel(ggAdd, ggDelete, ggView)

      val scroll    = new ScrollPane(t)
      scroll.border = null

      comp = new desktop.impl.WindowImpl {
        def style       = desktop.Window.Regular
        def handler     = Mellite.windowHandler

        title           = s"${document.folder.base} : Cursors"
        file            = Some(document.folder)
        closeOperation  = desktop.Window.CloseIgnore
        contents        = new BorderPanel {
          add(scroll,         BorderPanel.Position.Center)
          add(folderButPanel, BorderPanel.Position.South )
        }

        pack()
        // centerOnScreen()
        GUI.placeWindow(this, 1f, 0f, 24)
        front()
        // add(folderPanel, BorderPanel.Position.Center)
      }
    }
  }
}