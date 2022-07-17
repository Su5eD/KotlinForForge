val kotlin_version: String by project
val annotations_version: String by project
val coroutines_version: String by project
val serialization_version: String by project
val max_kotlin: String by project
val max_coroutines: String by project
val max_serialization: String by project

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    id("eclipse")
    id("net.minecraftforge.gradle") version "5.1.+"
    id("com.modrinth.minotaur") version "2.+"
}


java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
kotlin.jvmToolchain {}

// Current KFF version
val kffVersion = "3.7.0"
val kffGroup = "thedarkcolour"

// Enable JarInJar
jarJar.enable()

allprojects {
    version = kffVersion
    group = kffGroup
}

configurations {
    val library = maybeCreate("library")
    api.configure {
        extendsFrom(library)
    }
}

minecraft.runs.all {
    lazyToken("minecraft_classpath") {
        return@lazyToken configurations["library"].copyRecursive().resolve()
            .joinToString(File.pathSeparator) { it.absolutePath }
    }
}

repositories {
    mavenLocal()
}

dependencies {
    minecraft("net.minecraftforge:forge:1.19-41.0.91")

    val library = configurations["library"]

    fun includeKotlin(dependencyNotation: String, preferredVersion: String) {
        library(dependencyNotation) {
            exclude(group = "org.jetbrains", module = "annotations")
            jarJar(this) {
                exclude(group = "org.jetbrains", module = "annotations")
                jarJar.pin(this, preferredVersion)
            }
        }
    }

    constraints {
        library("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
        library("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version")
    }

    includeKotlin("org.jetbrains.kotlin:kotlin-stdlib-jdk8:[$kotlin_version, $max_kotlin)", kotlin_version)
    // includeKotlin("org.jetbrains.kotlin:kotlin-reflect:[$kotlin_version, $max_kotlin)", kotlin_version)
    // includeKotlin("org.jetbrains.kotlinx:kotlinx-coroutines-core:[$coroutines_version, $max_coroutines)", coroutines_version)
    // includeKotlin("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:[$coroutines_version, $max_coroutines)", coroutines_version)
    includeKotlin("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:[$coroutines_version, $max_coroutines)", coroutines_version)
    // includeKotlin("org.jetbrains.kotlinx:kotlinx-serialization-json:[$serialization_version, $max_serialization)", serialization_version)

    implementation(group = kffGroup, name = "kotlinforforge", version = "[${project.version}, 4.0)") {
        isTransitive = false
        jarJar(this) {
            jarJar.pin(this, kffVersion)
        }
    }

    implementation(group = kffGroup, name = "kfflib", version = "[${project.version}, 4.0)") {
        isTransitive = false
        jarJar(this) {
            jarJar.pin(this, kffVersion)
        }
    }
}

minecraft.run {
    mappings("official", "1.19")

    runs {
        create("client") {
            workingDirectory(project.file("run"))

            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")

            mods {
                create("kotlinforforge") {
                    source(sourceSets.main.get())
                }
            }
        }

        create("server") {
            workingDirectory(project.file("run/server"))

            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")

            mods {
                create("kotlinforforge") {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

sourceSets.main.configure {
    resources {
        srcDir("src/generated/resources")
    }
}

tasks.withType<Jar> {

    manifest {
        attributes(
            mapOf(
                "Specification-Title" to "Kotlin for Forge",
                "Automatic-Module-Name" to "kotlinforforge",
                "Specification-Vendor" to "Forge",
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to "${project.version}",
                "Implementation-Vendor" to "thedarkcolour",
                "Implementation-Timestamp" to `java.text`.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                    .format(`java.util`.Date())
            )
        )
    }
}

modrinth {
    projectId.set("ordsPcFz")
    versionNumber.set("${project.version}")
    versionType.set("release")
    uploadFile.set(tasks.jarJar as Any)
    gameVersions.addAll("1.18", "1.18.1", "1.19")
    loaders.add("forge")
}

fun DependencyHandler.minecraft(
    dependencyNotation: Any
): Dependency? = add("minecraft", dependencyNotation)