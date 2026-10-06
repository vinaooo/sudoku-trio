plugins {
    alias(libs.plugins.sudokutrio.android.library)
    alias(libs.plugins.sudokutrio.android.compose)
    alias(libs.plugins.sudokutrio.hilt)
}

android {
    namespace = "io.github.vinaooo.sudokutrio.core.ads"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
}
