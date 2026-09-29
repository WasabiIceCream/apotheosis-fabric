pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        gradlePluginPortal()
    }
}

rootProject.name = "apotheosis-fabric"

// Placebo is Apotheosis's required base library, ported as its own separate mod
// project to match the real upstream dependency architecture — see
// mod-dev/apotheosis-fabric/README.md.
includeBuild("../placebo-fabric")

// Apothic Attributes (our Fabric port), required by 15 gems and 24 affixes.
includeBuild("../apothic-attributes-fabric")
