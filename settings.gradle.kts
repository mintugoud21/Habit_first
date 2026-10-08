pluginManagement {
    repositories {
        maven { url = uri("/root/maven/localMvnRepository") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        maven { url = uri("/root/maven/localMvnRepository") }
        google()
        mavenCentral()
    }
}
rootProject.name = "HabitsFirst"
include(":app")
