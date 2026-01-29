pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

rootProject.name = "auctoritas-backend"

include("services:auth-service", "services:gateway-service", "services:worker-service")
