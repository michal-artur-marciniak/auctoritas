plugins {
  id("org.springframework.boot")
}

val bouncyCastleVersion = "1.79"
val jjwtVersion = "0.12.6"
val commonsCompressVersion = "1.27.1"
val commonsCodecVersion = "1.17.1"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")
  implementation("org.springframework.security:spring-security-crypto")
  implementation("org.bouncycastle:bcpkix-jdk18on:$bouncyCastleVersion")
  implementation("commons-codec:commons-codec:$commonsCodecVersion")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-amqp")
  implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
  runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
  runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("net.logstash.logback:logstash-logback-encoder:9.0")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("org.testcontainers:junit-jupiter")
  testImplementation("org.testcontainers:postgresql")
  testImplementation("com.h2database:h2")
  testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")

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
