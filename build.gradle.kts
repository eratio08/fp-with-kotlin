import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jvmVersion = "14"

plugins {
    kotlin("jvm") version "1.4.10"
    idea
    id("application")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

repositories {
    mavenCentral()
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = jvmVersion
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = jvmVersion
}