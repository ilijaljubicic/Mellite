package de.sciss.mellite
package gui
package impl
package interpreter

import de.sciss.desktop
import de.sciss.scalainterpreter.{InterpreterPane, Interpreter, CodePane}
import java.io.{IOException, FileInputStream, File}
import scala.swing.event.Key
import swing.Component
import de.sciss.desktop.{KeyStrokes, Window}

// careful... tripping over SI-3809 "illegal cyclic reference involving class Array"...
// actually SI-7481
private[gui] object InterpreterFrameImpl {
  val boom = Array(1, 2, 3)  // forcing scalac to recompile, so it doesn't crash

  private def readFile(file: File): String = {
    val fis = new FileInputStream(file)
    try {
      val arr = new Array[Byte](fis.available())
      fis.read(arr)
      new String(arr, "UTF-8")
    } finally {
      fis.close()
    }
  }

  def apply(): InterpreterFrame = {
    val codeCfg = CodePane.Config()

    val file = new File(/* new File( "" ).getAbsoluteFile.getParentFile, */ "interpreter.txt")
    if (file.isFile) try {
      codeCfg.text = readFile(file)
    } catch {
      case e: IOException => e.printStackTrace()
    }

    val txnKeyStroke  = KeyStrokes.shift + KeyStrokes.alt + Key.Enter
    // var txnCount      = 0

    def txnExecute(): Unit = {
      intp.codePane.getSelectedTextOrCurrentLine.foreach { txt =>
        // val txnId = txnCount
        // txnCount += 1
        Application.documentHandler.activeDocument.foreach {
          case cd: Workspace.Confluent =>
                        //            val txnTxt =
            //              s"""class _txnBody$txnId(implicit t: scala.concurrent.stm.InTxn) {
            //             |import MelliteDSL._
            //             |$txt
            //             |}
            //             |val _txnRes$txnId = doc.cursor.atomic(implicit t => new _txnBody$txnId)
            //             |import _txnRes$txnId._""".stripMargin
            val txnTxt =
              s"""confluentDocument.cursors.cursor.step { implicit tx =>
             |import de.sciss.mellite.gui.InterpreterFrame.Bindings.{confluentDocument => doc}
             |val _imp = proc.ExprImplicits[proc.Confluent]
             |import _imp._
             |$txt
             |}""".stripMargin
            intp.interpret(txnTxt)
          case _ =>
        }
      }
    }

    codeCfg.keyMap += txnKeyStroke -> (() => txnExecute())

    lazy val intpCfg = Interpreter.Config()
    intpCfg.imports = List(
      "de.sciss.mellite._",
      "de.sciss.synth._",
      "Ops._",
      // "concurrent.duration._",
      "proc.Implicits._",
      "de.sciss.span.Span",
      "MelliteDSL._",
      "gui.InterpreterFrame.Bindings._"
    )

    //      intpCfg.bindings = Seq( NamedParam( "replSupport", replSupport ))
    //         in.bind( "s", classOf[ Server ].getName, ntp )
    //         in.bind( "in", classOf[ Interpreter ].getName, in )

    //      intpCfg.out = Some( LogWindow.instance.log.writer )

    lazy val intp = InterpreterPane(interpreterConfig = intpCfg, codePaneConfig = codeCfg)

    new InterpreterFrame {
      val component = new de.sciss.desktop.impl.WindowImpl {
        frame =>

        def handler = Application.windowHandler

        override def style = Window.Auxiliary

        title           = "Interpreter"
        contents        = Component.wrap(intp.component)
        closeOperation  = Window.CloseDispose
        pack()
        desktop.Util.centerOnScreen(this)
        front()
      }
    }
  }
}
