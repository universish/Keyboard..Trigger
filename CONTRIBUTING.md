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

This project prefers to handle problem reports via email rather than GitHub
issues.  The built‑in debug tool (see the README) lets you capture a sanitized
log that you can inspect before sending; the app then composes an email with
that log attached.  Please use that mechanism so that any sensitive information
remains private.

1. Do **not** create a GitHub issue for a bug unless instructed otherwise.
2. Use the app’s export‑and‑send feature to deliver your report.
3. When emailing, include device model, Android version, and a short
   reproduction sequence.

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


    ## Keywords

android, kotlin, keyboard, android-development, android-app, androiddevelopment, androidapp, androidapplication, android-application, keyboards, keyboard-shortcuts, keymapping, accessibilityservice, keymapper, android-accessibility, keyevent, privacy-focused, free-libre-oss, input-method, trigger, keyboard-trigger, keyboard-invoker, keyboard-launcher, bring_out_the_keyboard, reveal_the_keyboard, keyboard-widget, accessibility, keyboard-layout, keyboard-button, keyboard-trigger-button, input, without-tracker, keyboardmapper, keyboardmapping, keyboard-mapper, keyboard-mapping, keyboard-bringing, keyboard-invoking, keyboard-revealing, assistivetouch, Assistive-Touch, trigger-button, keyboard-bringer, keyboard-invoker, keyboard-exposer, keyboard-bringer, button, trigger, keytrigger, keybutton, keyboardbutton, keyboard-button


