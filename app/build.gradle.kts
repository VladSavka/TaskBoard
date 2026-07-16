plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.taskboard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.taskboard"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Enables java.time (LocalDate) on the minSdk 24 runtime via desugaring.
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Debug-only headless acceptance harness (src/debug/.../Harness.kt) installs a
    // Main dispatcher via Dispatchers.setMain. compileOnly keeps coroutines-test's
    // TestMainDispatcherFactory OUT of the on-device debug APK (so it can't hijack
    // Dispatchers.Main); the harness gets it at runtime from the JVM test classpath.
    debugCompileOnly(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Headless acceptance harness support: dump the debug unit-test runtime classpath
// (Gradle resolves AAR classes.jars, coroutines-core, coroutines-test, lifecycle,
// etc. for plain-JVM execution) so acceptance/bin/product-harness can run the real
// TaskListViewModel off-device without hand-hunting jars.
tasks.register("dumpHarnessClasspath") {
    val outFile = layout.buildDirectory.file("harness/classpath.txt")
    outputs.file(outFile)
    doLast {
        // Resolve the runtime graph as extracted classes jars: the artifactView
        // triggers AGP's AAR->classes.jar transform so android libraries land on
        // the classpath as plain jars a bare `java -cp` can load (raw .aar cannot).
        // bin/product-harness appends the platform android.jar (stubs) so
        // SavedStateHandle can load headlessly.
        val artifactType = Attribute.of("artifactType", String::class.java)
        val runtime = configurations.getByName("debugUnitTestRuntimeClasspath")
            .incoming.artifactView {
                attributes.attribute(artifactType, "android-classes-jar")
            }.files
        val cp = runtime.joinToString(System.getProperty("path.separator")) { it.absolutePath }
        val f = outFile.get().asFile
        f.parentFile.mkdirs()
        f.writeText(cp)
    }
}