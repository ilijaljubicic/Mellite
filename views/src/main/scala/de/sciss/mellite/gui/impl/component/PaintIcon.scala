package de.sciss.mellite.gui.impl.component

import java.awt.{Paint, Graphics, Component}
import javax.swing.Icon

import scala.swing.Graphics2D

class PaintIcon(var paint: Paint, width: Int, height: Int) extends Icon {
  def getIconWidth : Int = width
  def getIconHeight: Int = height

  def paintIcon(c: Component, g: Graphics, x: Int, y: Int): Unit = {
     val g2 = g.asInstanceOf[Graphics2D]
    g2.setPaint(paint)
    g2.fillRect(x, y, width, height)
  }
}
