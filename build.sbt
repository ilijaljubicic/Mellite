lazy val baseName                   = "Mellite"
lazy val baseNameL                  = baseName.toLowerCase
lazy val fullDescr                  = "A computer music application based on SoundProcesses"
lazy val projectVersion             = "2.2.0-SNAPSHOT"

lazy val loggingEnabled             = true

// ---- core dependencies ----

lazy val soundProcessesVersion      = "3.4.0"
lazy val interpreterPaneVersion     = "1.7.3"
lazy val scalaColliderUGenVersion   = "1.14.1"
lazy val lucreVersion               = "3.3.1"
lazy val fscapeJobsVersion          = "1.5.0"
lazy val strugatzkiVersion          = "2.11.0"
lazy val scalaUtilsVersion          = "2.1.7"
lazy val scalaOSCVersion            = "1.1.5"
lazy val playJSONVersion            = "0.4.0"

lazy val bdb = "bdb" // either "bdb" or "bdb6"

// ---- views dependencies ----

lazy val nuagesVersion              = "2.4.0"
lazy val scalaColliderSwingVersion  = "1.27.1"
lazy val lucreSwingVersion          = "1.3.0"
lazy val swingPlusVersion           = "0.2.1"
lazy val spanVersion                = "1.3.1"
lazy val audioWidgetsVersion        = "1.9.4"
lazy val desktopVersion             = "0.7.2"
lazy val sonogramVersion            = "1.9.0"
lazy val raphaelIconsVersion        = "1.0.3"
lazy val pdflitzVersion             = "1.2.1"
lazy val subminVersion              = "0.2.0"

// ----

lazy val main = "de.sciss.mellite.Mellite"

lazy val commonSettings = Seq(
  version            := projectVersion,
  organization       := "de.sciss",
  homepage           := Some(url(s"https://github.com/Sciss/$baseName")),
  licenses           := Seq("GNU General Public License v3+" -> url("http://www.gnu.org/licenses/gpl-3.0.txt")),
  scalaVersion       := "2.11.8",
  crossScalaVersions := Seq("2.11.8", "2.10.6"),
  scalacOptions ++= {
    val xs = Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture")
    if (loggingEnabled || isSnapshot.value) xs else xs ++ Seq("-Xelide-below", "INFO")
  },
  javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
  resolvers += "Typesafe Maven Repository" at "http://repo.typesafe.com/typesafe/maven-releases/", // https://stackoverflow.com/questions/23979577
  // resolvers += "Typesafe Simple Repository" at "http://repo.typesafe.com/typesafe/simple/maven-releases/", // https://stackoverflow.com/questions/20497271
  aggregate in assembly := false,
  // ---- publishing ----
  publishMavenStyle := true,
  publishTo := {
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    )
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := { val n = name.value
  <scm>
    <url>git@github.com:Sciss/{n}.git</url>
    <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
  </scm>
    <developers>
      <developer>
        <id>sciss</id>
        <name>Hanns Holger Rutz</name>
        <url>http://www.sciss.de</url>
      </developer>
    </developers>
  }
)

// ---- projects ----

lazy val root = Project(id = baseNameL, base = file(".")).
  aggregate(core, views).
  dependsOn(core, views).
  settings(commonSettings).
  settings(
    name := baseName,
    description := fullDescr,
    mainClass in (Compile,run) := Some(main),
    publishArtifact in (Compile, packageBin) := false, // there are no binaries
    publishArtifact in (Compile, packageDoc) := false, // there are no javadocs
    publishArtifact in (Compile, packageSrc) := false,  // there are no sources
    // ---- packaging ----
    //    appbundle.icon      := Some(baseDirectory.value / ".." / "icons" / "application.png"),
    //    appbundle.target    := baseDirectory.value / "..",
    //    appbundle.signature := "Ttm ",
    //    appbundle.javaOptions ++= Seq("-Xms2048m", "-Xmx2048m", "-XX:PermSize=256m", "-XX:MaxPermSize=512m"),
    //    appbundle.documents += appbundle.Document(
    //      name       = "Mellite Document",
    //      role       = appbundle.Document.Editor,
    //      icon       = Some(baseDirectory.value / ".." / "icons" / "document.png"),
    //      extensions = Seq("mllt"),
    //      isPackage  = true
    //    ),
    mainClass             in assembly := Some(main),
    target                in assembly := baseDirectory.value,
    assemblyJarName       in assembly := s"$baseName.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("org", "xmlpull", xs @ _*) => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )

lazy val core = Project(id = s"$baseNameL-core", base = file("core")).
  settings(commonSettings).
  settings(
    name        := s"$baseName-core",
    description := "Core layer for Mellite",
    resolvers += "Oracle Repository" at "http://download.oracle.com/maven", // required for sleepycat
    libraryDependencies ++= Seq(
      "de.sciss"        %% "soundprocesses-core"      % soundProcessesVersion,    // computer-music framework
      "de.sciss"        %% "scalainterpreterpane"     % interpreterPaneVersion,   // REPL
      "de.sciss"        %% "scalacolliderugens-api"   % scalaColliderUGenVersion, // need latest version
      "de.sciss"        %  "scalacolliderugens-spec"  % scalaColliderUGenVersion, // meta data
      "de.sciss"        %% "lucre-core"               % lucreVersion,
      "de.sciss"        %% "lucre-confluent"          % lucreVersion,
      "de.sciss"        %% s"lucre-$bdb"              % lucreVersion,             // database backend
      "de.sciss"        %% "fscapejobs"               % fscapeJobsVersion,        // remote FScape invocation
      "de.sciss"        %% "strugatzki"               % strugatzkiVersion,        // feature extraction
      "de.sciss"        %% "span"                     % spanVersion,              // makes sbt happy :-E
      "org.scalautils"  %% "scalautils"               % scalaUtilsVersion,        // type-safe equals
      "de.sciss"        %% "scalaosc"                 % scalaOSCVersion,          // important fixes
      "de.sciss"        %% "play-json-sealed"         % playJSONVersion
    ),
    initialCommands in console := "import de.sciss.mellite._"
  )

lazy val views = Project(id = s"$baseNameL-views", base = file("views")).
  dependsOn(core).
  enablePlugins(BuildInfoPlugin).
  settings(commonSettings).
  settings(
    name        := s"$baseName-views",
    description := fullDescr,
    libraryDependencies ++= Seq(
      "de.sciss" %% "soundprocesses-views"            % soundProcessesVersion,      // computer-music framework
      "de.sciss" %% "soundprocesses-compiler"         % soundProcessesVersion,      // computer-music framework
      "de.sciss" %% "wolkenpumpe"                     % nuagesVersion,              // live improv
      "de.sciss" %% "scalacolliderswing-interpreter"  % scalaColliderSwingVersion,  // REPL
      "de.sciss" %% "lucreswing"                      % lucreSwingVersion,          // reactive Swing components
      "de.sciss" %% "swingplus"                       % swingPlusVersion,           // newest version: color-chooser
      "de.sciss" %% "span"                            % spanVersion,
      "de.sciss" %% "audiowidgets-swing"              % audioWidgetsVersion,        // audio application widgets
      "de.sciss" %% "audiowidgets-app"                % audioWidgetsVersion,        // audio application widgets
      "de.sciss" %% "desktop"                         % desktopVersion,
      "de.sciss" %% "desktop-mac"                     % desktopVersion,             // desktop framework; TODO: should be only added on OS X platforms
      "de.sciss" %% "sonogramoverview"                % sonogramVersion,            // sonogram component
      "de.sciss" %% "raphael-icons"                   % raphaelIconsVersion,        // icon set
      "de.sciss" %% "pdflitz"                         % pdflitzVersion,             // PDF export
      "de.sciss" %  "submin"                          % subminVersion               // dark skin
    ),
    mainClass in (Compile,run) := Some(main),
    initialCommands in console :=
      """import de.sciss.mellite._""".stripMargin,
    fork in run := true,  // required for shutdown hook, and also the scheduled thread pool, it seems
    // ---- build-info ----
    // sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys := Seq("name" -> baseName /* name */, organization, version, scalaVersion, description,
      BuildInfoKey.map(homepage) { case (k, opt) => k -> opt.get },
      BuildInfoKey.map(licenses) { case (_, Seq( (lic, _) )) => "license" -> lic }
    ),
    buildInfoPackage := "de.sciss.mellite"
  )
