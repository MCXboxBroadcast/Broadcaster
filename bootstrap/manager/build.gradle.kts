plugins {
	id("com.rtm516.mcxboxbroadcast.shadow-conventions")
	id("org.springframework.boot") version "3.3.0"
	id("io.spring.dependency-management") version "1.1.5"
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	api(project(":core"))
	api(libs.bedrock.common)
}

tasks.withType<Test> {
	useJUnitPlatform()
}

nameJar("MCXboxBroadcastManager")

description = "bootstrap-manager"