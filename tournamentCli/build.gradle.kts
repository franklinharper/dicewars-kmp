import java.security.MessageDigest
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":shared"))
    testImplementation(kotlin("test-junit"))
}

application {
    mainClass.set("com.franklinharper.dicewarsport.tournamentcli.MainKt")
}

tasks.test {
    useJUnit()
}

kotlin {
    jvmToolchain(17)
}

val tournamentFatJar by tasks.registering(Jar::class) {
    dependsOn(tasks.named("classes"))
    archiveFileName.set("dicewars-tournament-cli-all.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.franklinharper.dicewarsport.tournamentcli.MainKt"
    }
    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get().map { dependency ->
            if (dependency.isDirectory) dependency else zipTree(dependency)
        }
    })
}

tasks.register("generateTournamentDist") {
    notCompatibleWithConfigurationCache("Computes source hashes and writes the checked-in tournament CLI dist directory")
    dependsOn(tournamentFatJar)
    doLast {
        val distDir = rootProject.layout.projectDirectory.dir("dist/tournament-cli").asFile.also { it.mkdirs() }
        val jar = tournamentFatJar.get().archiveFile.get().asFile
        jar.copyTo(File(distDir, "dicewars-tournament-cli-all.jar"), overwrite = true)
        File(distDir, "source.hash").writeText(computeTournamentSourceHash(rootProject.projectDir))
        println("Tournament CLI dist updated: ${distDir.absolutePath}")
    }
}

fun computeTournamentSourceHash(rootDir: File): String {
    val inputPaths = listOf(
        "shared/src/commonMain/kotlin",
        "shared/src/jvmMain/kotlin",
        "tournamentCli/src/main/kotlin",
        "build.gradle.kts",
        "settings.gradle.kts",
        "shared/build.gradle.kts",
        "tournamentCli/build.gradle.kts",
        "gradle/libs.versions.toml",
    )
    val hashInput = buildString {
        for (path in inputPaths) {
            val file = File(rootDir, path)
            if (!file.exists()) continue
            val files = if (file.isDirectory) {
                file.walkTopDown().filter { it.isFile }.sortedBy { it.relativeTo(rootDir).invariantSeparatorsPath }.toList()
            } else {
                listOf(file)
            }
            for (source in files) {
                append(source.sha256())
                append("  ")
                append(source.relativeTo(rootDir).invariantSeparatorsPath)
                append('\n')
            }
        }
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(hashInput.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}

fun File.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(readBytes())
    .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
