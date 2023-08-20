/*
 *   Copyright 2023 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.google.firebase.crashlytics")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("io.realm.kotlin")
    id("kotlin-parcelize")
}

apply {
    from("batch.gradle.kts")
    from("iap.gradle.kts")
    from("atlas.gradle.kts")
}

android {
    namespace = "com.benoitletondor.easybudgetapp"

    defaultConfig {
        applicationId = "com.benoitletondor.easybudgetapp"
        compileSdk = 33
        minSdk = 21
        targetSdk = 33
        versionCode = 93
        versionName = "3.0.0"
        vectorDrawables.useSupportLibrary = true

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.incremental" to "true"
                )
            }
        }
    }

    buildTypes {
        debug {
            val batchDevKey = rootProject.extra["batchDevKey"] as String
            val licenceKey = rootProject.extra["licenceKey"] as String
            val atlasAppId = rootProject.extra["atlasAppId"] as String

            buildConfigField("boolean", "DEBUG_LOG", "true")
            buildConfigField("boolean", "CRASHLYTICS_ACTIVATED", "false")
            buildConfigField("String", "BATCH_API_KEY", "\"$batchDevKey\"")
            buildConfigField("boolean", "ANALYTICS_ACTIVATED", "false")
            buildConfigField("boolean", "DEV_PREFERENCES", "true")
            buildConfigField("String", "LICENCE_KEY", "\"$licenceKey\"")
            buildConfigField("String", "ATLAS_APP_ID", "\"$atlasAppId\"")

            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            val batchLiveKey = rootProject.extra["batchLiveKey"] as String
            val licenceKey = rootProject.extra["licenceKey"] as String
            val atlasAppId = rootProject.extra["atlasAppId"] as String

            buildConfigField("boolean", "DEBUG_LOG", "false")
            buildConfigField("boolean", "CRASHLYTICS_ACTIVATED", "true")
            buildConfigField("String", "BATCH_API_KEY", "\"$batchLiveKey\"")
            buildConfigField("boolean", "ANALYTICS_ACTIVATED", "true")
            buildConfigField("boolean", "DEV_PREFERENCES", "false")
            buildConfigField("String", "LICENCE_KEY", "\"$licenceKey\"")
            buildConfigField("String", "ATLAS_APP_ID", "\"$atlasAppId\"")

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("../key/debug.jks")
            storePassword = "uFdRPMWz69R28t6m9zV53jmw9hJVK3"
            keyAlias = "easybudget"
            keyPassword = "uFdRPMWz69R28t6m9zV53jmw9hJVK3"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    val kotlinVersion: String by rootProject.extra
    val hiltVersion: String by rootProject.extra
    val realmVersion: String by rootProject.extra

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("androidx.work:work-gcm:2.8.1")
    implementation("com.google.android.play:core:1.10.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.2")

    implementation(platform("com.google.firebase:firebase-bom:32.2.2"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")

    val composeBom = platform("androidx.compose:compose-bom:2023.06.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("com.google.accompanist:accompanist-themeadapter-material3:0.31.5-beta")

    implementation("com.android.billingclient:billing-ktx:6.0.1")

    implementation(project(":caldroid"))
    implementation("me.relex:circleindicator:2.1.6@aar")
    implementation("com.batch.android:batch-sdk:1.19.4")

    implementation("com.google.dagger:hilt-android:$hiltVersion")
    implementation("androidx.hilt:hilt-work:1.0.0")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")

    ksp("androidx.room:room-compiler:2.5.2")
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")

    implementation("io.realm.kotlin:library-sync:$realmVersion")

    implementation("net.sf.biweekly:biweekly:0.6.7")

    implementation("net.lingala.zip4j:zip4j:2.11.5")
}
