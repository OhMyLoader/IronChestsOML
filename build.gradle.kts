import java.util.Properties

plugins {
    alias(libs.plugins.kotlinJvm)
    id("org.ohmyloader.gradle")
}

version = "0.1.0"

kotlin {
    jvmToolchain(27)
}

val omlVersion = providers.gradleProperty("oml_version").get()
// The game version is gradle.properties' oml_game_version, read as a file: no `-P` can move it.
val gameVersion = Properties().apply {
    file("gradle.properties").inputStream().use { load(it) }
}.getProperty("oml_game_version")?.takeIf { it.isNotBlank() }
    ?: error("oml_game_version is missing from gradle.properties")

oml {
    minecraftVersion.set(gameVersion)
    apiVersion.set(omlVersion)
    // Workaround for an intermittent 0xC0000005 right after window creation on the Zulu 27 line
    // (the same flag the loader's verification mod carries).
    extraJvmArgs.addAll(listOf("-XX:TieredStopAtLevel=1"))
    // -Poml.quickPlay=<world>: boot straight into a save, for probe runs without touching the UI.
    providers.gradleProperty("oml.quickPlay").orNull?.let { world ->
        extraGameArgs.add("--quickPlaySingleplayer=$world")
    }
}
