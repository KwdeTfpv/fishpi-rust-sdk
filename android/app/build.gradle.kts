import org.gradle.internal.os.OperatingSystem
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val versionProps = Properties()
val versionFile = rootProject.projectDir.resolve("android/version.properties")
if (versionFile.exists()) {
    versionFile.inputStream().use { versionProps.load(it) }
}

val versionNameOverride = providers.gradleProperty("fishpiVersionName").orNull
val versionCodeOverride = providers.gradleProperty("fishpiVersionCode").orNull?.toIntOrNull()

val signingProps = Properties()
val signingFile = project.projectDir.resolve("signing.properties")
if (signingFile.exists()) {
    signingFile.inputStream().use { signingProps.load(it) }
}

fun signingValue(propertyName: String, environmentName: String): String? {
    return (signingProps.getProperty(propertyName) ?: System.getenv(environmentName))
        ?.takeIf { it.isNotBlank() }
}

val releaseStoreFile = signingValue("storeFile", "FISHPI_RELEASE_STORE_FILE")?.let { file(it) }
val releaseStorePassword = signingValue("storePassword", "FISHPI_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "FISHPI_RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "FISHPI_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = releaseStoreFile?.exists() == true &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

android {
    namespace = "dev.fishpi.mobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.fishpi.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeOverride
            ?: (versionProps.getProperty("versionCode") ?: "1").toInt()
        versionName = versionNameOverride
            ?: versionProps.getProperty("versionName")
            ?: "0.1.0"
        val updateRepoOwner = (project.findProperty("updateRepoOwner") as String?) ?: "KwdeTfpv"
        val updateRepoName = (project.findProperty("updateRepoName") as String?) ?: "fishpi-rust-sdk"
        buildConfigField("String", "UPDATE_REPO_OWNER", "\"$updateRepoOwner\"")
        buildConfigField("String", "UPDATE_REPO_NAME", "\"$updateRepoName\"")
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "src/main/proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDir(layout.buildDirectory.dir("generated/rustJniLibs"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val hostTag = when {
    OperatingSystem.current().isWindows -> "windows-x86_64"
    OperatingSystem.current().isMacOsX -> "darwin-x86_64"
    else -> "linux-x86_64"
}

val clangName = if (OperatingSystem.current().isWindows) {
    "aarch64-linux-android26-clang.cmd"
} else {
    "aarch64-linux-android26-clang"
}

val androidSdkDir = providers.environmentVariable("ANDROID_HOME")
    .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))
    .orElse("D:\\Android\\Sdk")

fun resolveNdkDir(): File {
    val explicit = providers.environmentVariable("ANDROID_NDK_HOME").orNull
    if (!explicit.isNullOrBlank()) {
        return file(explicit)
    }

    val sdk = file(androidSdkDir.get())
    val ndkRoot = sdk.resolve("ndk")
    return ndkRoot
        .listFiles()
        ?.filter { it.isDirectory }
        ?.sortedByDescending { it.name }
        ?.firstOrNull()
        ?: error("Android NDK not found under ${ndkRoot.absolutePath}")
}

val cargoBuildArm64 = tasks.register<Exec>("cargoBuildArm64") {
    val ndkDir = resolveNdkDir()
    val clangBin = ndkDir.resolve("toolchains/llvm/prebuilt/$hostTag/bin")
    val linker = clangBin.resolve(clangName)
    workingDir = rootProject.projectDir.resolve("android/native")
    environment("ANDROID_NDK_HOME", ndkDir.absolutePath)
    environment("PATH", "${clangBin.absolutePath}${File.pathSeparator}${System.getenv("PATH")}")
    environment("CC_aarch64_linux_android", linker.absolutePath)
    environment("CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER", linker.absolutePath)
    environment(
        "CARGO_TARGET_AARCH64_LINUX_ANDROID_RUSTFLAGS",
        "-C link-arg=-Wl,-z,max-page-size=16384 -C link-arg=-Wl,-z,common-page-size=16384",
    )
    commandLine("cargo", "build", "--release", "--target", "aarch64-linux-android")
}

val copyRustArm64 = tasks.register<Copy>("copyRustArm64") {
    dependsOn(cargoBuildArm64)
    from(rootProject.layout.projectDirectory.file("android/native/target/aarch64-linux-android/release/libfishpi_sdk.so"))
    into(layout.buildDirectory.dir("generated/rustJniLibs/arm64-v8a"))
}

val checkUiContract = tasks.register("checkUiContract") {
    val featureRoot = layout.projectDirectory.dir("src/main/java/dev/fishpi/mobile/feature").asFile
    inputs.dir(featureRoot)
    doLast {
        val forbiddenImports = listOf(
            "dev.fishpi.mobile.data.",
            "dev.fishpi.mobile.plugin.",
            "android.content.SharedPreferences",
        )
        val violations = featureRoot.walkTopDown()
            .filter { file ->
                file.isFile &&
                    file.extension == "kt" &&
                    file.path.contains("${File.separator}ui${File.separator}")
            }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) ->
                        val trimmed = line.trimStart()
                        trimmed.startsWith("import ") && forbiddenImports.any { trimmed.contains(it) }
                    }
                    .map { (index, line) ->
                        "${file.relativeTo(featureRoot).path}:${index + 1}: ${line.trim()}"
                    }
            }
            .toList()
        if (violations.isNotEmpty()) {
            throw org.gradle.api.GradleException(
                "feature/**/ui must not import data/plugin/SharedPreferences:\n" +
                    violations.joinToString("\n"),
            )
        }
    }
}

tasks.named("preBuild") {
    dependsOn(copyRustArm64, checkUiContract)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.blurview)
    implementation(libs.kyant.backdrop)
    implementation(libs.kyant.shapes)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.svg)
    implementation(libs.okhttp)
    implementation(libs.markwon.core)
    implementation(libs.markwon.html)
    implementation(libs.markwon.linkify)
    implementation(libs.markwon.ext.tables)
    implementation(libs.emoji.java)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    // CameraX + ML Kit for full-screen QR scan with zoom
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    testImplementation(libs.junit)
}
