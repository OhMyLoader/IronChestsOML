// An independent build that reaches OhMyLoader exactly the way a third-party mod project does:
// the plugin and every `org.ohmyloader:*` coordinate come from Maven, no composite substitution.
// Development loop: `gradlew publishToMavenLocal` in OhMyLoader/ (and in OhMyLoaderGradle/ when
// the plugin itself changed), then this project resolves everything from mavenLocal.

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.ohmyloader.gradle") {
                useVersion(providers.gradleProperty("oml_version").get())
            }
        }
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://maven.minecraftforge.net/")
        maven("https://libraries.minecraft.net/")
    }
}

rootProject.name = "ironchests-oml"
