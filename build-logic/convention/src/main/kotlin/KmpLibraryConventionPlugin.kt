import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    @OptIn(ExperimentalWasmDsl::class, ExperimentalKotlinGradlePluginApi::class)
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.library")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                applyDefaultHierarchyTemplate {
                    common {
                        group("jvmAndAndroid") {
                            withAndroidTarget()
                            withJvm()
                        }
                    }
                }

                androidTarget {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }

                jvm("desktop") {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }

                wasmJs {
                    browser()
                }
            }

            extensions.configure<LibraryExtension> {
                compileSdk = 35
                defaultConfig {
                    minSdk = 24
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
        }
    }
}
