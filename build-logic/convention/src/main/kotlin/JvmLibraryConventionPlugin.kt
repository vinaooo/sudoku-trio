import io.github.vinaooo.sudokutrio.buildlogic.JAVA_VERSION
import io.github.vinaooo.sudokutrio.buildlogic.configureJUnitPlatform
import io.github.vinaooo.sudokutrio.buildlogic.configureKotlinCompiler
import io.github.vinaooo.sudokutrio.buildlogic.libs
import io.github.vinaooo.sudokutrio.buildlogic.library
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("sudokutrio.quality")
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JAVA_VERSION
            targetCompatibility = JAVA_VERSION
        }
        configureKotlinCompiler()
        configureJUnitPlatform()
        dependencies {
            "testImplementation"(platform(libs.library("junit5-bom")))
            "testImplementation"(libs.library("junit5-jupiter"))
            "testImplementation"(libs.library("kotest-assertions-core"))
            "testImplementation"(libs.library("kotest-property"))
            "testImplementation"(libs.library("kotlinx-coroutines-test"))
            "testRuntimeOnly"(libs.library("junit5-platform-launcher"))
        }
    }
}
