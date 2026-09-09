plugins {
    id("fabric-loom") version "1.17.20"
    `java-library`
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

repositories {
    maven("https://api.modrinth.com/maven") { name = "Modrinth" }
    maven("https://maven.ladysnake.org/releases") { name = "Ladysnake" }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    // Provides javax.annotation.Nullable etc. — NeoForge bundles this transitively,
    // Fabric doesn't, and upstream Apotheosis source uses it throughout (same reason
    // placebo-fabric needs it — see that project's build.gradle.kts).
    include("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    // Placebo — Apotheosis's required base library (included as a composite build
    // from ../placebo-fabric, see settings.gradle.kts).
    implementation("dev.shadowsoffire.placebo:placebo-fabric")

    // Data attachment storage for affix/gem state on items and entities (replaces
    // NeoForge's DataAttachments system — see mod-dev/apotheosis-fabric/README.md).
    // Already installed separately on the target server as the bundled
    // cardinal-components-api-8.0.1.jar (which jar-in-jars these submodules, invisible
    // to Gradle's compile classpath) — depend on the individual submodules directly from
    // Ladysnake's own maven for compiling against, not bundled here (not include()d).
    val cardinalComponentsVersion = "8.0.1"
    implementation("org.ladysnake.cardinal-components-api:cardinal-components-base:$cardinalComponentsVersion")
    implementation("org.ladysnake.cardinal-components-api:cardinal-components-entity:$cardinalComponentsVersion")
}

loom {
    accessWidenerPath.set(file("src/main/resources/apotheosis.accesswidener"))
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}
