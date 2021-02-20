name := "api-durakov-scala"

version := "0.1"

scalaVersion := "2.13.4"
kotlinVersion := "1.3.70"
libraryDependencies ++= {
  val springBootVersion = "2.2.6.RELEASE"
  val graphqlVersion = "11.0.0"

  Seq(
    "io.reactivex.rxjava2" % "rxjava" % "2.2.20",
    "org.springframework.boot" % "spring-boot-starter-test" % springBootVersion,
    "com.graphql-java-kickstart" % "graphql-spring-boot-starter" % graphqlVersion,
    "com.graphql-java-kickstart" % "playground-spring-boot-starter" % graphqlVersion,
//    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.1"
  )
}
enablePlugins(JavaAppPackaging, AshScriptPlugin)
// set the main entrypoint to the application that is used in startup scripts
mainClass in Compile := Some("dev.durak.Application")
herokuAppName in Compile := "api-durakov-scala"
herokuJdkVersion in Compile := "8"