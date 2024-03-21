plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

val msalVersion: String = "4.+"

android {
    namespace = "com.example.universalappnotifier"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.universalappnotifier"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    //for google auth and calendar integrations
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.23.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev305-1.23.0")

    //to avoid conflicts in libraries
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")

    implementation("com.google.api-client:google-api-client-android:1.23.0") {
        exclude(group = ("org.apache.httpcomponents"))
    }

    //so that we can easily control permissions
    implementation("pub.devrel:easypermissions:3.0.0")

    //volley
    implementation("com.android.volley:volley:1.2.1")

//    if (findProject(":msal") != null) {
//        // For developer team only.
//        dependencies {
//            "localImplementation"(project(":msal"))
//            "externalImplementation"("com.microsoft.identity.client:msal:${msalVersion}")
//        }
//    } else {
//        // Downloads and Builds MSAL from maven central.
//        implementation("com.microsoft.identity.client:msal:${msalVersion}")
//    }
//
//    implementation("io.opentelemetry:opentelemetry-bom:1.18.0")

//    implementation("com.microsoft.services:outlook-services:2.0.0")
}