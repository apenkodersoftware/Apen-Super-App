import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
  alias(libs.plugins.ktlint)
  alias(libs.plugins.detekt)
}

// Release signing: keystore.properties (git-ignored) for local builds, KEYSTORE_PATH / STORE_PASSWORD /
// KEY_PASSWORD / KEY_ALIAS env vars for CI. The keystore itself never lives in the repo.
val keystoreProps =
  Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
  }

fun releaseValue(
  propKey: String,
  envKey: String,
): String? = keystoreProps.getProperty(propKey) ?: System.getenv(envKey)

val releaseKeyAvailable = releaseValue("storeFile", "KEYSTORE_PATH")?.let { file(it).exists() } == true
// A temporary isolated installation on emulator-5554 exercises fresh setup and recovery without
// replacing the owner's current records or PIN. This flag is never used for the delivered APK.
val apenVerificationBuild = providers.gradleProperty("apenVerification").orNull == "true"

android {
  namespace = "com.aistudio.fasmovapos.app"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = if (apenVerificationBuild) "com.apenkoder.apen.review.r3verification" else "com.apenkoder.apen.review"
    minSdk = 24
    targetSdk = 36
    versionCode = 3
    versionName = "0.3.0-super-review-r3"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    if (releaseKeyAvailable) {
      create("release") {
        storeFile = file(releaseValue("storeFile", "KEYSTORE_PATH")!!)
        storePassword = releaseValue("storePassword", "STORE_PASSWORD")
        keyAlias = releaseValue("keyAlias", "KEY_ALIAS") ?: "fasmova"
        keyPassword = releaseValue("keyPassword", "KEY_PASSWORD")
      }
    }
    create("debugConfig") {
      storeFile = file("$rootDir/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Signed with the real release key (keystore.properties locally, env vars in CI). Without it the
      // build falls back to the public debug key and says so; such an APK must never go to a client.
      signingConfig =
        if (releaseKeyAvailable) {
          signingConfigs.getByName("release")
        } else {
          logger.warn("WARNING: no release keystore configured - release build is signed with the DEBUG key.")
          signingConfigs.getByName("debugConfig")
        }
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  // Migration tests (Robolectric) read the exported Room schemas as assets. Debug only: release stays clean.
  sourceSets.getByName("debug").assets.srcDir("$projectDir/schemas")
  if (apenVerificationBuild) sourceSets.getByName("debug").manifest.srcFile("src/verification/AndroidManifest.xml")
}

// There are no Firebase runtime integrations. The alternate test package needs no generated
// Google client identifiers; ordinary app builds keep their existing configuration task.
if (apenVerificationBuild) tasks.matching { it.name.endsWith("GoogleServices") }.configureEach { enabled = false }

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

configure<KtlintExtension> {
  version.set("1.5.0")
  android.set(true)
  ignoreFailures.set(false)
  outputToConsole.set(true)
}

configure<DetektExtension> {
  config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
  buildUponDefaultConfig = true
  baseline = file("$rootDir/config/detekt/detekt-baseline.xml")
}

tasks.register("codeQualityCheck") {
  group = "verification"
  description = "Runs ktlintCheck and detekt together (CI code-quality gate)."
  dependsOn("ktlintCheck", "detekt")
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai) // unused: removing it also drops the INTERNET permission (the app is offline-only)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.konsist)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.leakcanary.android)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
  detektPlugins(libs.compose.rules.detekt)
}

// Room writes each database version's schema here so migrations can be tested against real history.
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
