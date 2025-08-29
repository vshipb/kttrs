# Gemini Project Context: kttrs

## Project Overview

This is an Android application named "kttrs" built with Kotlin and Jetpack Compose. It follows a standard Android project structure with a single application module. The user interface is built using modern Android development practices with Material Design 3.

**Key Technologies:**

*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose
*   **Build System:** Gradle
*   **Package Name:** `com.example.kttrs`
*   **Main Entry Point:** `MainActivity`

## Building and Running

This project uses the Gradle wrapper for building and running.

**Build the project:**

```bash
./gradlew build
```

**Run the application on a connected device or emulator:**

```bash
./gradlew installDebug
./gradlew run
```

**Run tests:**

```bash
./gradlew test
```

## Development Conventions

*   **UI:** The project uses Jetpack Compose for building the user interface. Follow Material Design 3 guidelines for UI components and styling.
*   **Dependencies:** Dependencies are managed using the `libs.versions.toml` file. Use this file to update or add new dependencies.
*   **Testing:** The project is set up for both unit tests (in `src/test`) and instrumented tests (in `src/androidTest`). Write tests for new features and bug fixes.
*   **Comments:** Add code comments sparingly. Focus on *why* something is done, especially for complex logic, rather than *what* is done. Only add high-value comments if necessary for clarity or if requested by the user. Do not edit comments that are separate from the code you are changing. *NEVER* talk to the user or describe your changes through comments.
*   **Committing:** Do not commit changes without explicit instruction from the user.
*   **Process:** Do not start implementing changes until all details have been discussed and agreed upon.