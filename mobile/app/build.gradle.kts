plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.presenceprotocol.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.presenceprotocol.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "role"
    productFlavors {
        create("client") {
            dimension = "role"
            buildConfigField("String", "BLE_ROLE", "\"CLIENT_ONLY\"")
            applicationIdSuffix = ".client"
            versionNameSuffix = "-client"
        }
        create("server") {
            dimension = "role"
            buildConfigField("String", "BLE_ROLE", "\"SERVER_ONLY\"")
            applicationIdSuffix = ".server"
            versionNameSuffix = "-server"
        }
        create("both") {
            dimension = "role"
            buildConfigField("String", "BLE_ROLE", "\"BOTH\"")
            applicationIdSuffix = ".both"
            versionNameSuffix = "-both"
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources.excludes += "META-INF/*"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":feature-relay"))
    implementation(project(":core-common"))
    implementation(project(":core-crypto"))
    implementation(project(":core-storage"))
    implementation(project(":domain"))
    implementation(project(":data-ble"))
    implementation(project(":data-storage"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coroutines.android)

    implementation(libs.mlkit.barcode)
    implementation("com.google.android.material:material:1.11.0")
}