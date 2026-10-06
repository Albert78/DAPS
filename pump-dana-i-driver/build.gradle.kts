plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "de.dh.pump.danai"
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
    api(project(":pump-common"))

    implementation(project(":pump-dana-protocol"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.annotation)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}