import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
    id("com.avast.gradle.docker-compose") version "0.17.12"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("deliveryhoursservice.DeliveryHoursServiceKt")
}

dependencies {
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-jackson")
    implementation("io.ktor:ktor-client-logging")
    implementation("io.ktor:ktor-client-java")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.+")
    implementation("ch.qos.logback:logback-classic:1.5.17")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-cio:3.1.1")

    testImplementation(platform("org.junit:junit-bom:5.12.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.ktor:ktor-client-content-negotiation")
}

dockerCompose {
    isRequiredBy(tasks.run)
    captureContainersOutput.set(false)
    stopContainers.set(false)
    setProjectName(null)
    listOf("/usr/bin/docker", "/usr/local/bin/docker").firstOrNull {
        File(it).exists()
    }?.let { docker ->
        // works around an issue where the docker command is not found
        // falls back to the default, which may work on some platforms
        dockerExecutable.set(docker)
    }
}

tasks {
    run.invoke {
        doFirst {
            val wiremockPort = dockerCompose.servicesInfos["external-services-mock"]?.port ?: 8080
            environment("VENUE_SERVICE_URL", "http://localhost:$wiremockPort/venue-service")
            environment("COURIER_SERVICE_URL", "http://localhost:$wiremockPort/courier-service")
        }
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.FAILED)
            showExceptions = true
            showStackTraces = true
            showCauses = true
            exceptionFormat = FULL
        }
    }
}
