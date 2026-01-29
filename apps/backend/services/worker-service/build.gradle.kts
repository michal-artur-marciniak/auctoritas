plugins {
  id("org.springframework.boot")
}

val commonsCompressVersion = "1.27.1"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-actuator")

  testImplementation("org.springframework.boot:spring-boot-starter-test")

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
