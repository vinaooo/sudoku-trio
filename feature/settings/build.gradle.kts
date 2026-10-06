plugins {
    alias(libs.plugins.sudokutrio.android.feature)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.vinaooo.sudokutrio.feature.settings"
}

dependencies {
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
