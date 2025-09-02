# kttrs - A Tetris-like game for Android

A classic block-stacking game built with modern Android development tools. This project is a demonstration of building a game using Kotlin and Jetpack Compose.

## Features

*   **Classic Gameplay:** Enjoy the timeless gameplay of Tetris.
*   **7-Bag Randomizer:** A fair piece generation system that ensures you get all 7 unique pieces in every bag.
*   **Scoring System:** Keep track of your score and cleared lines.
*   **High Score:** Your top score is saved for you to beat.
*   **Ghost Piece:** A helpful indicator to show where your current piece will land.
*   **Hold Piece:** Swap out your current piece for a stored one.
*   **Multiple Control Schemes:** Choose between on-screen buttons, swipe gestures, or both.
*   **T-Spin Detection:** Advanced move detection for extra points.

## Screenshots

*(Add screenshots or GIFs of the game in action here)*

| Game Screen | Game Over | Settings |
| :---: | :---: | :---: |
| *Screenshot 1* | *Screenshot 2* | *Screenshot 3* |

## Technologies Used

*   **Language:** [Kotlin](https://kotlinlang.org/)
*   **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **Build Tool:** [Gradle](https://gradle.org/)
*   **Architecture:** MVVM (Model-View-ViewModel)

## Getting Started

To build and run the project, you'll need Android Studio or just a Java Development Kit (JDK) and the Android SDK.

### Building

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/kttrs.git
    cd kttrs
    ```

2.  **Build the project using the Gradle wrapper:**
    *   On macOS/Linux:
        ```bash
        ./gradlew build
        ```
    *   On Windows:
        ```bash
        gradlew.bat build
        ```

### Running

You can run the application on a connected Android device or an emulator.

*   On macOS/Linux:
    ```bash
    ./gradlew installDebug
    ./gradlew run
    ```
*   On Windows:
    ```bash
    gradlew.bat installDebug
    gradlew.bat run
    ```

### Running Tests

To run the unit tests:

*   On macOS/Linux:
    ```bash
    ./gradlew test
    ```
*   On Windows:
    ```bash
    gradlew.bat test
    ```

## Project Structure

The project is a standard Android application with a single module (`app`).

*   `app/src/main/java/vsh/kttrs/`: Main source code.
    *   `data/`: Data storage (e.g., `SettingsDataStore` for high scores and preferences).
    *   `model/`: Data classes and game logic (`GameViewModel`, `Piece`, `SevenBagRandomizer`).
    *   `ui/`: Jetpack Compose UI components and screens.
        *   `game/`: Composables related to the game screen itself (`GameBoard`, `TetrisGame`).
    *   `MainActivity.kt`: The main entry point of the application.

## Contributing

Contributions are welcome! If you have any ideas, suggestions, or find any bugs, feel free to open an issue or submit a pull request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
