import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localConfig = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun configString(name: String): String = "\"" +
    localConfig.getProperty(name, "").replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.production.fridgewise"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.production.fridgewise"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "OCR_BASE_URL", configString("ocr.baseUrl"))
        buildConfigField("String", "API_BASE_URL", configString("api.baseUrl"))
        buildConfigField("String", "SUPABASE_URL", configString("supabase.url"))
        buildConfigField("String", "SUPABASE_ANON_KEY", configString("supabase.anonKey"))
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")
    testImplementation("junit:junit:4.13.2")
    implementation("com.google.code.gson:gson:2.11.0") {
        // Gson's Error Prone annotations are only needed by Java static analysis.
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
