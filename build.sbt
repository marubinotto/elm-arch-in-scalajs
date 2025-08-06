ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.2"

lazy val root = (project in file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "elm-arch-in-scalajs",

    // Scala.js configuration
    scalaJSUseMainModuleInitializer := true,

    // Dependencies
    libraryDependencies ++= Seq(
      // Cats Effect for functional programming with IO
      "org.typelevel" %%% "cats-effect" % "3.5.2",

      // QuickLens for immutable data transformations
      "com.softwaremill.quicklens" %%% "quicklens" % "1.9.6",

      // Scala.js DOM bindings
      "org.scala-js" %%% "scalajs-dom" % "2.4.0",

      // Testing dependencies
      "org.scalatest" %%% "scalatest" % "3.2.17" % Test,
      "org.scalatestplus" %%% "scalacheck-1-17" % "3.2.17.0" % Test,
      "org.typelevel" %%% "cats-effect-testing-scalatest" % "1.5.0" % Test
    ),

    // Scala.js linker configuration
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withSourceMap(false)
    }
  )
