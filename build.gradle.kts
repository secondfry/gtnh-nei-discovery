
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

version = "0.1.2"

tasks.jar {
    from("LICENSE") { into("META-INF") }
}
