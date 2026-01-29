import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.plugins.JavaPluginExtension

plugins {
  id("org.springframework.boot") version "4.0.1" apply false
  id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "dev.auctoritas"
version = "0.4.1"

val springCloudVersion = "2025.0.1"
val testcontainersVersion = "1.20.4"
val logstashLogbackVersion = "8.0"
val logbackVersion = "1.5.25"

subprojects {
  apply(plugin = "java")
  apply(plugin = "io.spring.dependency-management")

  group = rootProject.group
  version = rootProject.version

  extensions.configure<JavaPluginExtension> {
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(21))
    }
  }

  repositories {
    mavenCentral()
  }

  extensions.configure<DependencyManagementExtension> {
    imports {
      mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
      mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
    dependencies {
      dependency("net.logstash.logback:logstash-logback-encoder:$logstashLogbackVersion")
      dependency("ch.qos.logback:logback-core:$logbackVersion")
      dependency("ch.qos.logback:logback-classic:$logbackVersion")
    }
  }

  dependencies {
    add("compileOnly", "org.projectlombok:lombok")
    add("annotationProcessor", "org.projectlombok:lombok")
    add("testCompileOnly", "org.projectlombok:lombok")
    add("testAnnotationProcessor", "org.projectlombok:lombok")
  }

  tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    useJUnitPlatform()
  }
}
