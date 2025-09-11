plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.fpf.smartscansdk.extensions"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    lint {
        targetSdk = 34
    }
}

dependencies {
    implementation(project(":core"))
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.dev-diaries41"
            artifactId = project.name
            // Use a default version for local testing
            version = project.findProperty("publishVersion")?.toString() ?: "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}