plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val githubClientId = providers.gradleProperty("ABK_GITHUB_CLIENT_ID")
    .orElse(providers.environmentVariable("ABK_GITHUB_CLIENT_ID"))
    .orElse("Ov23li8skGo6AFPBeSTh")

val releaseStoreFile = providers.gradleProperty("ABK_RELEASE_STORE_FILE")
    .orElse(providers.environmentVariable("ABK_RELEASE_STORE_FILE"))
val releaseStorePassword = providers.gradleProperty("ABK_RELEASE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("ABK_RELEASE_STORE_PASSWORD"))
val releaseStoreType = providers.gradleProperty("ABK_RELEASE_STORE_TYPE")
    .orElse(providers.environmentVariable("ABK_RELEASE_STORE_TYPE"))
    .orElse("JKS")
val releaseKeyAlias = providers.gradleProperty("ABK_RELEASE_KEY_ALIAS")
    .orElse(providers.environmentVariable("ABK_RELEASE_KEY_ALIAS"))
val releaseKeyPassword = providers.gradleProperty("ABK_RELEASE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("ABK_RELEASE_KEY_PASSWORD"))
val hasReleaseSigning = !releaseStoreFile.orNull.isNullOrBlank() &&
    !releaseStorePassword.orNull.isNullOrBlank() &&
    !releaseKeyAlias.orNull.isNullOrBlank() &&
    !releaseKeyPassword.orNull.isNullOrBlank()

android {
    namespace = "com.abk.kernel"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.abk.kernel"
        minSdk = 26
        targetSdk = 35
        versionCode = 10020
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GITHUB_CLIENT_ID", "\"${githubClientId.get()}\"")
        buildConfigField("String", "SOURCE_REPO_OWNER", "\"xingguangcuican6666\"")
        buildConfigField("String", "SOURCE_REPO_NAME", "\"ABK\"")
        buildConfigField("String", "UPSTREAM_REPO_URL", "\"https://github.com/zzh20188/GKI_KernelSU_SUSFS\"")
        buildConfigField("String", "TOP_LEVEL_REPO_URL", "\"https://github.com/WildKernels/GKI_KernelSU_SUSFS\"")

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }

    ndkVersion = "28.2.13676358"

    signingConfigs {
        create("release") {
            storeFile = releaseStoreFile.orNull?.let { file(it) }
            storePassword = releaseStorePassword.orNull
            storeType = releaseStoreType.get()
            keyAlias = releaseKeyAlias.orNull
            keyPassword = releaseKeyPassword.orNull
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("main") {
            assets.srcDir("build/generated/abk-assets/main")
            jniLibs.srcDir("build/generated/abk-jniLibs/main")
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.google.material)
    implementation(libs.androidx.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)

    // Root
    implementation(libs.libsu.core)
    implementation(libs.libsu.io)

    // Image
    implementation(libs.coil.compose)

    // Background work & notifications
    implementation(libs.work.runtime.ktx)

    // Preferences
    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Windows: Gradle merges PATH + JDK bin into -Djava.library.path without quoting; spaces and stray
// quotes in PATH break the test worker ("Could not find or load main class ;C:\Users\...").
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        },
    )
    environment("CLASSPATH", "")
    environment("_JAVA_OPTIONS", "")
    environment("JAVA_TOOL_OPTIONS", "")
    environment("JDK_JAVA_OPTIONS", "")

    val jniLibDirs = listOf("src/testDebug/jniLibs", "src/test/jniLibs", ".")
        .map { layout.projectDirectory.dir(it).asFile }
        .filter { it.isDirectory }
    val libraryPath = jniLibDirs.joinToString(File.pathSeparator) { it.absolutePath }

    // AGP/Gradle append PATH and java.home\bin to java.library.path; a quoted or spaced PATH breaks argv on Windows.
    val systemRoot = System.getenv("SystemRoot") ?: "C:\\Windows"
    environment("PATH", "$systemRoot\\system32")

    doFirst {
        systemProperty("java.library.path", libraryPath)
        jvmArgs("-Djava.library.path=$libraryPath")
    }
}
