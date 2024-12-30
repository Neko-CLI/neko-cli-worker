@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    application
    id("com.github.johnrengelman.shadow") version "6.0.0"
    kotlin("plugin.serialization") version "1.8.21"
}

application.mainClassName = "MainKt"
group = "dev.unstackss"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    implementation("net.dv8tion:JDA:5.2.1") // JDA
    implementation("org.slf4j:slf4j-api:2.1.0-alpha1") // SLF4J
    testImplementation("org.slf4j:slf4j-simple:2.1.0-alpha1") // SLF4J
    runtimeOnly("org.slf4j:slf4j-simple:2.1.0-alpha1") // SLF4J
    runtimeOnly("ch.qos.logback:logback-classic:1.4.12") // LOGBAC
    implementation("org.fusesource.jansi:jansi:2.4.1") // JANSI
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // KOTLIN COROUTINE
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") // KOTLIN SERIALIZER
    implementation("org.json:json:20240303") // JSON
    implementation("org.jsoup:jsoup:1.18.3") // JSOUP
    implementation("org.apache.xmlgraphics:batik-all:1.18") // APACHE-BATIK-FULL
    implementation("org.mongodb:mongodb-driver-sync:4.10.2") // MONGODB-DRIVER-SY
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isIncremental = true
    sourceCompatibility = "18"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "18"
}

application {
    mainClass.set("MainKt")
}
