plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":shared"))
    testImplementation(kotlin("test-junit"))
}

application {
    mainClass.set("com.franklinharper.dicewarsport.trainingcli.MainKt")
}

tasks.test {
    useJUnit()
}

kotlin {
    jvmToolchain(17)
}
