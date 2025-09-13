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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        // Add any enabled features here if needed
    }

    lint {
        targetSdk = 34
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

dependencies {
    // ONNX Runtime for Android
    implementation(libs.onnxruntime.android)

    // Doc file handling
    implementation(libs.androidx.documentfile)

    // Expose core-ktx to consumers of core or extensions
    api(libs.androidx.core.ktx)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.dev-diaries41"
            artifactId = "smartscan-${project.name}"
            version = project.findProperty("publishVersion")?.toString() ?: "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
