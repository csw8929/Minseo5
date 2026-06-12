plugins {
    alias(libs.plugins.android.application)
}

val appVersion = rootProject.file("VERSION").readText().trim()
val versionParts = appVersion.split(".")
val computedVersionCode =
    versionParts[0].toInt() * 1000 +
    versionParts[1].toInt() * 100 +
    versionParts[2].toInt() * 10 +
    versionParts[3].toInt()

android {
    namespace = "com.example.minseo5"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.minseo5"
        minSdk = 24
        targetSdk = 36
        versionCode = computedVersionCode
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "Minseo5.apk"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.room.runtime)
    implementation(libs.gson)
    annotationProcessor(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
