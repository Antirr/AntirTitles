plugins {
    id("java")
}

group = "me.antir"
version = "0.1"

val hytaleHome: String by project
val hytalePatchline: String by project
val hytaleServerJar = "$hytaleHome/install/$hytalePatchline/package/game/latest/Server/HytaleServer.jar"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(files(hytaleServerJar))
}

tasks.test {
    useJUnitPlatform()
}

/**
 * Produces a slim API JAR that other mods can add as a compileOnly dependency.
 * The JAR contains only the public-facing interfaces, events, and utilities —
 * nothing that requires the full mod to be on the compile classpath.
 *
 * Usage in a consuming mod's build.gradle.kts:
 *   compileOnly(files("libs/antirtitles-0.1-api.jar"))
 */
val apiJar by tasks.registering(Jar::class) {
    archiveClassifier.set("api")
    dependsOn(tasks.named("compileJava"))
    from(sourceSets.main.get().output) {
        include(
            // Public API interfaces and utilities
            "me/antir/api/**",
            // Core data interfaces
            "me/antir/data/ITitle.class",
            // Effect system interfaces
            "me/antir/data/effect/ITitleEffect.class",
            "me/antir/data/effect/TitleEffectContext.class",
            // Requirement interface
            "me/antir/data/requirement/ITitleRequirement.class",
            // Events (including inner TitleGrantEventType / TitleEquipEventType)
            "me/antir/events/TitleGrantEvent.class",
            "me/antir/events/TitleGrantEvent\$*.class",
            "me/antir/events/TitleEquipEvent.class",
            "me/antir/events/TitleEquipEvent\$*.class",
            // Requirement types needed by consuming mods to call addTierXp / onActivity / onItemObtained
            "me/antir/data/requirement/TierProgressRequirement.class",
            "me/antir/data/requirement/ObtainItemRequirement.class",
            "me/antir/data/requirement/ActivityCountRequirement.class"
        )
    }
}