name := "api-durakov-scala"

version := "0.1"

scalaVersion := "2.13.4"

libraryDependencies ++= Seq(
  "org.springframework.boot" % "spring-boot-starter-web" % "2.4.1"
)

mainClass in Compile := Some("dev.durak.Application")