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

## 📱 Detailed Usage Guide

The core of Keyboard Trigger is the little floating button that lives at the
edge of your screen.  It can be placed on any side (left, right, top or bottom)
and it behaves like a sticky handle: once you tap it the soft keyboard instantly
appears, no matter which app or screen you are looking at.

### First‑time setup

1. Open **Settings → Accessibility** and enable **Keyboard Trigger**.  This
   grants the service permission to monitor window changes and display the
   keyboard on demand.
2. When you run the app it will ask for **display over other apps** permission.
   Grant it; if the system refuses you will be offered the security‑safe
   fallback (an invisible accessibility overlay).
3. Optionally enable the small **selection bubble** in the settings.  When
   enabled, tapping the button briefly shows a confirmation circle to reduce
   accidental taps.

### Positioning the button

- After installation the button appears at the left edge by default.
- **Reposition**: long–press and drag it anywhere along the edge or to another
  edge.  Release to stick it in place.
- **Hide / show**: use the notification that the service posts, or open the
  main app and toggle the visibility control.
- **Appearance**: the button’s colour and size are fixed for now; future
  versions may add themes.

### Using the button

1. Touch the floating button once.  If the keyboard was hidden, it will pop up
over whatever app you are currently using.
2. If the keyboard is already visible, tapping the button again has no effect
   (this prevents interference with standard typing).
3. To dismiss the keyboard, use the normal back gesture or tap the keyboard’s
   hide key; the button does not interfere.

### Fallback mode (no overlay)

Some devices or launchers block drawing over other apps.  In that case the
button will still appear but with slightly different behaviour: a tiny
accessibility window appears instead of a system overlay.  It still works the
same way, and the app will show a warning in the settings screen if you are in
fallback mode.

### Troubleshooting

- If the button disappears after a reboot, open the main app and press
  "Start / Restart service".
- If you cannot grant overlay permission, enable the fallback and reinstall the
  app.
- For detailed logs see **Debug → Export Logs**; they are sanitized and can be
  emailed to the developer.

This persistent, lightweight button allows you to summon the keyboard without
changing focus, opening notifications, or touching text fields.  It works
across the system – launcher, games, lock screen (if allowed) – and is safe
for sensitive input because it never records or transmits what you type.

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

**Developer:** universish (S____t Y___z)

---

## 🏷️Keywords

android, kotlin, keyboard, android-development, android-app, androiddevelopment, androidapp, androidapplication, android-application, keyboards, keyboard-shortcuts, keymapping, accessibilityservice, keymapper, android-accessibility, keyevent, privacy-focused, free-libre-oss, input-method, trigger, keyboard-trigger, keyboard-invoker, keyboard-launcher, bring_out_the_keyboard, reveal_the_keyboard, keyboard-widget, accessibility, keyboard-layout, keyboard-button, keyboard-trigger-button, input, without-tracker, keyboardmapper, keyboardmapping, keyboard-mapper, keyboard-mapping, keyboard-bringing, keyboard-invoking, keyboard-revealing, assistivetouch, Assistive-Touch, trigger-button, keyboard-bringer, keyboard-invoker, keyboard-exposer, keyboard-bringer, button, trigger, keytrigger, keybutton, keyboardbutton, keyboard-button


Happy typing! 🎹





