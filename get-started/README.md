# Get Started with zvec-java

A minimal example showing how to use the zvec-java library in your project.

## Requirements

- **Java 25+** — zvec-java uses the Foreign Function & Memory (FFM) API
- Add `--enable-native-access=ALL-UNNAMED` to your JVM arguments

## Gradle setup

Add the Seqera Maven repository and the `lib-zvec` dependency to your `build.gradle`:

```groovy
repositories {
    maven { url = 'https://s3-eu-west-1.amazonaws.com/maven.seqera.io/snapshots' }
}

dependencies {
    implementation 'io.seqera:lib-zvec:0.1.0-SNAPSHOT'
}
```

## Run

```bash
./gradlew :get-started:run
```

## What it does

The example initializes zvec, creates a collection with a string field and a 4-dimensional vector field using cosine similarity, inserts two documents, and queries for the most similar documents to a given vector.
