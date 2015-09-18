name          := "Mellite"

version       := "0.3.1-SNAPSHOT"

organization  := "de.sciss"

homepage      := Some(url(s"https://github.com/Sciss/${name.value}"))

description   := "An application based on SoundProcesses"

licenses      := Seq("GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt"))

scalaVersion  := "2.10.5"

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/maven-releases/"

libraryDependencies ++= Seq(
  "de.sciss" %% "soundprocesses"     % "1.9.1",
  "de.sciss" %% "scalacolliderswing" % "1.9.1",  // AudioBusMeter in 1.9.1
  "de.sciss" %% "lucrestm-bdb"       % "2.0.4",
  "de.sciss" %% "desktop"            % "0.3.4",
  "de.sciss" %% "swingplus"          % "0.0.2",
  "de.sciss" %% "audiowidgets-app"   % "1.3.1",
  "de.sciss" %% "sonogramoverview"   % "1.6.2",  // bug in 1.6.1
  "de.sciss" %% "serial"             % "1.0.2",  // bug in 1.0.0
  "de.sciss" %% "treetable-scala"    % "1.3.8",
  "de.sciss" %% "fscapejobs"         % "1.4.1",
  "de.sciss" %% "strugatzki"         % "2.1.0",
  "de.sciss" %% "fileutil"           % "1.0.0",
  "de.sciss" %% "play-json-sealed"   % "0.2.0",
  "de.sciss" %  "weblaf"             % "1.28"
)

// retrieveManaged := true

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

scalacOptions += "-no-specialization"

// scalacOptions ++= Seq("-Xelide-below", "INFO")

initialCommands in console :=
  """import de.sciss.mellite._
    |import de.sciss.indeterminus._""".stripMargin

fork in run := true  // required for shutdown hook, and also the scheduled thread pool, it seems
