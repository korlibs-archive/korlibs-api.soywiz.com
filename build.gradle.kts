import org.gradle.kotlin.dsl.*

plugins {
	kotlin("jvm") version "1.3.70"
	application
}
group = "com.soywiz.korlibs-api"
version = "1.0-SNAPSHOT"

repositories {
	mavenLocal()
	mavenCentral()
	jcenter()
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	//implementation("mysql:mysql-connector-java:8.0.19")
	implementation("org.freemarker:freemarker:2.3.29")
	//implementation('io.vertx:vertx-web:3.8.5')
	//implementation('io.vertx:vertx-lang-kotlin:3.8.5')
	//implementation('io.vertx:vertx-lang-kotlin-coroutines:3.8.5')
	implementation("com.hubspot.slack:slack-client:1.6")
	implementation("io.ktor:ktor-server-netty:1.3.0")
	implementation("io.ktor:ktor-client-core-jvm:1.3.0")
	implementation("io.ktor:ktor-client-okhttp:1.3.0")
	implementation("io.ktor:ktor-client-jackson:1.3.0")
	//implementation('com.soywiz.korlibs.korio:korio-jvm:1.9.8')
	implementation("com.soywiz.korlibs.klock:klock-jvm:1.8.7")
	implementation("com.soywiz.kminiorm:kminiorm:0.5.0")
	implementation("com.soywiz.kminiorm:kminiorm-jdbc:0.5.0")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.2")
	implementation("org.yaml:snakeyaml:1.25")
	implementation("com.h2database:h2:1.4.199")
	testImplementation("junit:junit:4.13")
	testImplementation("org.jetbrains.kotlin:kotlin-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

	implementation("ch.qos.logback:logback-classic:1.2.3")

}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
	kotlinOptions {
		jvmTarget = "1.8"
	}
}

val baseMainClassName = "com.soywiz.korlibs.api.MainKt"

application {
	mainClassName = baseMainClassName
}

tasks {
	val fatJar by creating(Jar::class) {
		manifest {
			attributes(mapOf("Main-Class" to baseMainClassName))
		}
		archiveBaseName.set("app")
		archiveVersion.set("")
		from(configurations.runtimeClasspath.get().files.map { if (it.isDirectory) it else zipTree(it) })
		with(jar.get())
	}

	//val run by creating(JavaExec::class) {}

	val jarFile = fatJar.outputs.files.first()
	val server = "soywiz"
	val baseDir = "/home/virtual/korlibs/korlibs-api.soywiz.com"
	val baseOut = "$server:$baseDir"

	val publishDockerCompose by creating {
		doLast {
			exec { commandLine("scp", file("docker-compose.yml"), "$baseOut/docker-compose.yml") }
		}
	}

	val publishFatJar by creating {
		dependsOn(fatJar)
		doLast {
			exec { commandLine("scp", jarFile, "$baseOut/app/") }
		}
	}

	val restartDockerCompose by creating {
		dependsOn(fatJar)
		doLast {
			exec { commandLine("ssh", server, "/bin/bash", "-c", "'cd $baseDir; docker-compose restart'") }
		}
	}

	val publish by creating {
		dependsOn(publishDockerCompose, publishFatJar)
		finalizedBy(restartDockerCompose)
	}
}
