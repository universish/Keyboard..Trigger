# Contributing to Keyboard Trigger

Thank you for your interest in helping out!  Whether you're fixing a bug, adding a feature or improving the docs, your contributions make this project better for everyone.

## Getting the code

```bash
git clone https://github.com/universish/Keyboard..Trigger.git
cd Keyboard__Trigger
```

The project uses the Gradle wrapper (`gradlew` / `gradlew.bat`).  On Unix-like systems use `./gradlew`, on Windows `.

## Building & testing

- `./gradlew assembleDebug` produces a debug APK.
- `./gradlew assembleRelease` creates a signed release build (requires `local.properties` with keystore info).
- A device connected via USB or emulator is recommended for manual testing.

Before submitting an issue or pull request, please verify that your change builds and the existing functionality still works.

## Style guidelines

- Code is written in Kotlin; follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Use `snake_case` for resource names and `camelCase` for variables.
- XML layout files should be tidy and use string resources for text.

## Reporting bugs

1. Search existing issues first—your problem may already be known.
2. Open a new issue with a clear title and steps to reproduce.
3. Include device model, Android version, and any log output (logs are sanitized automatically).

## Submitting patches

1. Fork the repository and create a feature branch:
   ```bash
   git checkout -b fix-awesome-button
   ```
2. Develop your changes. Keep commits focused and atomic.
3. Run `./gradlew assembleDebug` and test manually.
4. Push your branch to your fork and open a pull request.
5. Pull requests should target `main` and include a description of what was changed and why.

## Code review

All submissions are reviewed; please be patient.  I may request changes or improvements before merging.

## Communication

- Use GitHub Issues for bugs and feature requests.
- For discussions you can open an issue labeled `discussion`.

## Community

Be respectful, follow the [Contributor Covenant](https://www.contributor-covenant.org/), and help keep the project welcoming.

Happy hacking!