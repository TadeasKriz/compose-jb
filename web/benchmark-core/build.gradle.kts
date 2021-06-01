plugins {
    id("kotlin-multiplatform")
    id("org.jetbrains.compose")
}


kotlin {
    js(IR) {
        browser() {
            testTask {
                testLogging.showStandardStreams = true
                useKarma {
                    useChromeHeadless()
                    //useFirefox()
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(project(":web-core"))
                implementation(kotlin("stdlib-common"))
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

val printBenchmarkResults by tasks.registering {
    doLast {
        val report = buildDir.resolve("reports/tests/jsTest/classes/BenchmarkTests.html").readText()
        val stdout = "#.*;".toRegex().findAll(report).map { it.value }.firstOrNull()

        val benchmarks = stdout?.split(";")?.mapNotNull {
            if (it.isEmpty()) {
                null
            } else {
                val b = it.split(":")
                val testName = b[0].replace("#", "")
                val benchmarkMs = b[1].toInt()

                testName to benchmarkMs
            }
        }?.toMap()

        println("##teamcity[testSuiteStarted name='BenchmarkTests']")
        benchmarks?.forEach {
            // TeamCity messages need to escape '[' and ']' using '|'
            val testName = it.key
                .replace("[", "|[")
                .replace("]", "|]")
            println("##teamcity[testStarted name='$testName']")
            println("##teamcity[testMetadata name='benchmark avg' type='number' value='${it.value}']")
            println("##teamcity[testFinished name='$testName']")
        }
        println("##teamcity[testSuiteFinished name='BenchmarkTests']")
    }
}

tasks.named("jsTest") { finalizedBy(printBenchmarkResults) }