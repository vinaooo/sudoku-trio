import com.android.build.api.dsl.LibraryExtension
import io.github.vinaooo.sudokutrio.buildlogic.configureJUnitPlatform
import io.github.vinaooo.sudokutrio.buildlogic.configureKotlinAndroid
import io.github.vinaooo.sudokutrio.buildlogic.libs
import io.github.vinaooo.sudokutrio.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("sudokutrio.quality")
        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            testOptions.targetSdk = libs.version("targetSdk").toInt()
            lint.targetSdk = libs.version("targetSdk").toInt()
        }
        configureJUnitPlatform()
    }
}
