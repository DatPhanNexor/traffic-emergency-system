pluginManagement {

    repositories {

        google {

            content {

                includeGroupByRegex("com\\.android.*")

                includeGroupByRegex("com\\.google.*")

                includeGroupByRegex("androidx.*")

            }

        }

        mavenCentral()

        gradlePluginPortal()

    }

}


val localProperties = java.util.Properties()

val localPropertiesFile = rootProject.projectDir.resolve("local.properties")

if (localPropertiesFile.exists()) {

    localProperties.load(localPropertiesFile.inputStream())

}

val mapboxToken: String = localProperties.getProperty("MAPBOX_ACCESS_TOKEN") ?: ""

// --------------------------------------------------



dependencyResolutionManagement {

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {

        google()

        mavenCentral()



        maven { url = uri("https://github.com/jitsi/jitsi-maven-repository/raw/master/releases") }

        maven { url = uri("https://jitpack.io") }



        maven {

            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")

            authentication {

                create<BasicAuthentication>("basic")

            }

            credentials {

                username = "mapbox"

                // Dùng biến đã đọc từ local.properties thay vì dán cứng

                password = mapboxToken

            }

        }

    }

}



rootProject.name = "CanhBao"

include(":app")