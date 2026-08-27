# English Drill Helper

A desktop helper for English drills, built with JavaFX.

This is a starter project with one screen. It follows the MVVM
structure of [sss-music-player](https://github.com/sosuisen/sss-music-player)
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

## Project Layout

```
com.sosuisha/
├── domain/exception/     # UnrecoverableException (base of unchecked app errors)
├── presentation/
│   ├── View.java         # Common interface of all screens
│   ├── WindowManager.java# Registers views and shows them in windows
│   └── screens/
│       ├── alert/        # AlertDialog (common error dialogs)
│       └── drill/        # DrillView + DrillViewModel (the first screen)
└── main/                 # App (composition root) and Launcher (main method)
```
