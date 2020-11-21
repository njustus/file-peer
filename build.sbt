fork in run := true
connectInput in run := true
cancelable in Global := true

resolvers += Resolver.sonatypeRepo("releases")

lazy val root = (project in file("."))
  //.enablePlugins(BuildInfoPlugin, JavaAppPackaging)
  .settings(Settings.settings)
  .settings(
    version := "0.0.1",
    name := "file-peer",
    mainClass in Compile := Some("filepeer.core.FilePeerMain"),
    libraryDependencies ++= Dependencies.deps
  )
