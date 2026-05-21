plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Read sources as UTF-8 regardless of the build machine's default charset, so
// non-ASCII string literals (e.g. emoji in seeded demo posts) compile correctly.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
