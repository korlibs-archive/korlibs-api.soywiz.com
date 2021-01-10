import org.gradle.kotlin.dsl.*

plugins {
	kotlin("jvm") version "1.4.21"
	application
}
group = "com.soywiz.korlibs-api"
version = "1.0-SNAPSHOT"

repositories {
	mavenLocal()
	mavenCentral()
	jcenter()
	maven {
		url = uri("https://oss.sonatype.org/content/repositories/snapshots")
	}
}

dependencies {
	//val ktor_version = "1.5.0"
	val ktor_version = "1.4.1"

	//implementation("com.gitlab.kordlib.kord:kord-core:0.6.10")
	implementation("com.soywiz.korlibs.korinject:korinject:2.0.2")
	implementation("com.soywiz.korlibs.korio:korio-jvm:2.0.2")
	implementation("dev.kord:kord-core:0.7.0-SNAPSHOT")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	//implementation("mysql:mysql-connector-java:8.0.19")
	implementation("org.freemarker:freemarker:2.3.29")
	//implementation('io.vertx:vertx-web:3.8.5')
	//implementation('io.vertx:vertx-lang-kotlin:3.8.5')
	//implementation('io.vertx:vertx-lang-kotlin-coroutines:3.8.5')
	implementation("com.hubspot.slack:slack-client:1.6")
	implementation("io.ktor:ktor-server-netty:$ktor_version")
	implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
	implementation("io.ktor:ktor-client-okhttp:$ktor_version")
	implementation("io.ktor:ktor-client-cio:$ktor_version")
	implementation("io.ktor:ktor-client-jackson:$ktor_version")
	implementation("io.ktor:ktor-client-websockets:$ktor_version")
	implementation("com.soywiz.korlibs.klock:klock-jvm:1.8.7")
	implementation("com.soywiz.kminiorm:kminiorm:0.8.3")
	implementation("com.soywiz.kminiorm:kminiorm-jdbc:0.8.3")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.2")
	implementation("org.yaml:snakeyaml:1.25")
	implementation("com.h2database:h2:1.4.199")
	testImplementation("junit:junit:4.13")
	testImplementation("org.jetbrains.kotlin:kotlin-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
	testImplementation("io.mockk:mockk:1.10.4")

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
	val server = "soywiz2"
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
