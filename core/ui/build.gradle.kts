plugins {
    alias(libs.plugins.sudokutrio.android.library)
    alias(libs.plugins.sudokutrio.android.compose)
}

android {
    namespace = "io.github.vinaooo.sudokutrio.core.ui"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":domain"))
}
