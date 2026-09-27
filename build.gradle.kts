plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = property("maven_group") as String
version = property("plugin_version") as String
description = "Spawn a bot of yourself to stay AFK while you disconnect"

val javaVersion = (property("java_version") as String).toInt()
val minecraftVersion = property("minecraft_version") as String

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle(property("paper_dev_bundle") as String)

    compileOnly("org.jetbrains:annotations:26.0.2")
    compileOnly("org.jspecify:jspecify:1.0.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

base {
    archivesName = property("archives_base_name") as String
}

tasks {
    compileJava {
        options.release = javaVersion
        options.encoding = "UTF-8"
    }

    javadoc {
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()
    }

    processResources {
        val props = mapOf(
            "id" to project.property("plugin_id"),
            "slug" to project.property("plugin_slug"),
            "displayName" to project.property("plugin_display_name"),
            "author" to project.property("plugin_author"),
            "originalAuthor" to project.property("plugin_original_author"),
            "version" to project.version,
            "description" to project.description,
            "apiVersion" to minecraftVersion,
        )
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    runServer {
        minecraftVersion(minecraftVersion)
    }
}
