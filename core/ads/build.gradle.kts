plugins {
    alias(libs.plugins.sudokutrio.android.library)
    alias(libs.plugins.sudokutrio.android.compose)
    alias(libs.plugins.sudokutrio.hilt)
}

android {
    namespace = "io.github.vinaooo.sudokutrio.core.ads"
}

// The placeholder banner only; AdMob and the consent SDK join at release prep.
dependencies {
    implementation(project(":core:designsystem"))
}
