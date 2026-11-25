import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.splitsignal.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "splitsignal.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "splitsignal.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
    }
}
