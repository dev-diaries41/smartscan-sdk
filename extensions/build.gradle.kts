plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("com.google.devtools.ksp")

}

android {
    namespace = "com.fpf.smartscansdk.extensions"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources.excludes.addAll(
            listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
            )
        )
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

    lint {
        targetSdk = 34
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
            }
        }
    }

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

dependencies {
    // Pull in core transitively so consumers only need extensions
    api(project(":core"))

    // Add any extension-specific dependencies here
    // Tests
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("io.mockk:mockk:1.14.5")
    testImplementation(kotlin("test"))

    // Android instrumented tests
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.3.0")
    androidTestImplementation("androidx.test:runner:1.6.1")
    androidTestImplementation("io.mockk:mockk-android:1.14.5")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // room tests
    androidTestImplementation("androidx.room:room-runtime:2.7.2")
    androidTestImplementation("androidx.room:room-ktx:2.7.2")
    androidTestImplementation("androidx.room:room-testing:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2") // only for annotation processing

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
