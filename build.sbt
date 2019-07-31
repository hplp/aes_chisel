// started with the chisel template from:
//   https://github.com/freechipsproject/chisel-template
//

def scalacOptionsVersion(scalaVersion: String): Seq[String] = {
  Seq() ++ {
    // If we're building with Scala > 2.11, enable the compile option
    //  switch to support our anonymous Bundle definitions:
    //  https://github.com/scala/bug/issues/10047
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, scalaMajor: Long)) if scalaMajor < 12 => Seq()
      case _ => Seq("-Xsource:2.11")
    }
  }
}

def javacOptionsVersion(scalaVersion: String): Seq[String] = {
  Seq() ++ {
    // Scala 2.12 requires Java 8. We continue to generate
    //  Java 7 compatible code for Scala 2.11
    //  for compatibility with old clients.
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, scalaMajor: Long)) if scalaMajor < 12 =>
        Seq("-source", "1.7", "-target", "1.7")
      case _ =>
        Seq("-source", "1.8", "-target", "1.8")
    }
  }
}

name := "aes_chisel"

version := "3.2.0"

scalaVersion := "2.11.12"

crossScalaVersions := Seq("2.11.12", "2.12.4")

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

// Provide a managed dependency on X if -DXVersion="" is supplied on the command line.
val defaultVersions = Map(
  "chisel3" -> "3.1.+",
  "chisel-iotesters" -> "1.2.+"
)

libraryDependencies ++= Seq("chisel3", "chisel-iotesters").map {
  dep: String => "edu.berkeley.cs" %% dep % sys.props.getOrElse(dep + "Version", defaultVersions(dep))
}

scalacOptions ++= scalacOptionsVersion(scalaVersion.value)
//scalacOptions += "-feature"
//scalacOptions += "-deprecation"

javacOptions ++= javacOptionsVersion(scalaVersion.value)

// POM settings for Sonatype
// Sonatype Username: smosanu (same as github username)
// Full name: Sergiu Mosanu
organization := "com.github.hplp"
organizationName := "hplp"
homepage := Some(url("http://hplp.ece.virginia.edu/home"))
scmInfo := Some(ScmInfo(url("https://github.com/hplp/aes_chisel"), "git@github.com:hplp/aes_chisel.git"))
developers := List(Developer("smosanu", "Sergiu Mosanu", "sm7ed@virginia.edu", url("https://github.com/smosanu")))
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

publishMavenStyle := true
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

// Add sonatype repository settings
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

pgpReadOnly := false