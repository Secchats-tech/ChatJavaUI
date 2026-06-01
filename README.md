# ChatJavaUI

This is a Java Swing desktop client built as a reference implementation for the SecureChat protocol.

## Requirements

- **Java 21+** (Gradle's toolchain will fetch a matching JDK if your system one is older).

## Build

The two runtime jars (`shared_client.jar` and `sqlite-jdbc-3.47.0.0.jar`) live in `libs/`
and are picked up by Gradle as flat-file dependencies.

```sh
./gradlew build
```

## Run

```sh
./gradlew run
```

The `application` plugin uses `com.secchat.ui.JClientMain` as the entry point.

## Package a distribution

```sh
./gradlew installDist
```

Produces `build/install/ChatJavaUI/` with:

- `bin/ChatJavaUI` (and `.bat`) — launcher scripts.
- `lib/` — `ChatJavaUI.jar` plus the bundled deps from `libs/`.
