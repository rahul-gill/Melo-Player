package meloplayer.buildlogic

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object VersionProperties {
    const val compileSdk = 34
    const val minSdk = 24

    const val versionMajor = 1/*1<=v<=9*/
    const val versionMinor = 0/*0<=v<=99*/
    const val versionPatch = 0/*0<=v<=99*/
    const val versionBuild = 0/*0<=v<=99*/
    const val versionCode =
        versionMajor * 1000000 + versionMinor * 10000 + versionPatch * 100 + versionBuild
    const val versionName = "${versionMajor}.${versionMinor}.${versionPatch}"

}


val gitCommitSha: String
    get() = "git rev-parse --short HEAD".runCommand() ?: "~~~~"
val buildTime
    get() = Instant.now().toString()


fun String.runCommand(workingDir: File = File("./")): String?
    = ProcessBuilder(*split(" ").toTypedArray())
    .directory(workingDir)
    .start()
    .inputStream.readAllBytes()?.let { String(it) }
