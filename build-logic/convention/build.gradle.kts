plugins {
    `kotlin-dsl`
}

group = "io.github.vinaooo.sudokutrio.buildlogic"

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
    implementation(libs.kover.gradle.plugin)

    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.junit5.jupiter)
    testImplementation(libs.kotest.assertions.core)
    testRuntimeOnly(libs.junit5.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "sudokutrio.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "sudokutrio.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "sudokutrio.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "sudokutrio.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("hilt") {
            id = "sudokutrio.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = "sudokutrio.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("rootCoverage") {
            id = "sudokutrio.root.coverage"
            implementationClass = "RootCoverageConventionPlugin"
        }
        register("quality") {
            id = "sudokutrio.quality"
            implementationClass = "QualityConventionPlugin"
        }
    }
}
