plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

android {
    namespace = "de.dh.pump.omnipod.protocol"
    compileSdk = libs.versions.sdkCompile.get().toInt()

    defaultConfig {
        minSdk = libs.versions.sdkMin.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    }
}

dependencies {
    api(project(":common"))
    api(project(":pump-common"))

    implementation(libs.bouncycastle)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}