pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://repo.spring.io/plugins-release/")
//        maven(url = "https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1")
//        maven {
//            name = "vsts-maven-adal-android"
//            url = uri("https://identitydivision.pkgs.visualstudio.com/_packaging/AndroidADAL/maven/v1")
//            credentials {
//                val vstsUsername = System.getenv("ENV_VSTS_MVN_ANDROIDADAL_USERNAME")
//                val vstsMavenAccessToken = System.getenv("ENV_VSTS_MVN_ANDROIDADAL_ACCESSTOKEN")
//
//                username = vstsUsername
//                password = vstsMavenAccessToken
//            }
//        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "UniversalAppNotifier"
include(":app")
 