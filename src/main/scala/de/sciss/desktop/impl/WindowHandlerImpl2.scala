package de.sciss.desktop.impl

import javax.swing.{JDesktopPane, JInternalFrame}

import de.sciss.desktop.{Desktop, DialogSource, Menu, SwingApplication, Window, WindowHandler}

import scala.swing.Rectangle

final class WindowHandlerImpl2(val application: SwingApplication, val menuFactory: Menu.Root) extends WindowHandler {
  hndl =>

  private var _windows = Vector.empty[Window]

  def showDialog[A](window: Option[Window], source: DialogSource[A]): A = {
    // temporarily disable alwaysOnTop
    val wasOnTop = if (!usesInternalFrames && usesFloatingPalettes) windows.filter { w =>
      val res = w.alwaysOnTop
      if (res) w.alwaysOnTop = false
      res
    } .toList else Nil

    try {
      source.show(window)
    } finally { // make sure to restore original state
      wasOnTop.foreach(_.alwaysOnTop = true)
    }
  }

  def addWindow(w: Window): Unit = {
    _windows :+= w
    MainWindowImpl.add(w)
  }

  def removeWindow(w: Window): Unit = {
    val i = _windows.indexOf(w)
    if (i >= 0) _windows = _windows.patch(i, Vector.empty, 1)
  }

  def windows: Iterator[Window] = _windows.iterator

  def usesInternalFrames  : Boolean = false // !Desktop.isMac
  def usesScreenMenuBar   : Boolean = Desktop.isMac
  def usesFloatingPalettes: Boolean = true

  //  private var _mainWindow: Window = null
  def mainWindow: Window = MainWindowImpl
  //  def mainWindow_=(value: Window) {
  //    if (_mainWindow != null) throw new IllegalStateException("Main window has already been registered")
  //    _mainWindow = value
  //  }

  // mainWindow.front()

  private object MainWindowImpl extends WindowStub {
    import WindowImpl._

    protected def style = Window.Regular
    def handler = hndl

    private val frame = new swing.Frame
    protected val delegate =
      Delegate.frame(this, frame, hasMenuBar = true, screen = handler.usesScreenMenuBar)

    if (Desktop.isMac) {
      makeUndecorated()
      bounds      = new Rectangle(Short.MaxValue, Short.MaxValue, 0, 0)
    } else {
      bounds      = Window.availableSpace
    }

    private val desktop: Option[JDesktopPane] = {
      if (handler.usesInternalFrames) {
        val res = new JDesktopPane
        frame.peer.setContentPane(res)
        Some(res)
      } else None
    }

    def add(w: Window): Unit =
      desktop.foreach { d =>
        w.component.peer match {
          case jif: JInternalFrame =>
            //            jif.addComponentListener(new ComponentAdapter {
            //              override def componentShown(e: ComponentEvent) {
            //                println("SHOWN")
            d.add(jif)
          //              }
          //            })
          //            println("ADD")
          //            jif.setVisible(true)
          case _ =>
        }
      }

    // handler.mainWindow = this
    closeOperation = Window.CloseIgnore
    reactions += {
      case Window.Closing(_) => application.quit()
    }
  }
}