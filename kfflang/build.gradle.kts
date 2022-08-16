import net.minecraftforge.gradle.userdev.tasks.JarJar

val kotlin_version: String by project
val annotations_version: String by project
val coroutines_version: String by project
val serialization_version: String by project
val max_kotlin: String by project
val max_coroutines: String by project
val max_serialization: String by project

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("net.minecraftforge.gradle")
    id("com.modrinth.minotaur") version "2.+"
    `maven-publish`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}

// Enable JarInJar
jarJar.enable()

// Workaround to remove build\java from MOD_CLASSES because SJH doesn't like nonexistent dirs
for (s in arrayOf(sourceSets.main, sourceSets.test)) {
    val sourceSet = s.get()
    val mutClassesDirs = sourceSet.output.classesDirs as ConfigurableFileCollection
    val javaClassDir = sourceSet.java.classesDirectory.get()
    val mutClassesFrom = HashSet(mutClassesDirs.from.filter {
        val provider = it as Provider<*>?
        val toCompare = if (it != null) provider!!.get() else it
        return@filter javaClassDir != toCompare
    })
    mutClassesDirs.setFrom(mutClassesFrom)
}

val library: Configuration by configurations.creating

configurations {
    api {
        extendsFrom(library)
    }
    
    runtimeElements {
        // Remove Minecraft from transitive maven dependencies
        exclude(group = "net.minecraftforge", module = "forge")
        
        // Include obf jar in the final JarJar
        outgoing { 
            artifacts.clear()
            artifact(tasks.jarJar)
        }
    }
}

repositories {
    mavenCentral()
    // For testing with kfflib and making JarJar shut up
    mavenLocal()
}

dependencies {
    minecraft("net.minecraftforge:forge:1.19-41.0.91")

    fun include(group: String, name: String, version: String) {
        library(group = group, name = name, version = version) {
            exclude(group = "org.jetbrains", module = "annotations")
            isTransitive = false
        }
    }

    include("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", kotlin_version)
    include("org.jetbrains.kotlin", "kotlin-reflect", kotlin_version)
    include("org.jetbrains.kotlinx", "kotlinx-coroutines-core", coroutines_version)
    include("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", coroutines_version)
    include("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", coroutines_version)
    include("org.jetbrains.kotlinx", "kotlinx-serialization-json", serialization_version)
    include("org.jetbrains.kotlin", "kotlin-stdlib-jdk7", kotlin_version)
    include("org.jetbrains.kotlinx", "kotlinx-serialization-core", serialization_version)
    include("org.jetbrains.kotlin", "kotlin-stdlib", kotlin_version)
    include("org.jetbrains.kotlin", "kotlin-stdlib-common", kotlin_version)
}

minecraft {
    mappings("official", "1.19")

    runs {
        create("client") {
            workingDirectory(project.file("run"))

            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")

            mods {
                create("kfflang") {
                    source(sourceSets.main.get())
                }

                create("kfflangtest") {
                    source(sourceSets.test.get())
                }
            }
        }

        create("server") {
            workingDirectory(project.file("run/server"))

            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")

            mods {
                create("kfflang") {
                    source(sourceSets.main.get())
                }

                create("kfflangtest") {
                    source(sourceSets.test.get())
                }
            }
        }
        
        all {
            lazyToken("minecraft_classpath") {
                library.copyRecursive().resolve()
                    .joinToString(separator = File.pathSeparator, transform = File::getAbsolutePath)
            }
        }
    }
}

tasks {
    withType<Jar> {
        manifest {
            attributes(
                mapOf(
                    "FMLModType" to "LANGPROVIDER",
                    "Specification-Title" to "Kotlin for Forge",
                    "Automatic-Module-Name" to "kfflang",
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
    
    withType<JarJar> {
        archiveClassifier.set("obf")
    }
    
    // Only require the lang provider to use explicit visibility modifiers, not the test mod
    compileKotlin {
        kotlinOptions.freeCompilerArgs = listOf("-Xexplicit-api=warning", "-Xjvm-default=all")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
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
