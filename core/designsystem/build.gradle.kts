plugins {
    alias(libs.plugins.sudokutrio.android.library)
    alias(libs.plugins.sudokutrio.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.vinaooo.sudokutrio.core.designsystem"
}

dependencies {
    implementation(project(":domain"))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
