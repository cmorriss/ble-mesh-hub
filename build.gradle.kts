import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

plugins {
    kotlin("jvm") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.0"
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.weliem.blessed-bluez:blessed:0.61")
    implementation("software.amazon.awssdk.iotdevicesdk:aws-iot-device-sdk:1.11.0")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.9.1"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:okhttp-tls")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.4.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("org.apache.logging.log4j:log4j-api:2.19.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")
    implementation("com.squareup.moshi:moshi:1.14.0")

    runtimeOnly("com.github.hypfvieh:bluez-dbus:0.1.4")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("io.mockk:mockk:1.13.3")
    testImplementation("io.mockk:mockk-dsl:1.13.3")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xopt-in=kotlin.ExperimentalUnsignedTypes")
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    mergeServiceFiles()
    manifest {
        attributes("Manifest-Version" to "1.0", "Main-Class" to "io.morrissey.iot.ble.MeshHubKt")
    }
}

tasks.assemble {
    dependsOn(shadowJar)
}
