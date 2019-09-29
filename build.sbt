name := "zio-circuit-breaker"

version := "0.1"

scalaVersion := "2.12.10"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.1")
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
scalacOptions += "-Ypartial-unification"
libraryDependencies += "dev.zio" %% "zio" % "1.0.0-RC12-1"