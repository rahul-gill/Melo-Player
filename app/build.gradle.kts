import meloplayer.build.VersionProperties
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.navigationSafeargs)
    alias(libs.plugins.ksp)
}

android {
    namespace = "meloplayer.app"
    compileSdk = VersionProperties.compileSdk

    defaultConfig {
        minSdk = VersionProperties.minSdk
        targetSdk = VersionProperties.compileSdk
        vectorDrawables.useSupportLibrary = true
        applicationId = "meloplayer.app"
        versionCode = VersionProperties.versionCode
        versionName = VersionProperties.versionName
    }
    val releaseSigning = run {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        if(!keystorePropertiesFile.exists()){
            return@run null
        }
        val keystoreProperties = Properties()
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
        signingConfigs.create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if(releaseSigning != null){
                signingConfig = releaseSigning
            } else if(System.getenv().containsKey("SIGN_RELEASE_WITH_DEBUG")){
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    flavorDimensions.add("version")
    productFlavors {
        create("playstore") {
            dimension  = "version"
        }
        create("fdroid") {
            dimension = "version"
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

}

val playstoreImplementation by configurations
dependencies {
    implementation( project(":appthemehelper"))
    implementation( libs.gridLayout)

    coreLibraryDesugaring(libs.desugar.libs)

    implementation( libs.androidx.appcompat)
    implementation( libs.androidx.annotation)
    implementation( libs.androidx.constraintLayout)
    implementation( libs.androidx.recyclerview)
    implementation( libs.androidx.preference.ktx)
    implementation( libs.androidx.core.ktx)
    implementation( libs.androidx.palette.ktx)

    implementation( libs.androidx.mediarouter)
    //Cast Dependencies
    playstoreImplementation( libs.google.play.services.cast.framework)
    playstoreImplementation( libs.nanohttpd)

    implementation( libs.androidx.navigation.runtime.ktx)
    implementation( libs.androidx.navigation.fragment.ktx)
    implementation( libs.androidx.navigation.ui.ktx)


    implementation( libs.timber)

    implementation( libs.androidx.room.runtime)
    implementation( libs.androidx.room.ktx)
    ksp (libs.androidx.room.compiler)

    implementation( libs.androidx.lifecycle.viewmodel.ktx)
    implementation( libs.androidx.lifecycle.livedata.ktx)
    implementation( libs.androidx.lifecycle.common.java8)

    implementation( libs.androidx.core.splashscreen)

    playstoreImplementation( libs.google.feature.delivery)
    playstoreImplementation( libs.google.play.review)

    implementation( libs.android.material)

    implementation( libs.retrofit)
    implementation( libs.retrofit.converter.gson)
    implementation( libs.okhttp3.logging.interceptor)

    implementation( libs.afollestad.material.dialogs.core)
    implementation( libs.afollestad.material.dialogs.input)
    implementation( libs.afollestad.material.dialogs.color)
    implementation( libs.afollestad.material.cab)

    implementation( libs.kotlinx.coroutines.android)

    implementation( libs.koin.core)
    implementation( libs.koin.android)

    implementation( libs.glide)
    ksp (libs.glide.ksp)
    implementation( libs.glide.okhttp3.integration)

    implementation( libs.advrecyclerview)

    implementation( libs.fadingedgelayout)

    implementation( libs.keyboardvisibilityevent)
    implementation( libs.jetradarmobile.android.snowfall)

    implementation( libs.chrisbanes.insetter)

    implementation( libs.org.eclipse.egit.github.core)
    implementation( libs.jaudiotagger)
    playstoreImplementation( libs.android.lab.library)
    implementation( libs.slidableactivity)
    implementation( libs.material.intro)
    implementation( libs.dhaval2404.imagepicker)
    implementation( libs.fastscroll.library)
    implementation( libs.customactivityoncrash)
    implementation( libs.tankery.circularSeekBar)
}