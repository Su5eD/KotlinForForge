plugins {
    id("org.jetbrains.kotlin.jvm")
    id("net.minecraftforge.gradle")
    `maven-publish`
}

val kotlin_version: String by project
val annotations_version: String by project
val coroutines_version: String by project
val serialization_version: String by project

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
}

val library: Configuration by configurations.creating

configurations {
    api {
        extendsFrom(library)
    }

    // Remove Minecraft from transitive maven dependencies
    runtimeElements {
        exclude(group = "net.minecraftforge", module = "forge")
    }

    all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("thedarkcolour:kfflang")).using(project(":kfflang")).because("Include from local instead of maven")
        }
    }
}

repositories {
    mavenCentral()

    mavenLocal()
}

dependencies {
    minecraft("net.minecraftforge:forge:1.19-41.0.91")

    val excludeAnnotations: ExternalModuleDependency.() -> Unit = {
        exclude(group = "org.jetbrains", module = "annotations")
    }

    library("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version", excludeAnnotations)
    library("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version", excludeAnnotations)
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version", excludeAnnotations)
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutines_version", excludeAnnotations)
    library("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version", excludeAnnotations)
    library("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version", excludeAnnotations)

    implementation(group = "thedarkcolour", name = "kfflang", version = "[${project.version}, 4.0)")
}

minecraft {
    mappings("official", "1.19")

    runs {
        create("client") {
            workingDirectory(project.file("run"))

            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")

            mods {
                create("kfflib") {
                    source(sourceSets.main.get())
                }
            }

            mods {
                create("kfflibtest") {
                    source(sourceSets.test.get())
                }
            }
        }


        create("server") {
            workingDirectory(project.file("run/server"))

            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")

            mods {
                create("kfflib") {
                    source(sourceSets.main.get())
                }
            }

            mods {
                create("kfflibtest") {
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
    // Only require the lang provider to use explicit visibility modifiers, not the test mod
    compileKotlin {
        kotlinOptions.freeCompilerArgs = listOf("-Xexplicit-api=warning", "-Xjvm-default=all")
    }

    withType<Jar> {
        manifest {
            attributes(
                mapOf(
                    "FMLModType" to "GAMELIBRARY",
                    "Specification-Title" to "kfflib",
                    "Automatic-Module-Name" to "kfflib",
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
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
