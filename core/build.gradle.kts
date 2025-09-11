plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.fpf.smartscansdk.core"
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

    buildFeatures {
    }

    // Optional: set targetSdk for lint or tests only
    lint {
        targetSdk = 34
    }
}

dependencies {
    // ONNX Runtime for Android
    implementation(libs.onnxruntime.android)

    // Doc file handling
    implementation(libs.androidx.documentfile)

    // Optional core-ktx if your library uses Android utilities
    api(libs.androidx.core.ktx)
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
