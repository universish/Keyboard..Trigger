# Keyboard Trigger

> _"Bring the keyboard wherever you need it – fast, private, and under your control."_

Keyboard Trigger is a **free‑libre open‑source Android application** designed for people who value privacy and convenience.  Instead of reaching for the system UI or waiting for a text field to gain focus, a small floating button sits at the edge of your screen; tap it and the soft keyboard appears instantly.  It's ideal for users with accessibility needs, power users who like custom workflows, or anyone who hates hunting for the virtual keyboard.

---

## ✨ Highlights

- **Accessibility‑first**: built to work with Android's accessibility service, ensuring compatibility with screen readers and alternative input methods.
- **Private by design**: no analytics, no advertisements, no network permissions, no tracking of any kind.
- **Overlay‑free fallback**: if the system prevents drawing over apps, the button still works using a secure accessibility fallback.
- **Lightweight & open**: written in Kotlin, under GPLv3. Source code available on GitHub.

---

## 🚀 Installation

You can download the latest debug build from the [releases page](https://github.com/universish/Keyboard..Trigger/releases).  For development or testing, connect your device and run:

```bash
git clone https://github.com/universish/Keyboard..Trigger.git
cd Keyboard__Trigger
./gradlew installDebug
```

(Windows users should use `gradlew.bat`.)

---

## 📱 Usage Guide

1. Open **Settings → Accessibility** and enable **Keyboard Trigger**.
2. Grant overlay permission when prompted (or opt‑in to the fallback mode).
3. Drag the floating button to any screen edge and tap to summon the keyboard.
4. To reposition, long‑press and drag.
5. Toggle the selection bubble from the settings panel if desired.

This button works everywhere – homescreen, games, other apps – without stealing focus or spying on your input.

---

## 🛡 Privacy & Security

Your privacy is paramount. Keyboard Trigger does **not** collect or transmit any personal data.  There are **no servers, no network libraries, and no third‑party SDKs**.  Everything runs locally.  Debug logs strip out any potential identifiers.  You remain in control; nothing is sent to me or anyone else.

Because the app uses Android's accessibility service, it requires the corresponding permission – but that is solely to perform the core function of showing the keyboard.  It does **not** read or store what you type.

---

## 🐛 Reporting Bugs

If you find a problem or have a suggestion:

1. Open an issue at [GitHub Issues](https://github.com/universish/Keyboard..Trigger/issues).
2. Provide your device model, Android version, and a short reproduction sequence.
3. Attach logs if possible (see **Debug → Export Logs** in the app).

Debug reports are sanitized before sharing; personal information is automatically removed.

---

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on building, testing, and sending patches.

---

## 📜 License

This project is licensed under the **GNU General Public License v3 (or later)**.  You are free to use, study, share and improve the software.  See [LICENSE](LICENSE) for full text.

---

## 📂 Topics

The repository is tagged with keywords to help others discover it: android, kotlin, keyboard, accessibility, privacy, FOSS, input-method, trigger, and more.

---

**Developer:** universish (Saffet Yavuz)

Happy typing! 🎹


