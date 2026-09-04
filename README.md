# English Drill Helper

A desktop helper for English drills, built with JavaFX.

It follows the MVVM structure of
[sss-music-player](https://github.com/sosuisen/sss-music-player)
and builds the scene graph in code with the
[JavaFX Builder API](https://github.com/sosuisen/javafx-builder-api).

## Requirements

- Java 25
- Maven

JavaFX libraries are downloaded by Maven, so no extra install is needed.

## How to Run

```bash
mvn javafx:run
```

## How to Test

```bash
mvn test
```

The tests run in headless mode, so no window is opened.

## How to Format

```bash
mvn formatter:format
```

## How to Package

```bash
mvn package
```

This creates a native application image with jpackage under `target/jpackage/`.
