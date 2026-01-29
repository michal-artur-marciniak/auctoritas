plugins {
  id("org.springframework.boot")
}

val commonsCompressVersion = "1.26.0"

dependencies {
  implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
  implementation("org.springframework.boot:spring-boot-starter-actuator")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.projectreactor:reactor-test")

  constraints {
    implementation("org.apache.commons:commons-compress:$commonsCompressVersion")
  }
}

tasks.named("jar") {
  enabled = false
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
  archiveFileName.set("app.jar")
}
