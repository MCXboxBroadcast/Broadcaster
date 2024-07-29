plugins {
	id("com.rtm516.mcxboxbroadcast.shadow-conventions")
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency)
}

dependencies {
	implementation(libs.bundles.spring.runtime) {
		exclude("org.springframework.boot", "spring-boot-starter-json")
	}

	testImplementation(libs.bundles.spring.test)
	testRuntimeOnly(libs.junit.platform.launcher)

	api(project(":core"))
	api(libs.bedrock.common)
	api(libs.guava)
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks {
	bootJar {
		archiveFileName.set("MCXboxBroadcastManager.${archiveExtension.get()}")
	}
}

description = "bootstrap-manager"