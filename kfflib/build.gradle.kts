import org.jetbrains.kotlin.utils.addToStdlib.cast

val kotlin_version: String by project
val annotations_version: String by project
val coroutines_version: String by project
val serialization_version: String by project

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("net.minecraftforge.gradle")
    `maven-publish`
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
kotlin.jvmToolchain {}

val kotlinSourceJar by tasks.creating(Jar::class) {
    val kotlinSourceSet = kotlin.sourceSets.main.get()

    from(kotlinSourceSet.kotlin.srcDirs)
    archiveClassifier.set("sources")
}

tasks.build.get().dependsOn(kotlinSourceJar)

repositories {
    mavenCentral()

    mavenLocal()
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

dependencies {
    minecraft("net.minecraftforge:forge:1.19-41.0.91")

    val library = configurations["library"]

    library("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    library("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutines_version")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version")
    library("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")

    implementation(group = "thedarkcolour", name = "kotlinforforge", version = "[${project.version}, 4.0)")
}

minecraft.run {
    mappings("official", "1.19")

    runs {
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
        }
    }
}

// Only require the lang provider to use explicit visibility modifiers, not the test mod
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().getByName("compileKotlin") {
    kotlinOptions.freeCompilerArgs = listOf("-Xexplicit-api=warning", "-Xjvm-default=all")
}

tasks.withType<Jar> {
    archiveBaseName.set("kfflib")

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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            artifact(kotlinSourceJar)

            // Remove Minecraft from transitive dependencies
            pom.withXml {
                asNode().get("dependencies").cast<groovy.util.NodeList>().first().cast<groovy.util.Node>().children().cast<MutableList<groovy.util.Node>>().removeAll { child ->
                    child.get("groupId").cast<groovy.util.NodeList>().first().cast<groovy.util.Node>().value() == "net.minecraftforge"
                }
            }
        }
    }
}
