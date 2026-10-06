plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "de.dh.pump.common"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    api(project(":common"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.annotation)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}