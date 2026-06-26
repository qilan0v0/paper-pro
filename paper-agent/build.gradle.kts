plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

description = "QLTZ Agent - 系统监控探针（Java 实现，环境变量配置）"

val oshiVersion = "6.6.5"
val jacksonVersion = "2.17.2"

dependencies {
    implementation("com.github.oshi:oshi-core:$oshiVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Main-Class" to "com.qltz.agent.Main",
            "Implementation-Title" to "QLTZ Agent",
            "Implementation-Version" to "0.1.0"
        )
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("qltz-agent")
    archiveClassifier.set("")
    archiveVersion.set("0.1.0")
    mergeServiceFiles()
    minimize {
        exclude(dependency("com.github.oshi:oshi-core"))
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
}
