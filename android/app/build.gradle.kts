import org.gradle.internal.os.OperatingSystem
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val versionProps = Properties()
val versionFile = rootProject.projectDir.resolve("android/version.properties")
if (versionFile.exists()) {
    versionFile.inputStream().use { versionProps.load(it) }
}

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
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.fishpi.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = (versionProps.getProperty("versionCode") ?: "1").toInt()
        versionName = versionProps.getProperty("versionName") ?: "0.1.0"
        val updateRepoOwner = (project.findProperty("updateRepoOwner") as String?) ?: "KwdeTfpv"
        val updateRepoName = (project.findProperty("updateRepoName") as String?) ?: "fishpi-rust-sdk"
        buildConfigField("String", "UPDATE_REPO_OWNER", "\"$updateRepoOwner\"")
        buildConfigField("String", "UPDATE_REPO_NAME", "\"$updateRepoName\"")
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
            isMinifyEnabled = false
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
    commandLine("cargo", "build", "--release", "--target", "aarch64-linux-android")
}

val copyRustArm64 = tasks.register<Copy>("copyRustArm64") {
    dependsOn(cargoBuildArm64)
    from(rootProject.layout.projectDirectory.file("android/native/target/aarch64-linux-android/release/libfishpi_sdk.so"))
    into(layout.buildDirectory.dir("generated/rustJniLibs/arm64-v8a"))
}

tasks.named("preBuild") {
    dependsOn(copyRustArm64)
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-gif:3.4.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")
    implementation("io.coil-kt.coil3:coil-svg:3.4.0")
    val markwonVersion = "4.6.2"
    implementation("io.noties.markwon:core:$markwonVersion")
    implementation("io.noties.markwon:html:$markwonVersion")
    implementation("io.noties.markwon:linkify:$markwonVersion")
    implementation("io.noties.markwon:ext-tables:$markwonVersion")
    implementation("com.vdurmont:emoji-java:5.1.1")
    val media3Version = "1.10.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    // CameraX + ML Kit for full-screen QR scan with zoom
    val cameraxVersion = "1.4.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    testImplementation("junit:junit:4.13.2")
}
