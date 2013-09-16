# Mellite

## statement

Mellite is a graphical front end for [SoundProcesses](http://github.com/Sciss/SoundProcesses). It is (C)opyright 2012&ndash;2013 by Hanns Holger Rutz. All rights reserved. Mellite is released under the [GNU General Public License](http://github.com/Sciss/Mellite/blob/master/licenses/Mellite-License.txt) and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`.

BUG REPORT BRANCH

## reproducing the bug

Clone this branch. Run `./sbt gen-idea` to create the IntelliJ IDEA project. Open that project. Open file `src/main/scala/de/sciss/mellite/gui/impl/audiofile/ViewImpl.scala`. Observation: IDEA code analysis hangs forever.
