plugins {
    id("splitsignal.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        val jvmAndAndroidMain by getting
        val wasmJsMain by getting

        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmAndAndroidMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.websockets)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

android {
    namespace = "com.splitsignal.core.network"
}
