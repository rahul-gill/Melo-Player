import meloplayer.buildlogic.VersionProperties

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlin.android)
}


android {
    compileSdk = VersionProperties.compileSdk
    namespace = "meloplayer.appthemehelper"

    defaultConfig {
        minSdk = VersionProperties.minSdk
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
//            signingConfig = releaseSigning
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
}

dependencies {
    implementation( libs.androidx.appcompat)
    implementation( libs.android.material)
    implementation( libs.androidx.preference.ktx)
    coreLibraryDesugaring(libs.desugar.libs)

}
