import org.gradle.api.JavaVersion.VERSION_11

plugins {
    application
}

group = "org.twitchstats"
version = ""

description = "A Twitch Stat bot for BMJ"

java {
    sourceCompatibility = VERSION_11
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.javacord:javacord:3.7.0")
    implementation("org.apache.logging.log4j:log4j-api:2.17.2")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.0.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.0")
    implementation("com.github.twitch4j:twitch4j:1.20.0")

    runtimeOnly("org.apache.logging.log4j:log4j-core:2.19.0")
}

application {
    mainClass.set("org.twitchstats.Main")
}
val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}"
    manifest {
        attributes["Implementation-Title"] = "Gradle Jar File"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "org.twitchstats.Main"
    }
    from(configurations.runtimeClasspath.get().map({ if (it.isDirectory) it else zipTree(it) }))
    with(tasks.jar.get() as CopySpec)
}

tasks.withType<org.gradle.jvm.tasks.Jar>() {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("META-INF/BC1024KE.RSA", "META-INF/BC1024KE.SF", "META-INF/BC1024KE.DSA")
    exclude("META-INF/BC2048KE.RSA", "META-INF/BC2048KE.SF", "META-INF/BC2048KE.DSA")
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
