# REST

> A gentle sleep companion that tracks nighttime phone usage without judgment.

REST is an open-source Android app designed to help people with sleep anxiety. Built with compassion over metrics, warmth over guilt.

---

## Philosophy

**The strongest version of this app is you not needing it at all.**

REST was built on a simple insight: clock-watching at 3 AM is one of the most consistent drivers of insomnia, yet every sleep app shows you exactly what time it is. This app takes a different approach.

| Principle | What It Means |
|-----------|---------------|
| **No scoring** | Your sleep isn't a performance |
| **No goals** | Pressure to sleep prevents sleep |
| **No shame** | You get gentle truths, never judgment |
| **Designed to be left behind** | Success means uninstalling |

---

## Features

### During Sleep Sessions

| Feature | Description |
|---------|-------------|
| **Calming night messages** | One-line affirmations when you wake — not content to consume, just exit signs |
| **Event tracking** | Logs screen-on, unlock, and flashlight events during active sessions only |
| **Minimal interruption** | Runs quietly in the background with optimized battery usage |
| **Breathing exercises** | Three guided patterns (Box, Extended Exhale, 4-7-8) for settling back to sleep |

### Morning Insights

| Feature | Description |
|---------|-------------|
| **Sleep timeline** | Visual breakdown of your night with sleep cycles and wake episodes |
| **Quality analysis** | Based on wake clustering and cycle completion, not arbitrary scores |
| **Trend tracking** | Shows "a little less than usual" instead of raw counts that induce anxiety |
| **Session history** | Track patterns over time without leaderboards or competition |

### Core Values

```
Privacy-first     → All data stays on your device
Battery-conscious → Optimized background service (<2% drain per 24h)
No analytics      → Zero telemetry, no tracking
Open source       → MIT license, build it yourself
```

---

## Technical Stack

| Component | Technology |
|-----------|-----------|
| **Language** | Kotlin 1.9.20 |
| **UI Framework** | Jetpack Compose + Material Design 3 |
| **Database** | Room 2.6.1 (SQLite) |
| **Architecture** | MVVM with coroutines |
| **Min SDK** | 26 (Android 8.0 Oreo) |
| **Target SDK** | 34 (Android 14) |

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android device running Android 8.0+

### Installation

```bash
# Clone the repository
git clone https://github.com/pranavkumaarofficial/lock-and-flash.git
cd lock-and-flash

# Open in Android Studio
# File → Open → Select project folder
# Wait for Gradle sync

# Build and Run
# Connect your Android device or start an emulator
# Click Run (▶️) or press Shift+F10
```

### Required Permissions

| Permission | Purpose |
|------------|---------|
| `FOREGROUND_SERVICE` | For background sleep tracking |
| `POST_NOTIFICATIONS` | For persistent notification during sessions |
| `CAMERA` | For flashlight detection (never accesses camera) |

---

## How It Works

```
1. Start a sleep session
   ↓
2. Set your phone down
   ↓
3. App monitors screen activity in the background
   ↓
4. If you wake → See a calming message if you check your phone
   ↓
5. Morning insights → Review your night with gentle, non-judgmental feedback
```

**Important:** The app only tracks events during active sleep sessions. When you stop tracking, all monitoring stops immediately — no persistent background service, no constant tracking.

---

## Design System

REST follows Linear + Notion design principles for a calm, focused experience:

| Element | Specification |
|---------|--------------|
| **Canvas** | Deep dark (#010102) |
| **Accent** | Lavender (#6750A4) |
| **Spacing** | 4px base grid |
| **Typography** | Inter font family with negative letter-spacing |
| **Elevation** | 4-step surface ladder without shadows |

See [`RestDesignSystem.kt`](app/src/main/java/com/screentracker/app/RestDesignSystem.kt) for the complete token system.

---

## Contributing & Advocacy

This project exists as both a tool and an advocacy effort.

### Help Make This a Platform Feature

REST tracks nighttime phone usage, but the real solution is making the problem unnecessary. **Bedtime Mode should let you hide the lock screen clock during sleep sessions.**

#### Android Feature Request

**[⭐ Star the issue on Android Issue Tracker](https://issuetracker.google.com/issues/XXXXX)**

| Aspect | Details |
|--------|---------|
| **Why it matters** | Clock-watching at 3 AM is a clinically documented driver of insomnia<br>"Don't check the clock at night" is standard CBT-I (sleep therapy) guidance<br>Bedtime Mode already forces grayscale and enables DND — hiding the clock is the missing piece |
| **What we're asking** | • Opt-in toggle in Bedtime Mode (default OFF)<br>• Session-scoped — clock returns when bedtime ends<br>• Alarms unaffected — you still wake on time<br>• Evidence-based — aligned with sleep therapy best practices |
| **Impact** | One small toggle that could help millions sleep better |

This is a **Corporate Social Responsibility (CSR) ask**: a thoughtful feature that addresses a real health concern with minimal implementation complexity.

### Code Contributions

Contributions are welcome in these areas:

- Bug fixes and performance improvements
- Accessibility enhancements
- Battery optimization
- New calming message suggestions
- Translations

#### Pull Request Guidelines

1. Follow the existing code style (see [`CLAUDE.md`](CLAUDE.md))
2. Test on a physical device
3. Keep PRs focused on a single change
4. Update documentation if needed

---

## Roadmap

**Completed**
- [x] Sleep session tracking with quality analysis
- [x] Breathing exercises integration
- [x] Night message system
- [x] CSV export functionality

**Planned**
- [ ] Service health detection improvements
- [ ] Widget support
- [ ] Backup/restore functionality
- [ ] Multi-language support
- [ ] Wear OS companion app

---

## Known Issues

| Issue | Affected Devices | Solution |
|-------|-----------------|----------|
| **OEM battery killers** | Samsung/Xiaomi/OnePlus | Requires manual battery optimization exemption in device settings |
| **Android 12+ restrictions** | All devices on Android 12+ | May need additional permissions for reliable background operation |

See [`notes/TROUBLESHOOTING.md`](notes/TROUBLESHOOTING.md) for detailed solutions.

---

## License

This project is licensed under the MIT License - see the [`LICENSE`](LICENSE) file for details.

---

## Acknowledgments

Built with insights from:

- CBT-I (Cognitive Behavioral Therapy for Insomnia) sleep therapy principles
- The "I Am" manifestation app's gentle interaction model
- Linear, Notion, and Superhuman's minimalist design philosophy
- The insomnia community on Reddit

---

## Support

| Type | Link |
|------|------|
| **Bug Reports** | [GitHub Issues](https://github.com/pranavkumaarofficial/lock-and-flash/issues) |
| **Discussions** | [GitHub Discussions](https://github.com/pranavkumaarofficial/lock-and-flash/discussions) |

---

## How You Can Help

If REST helped you sleep better, consider:

1. **Star this repository** to help others find it
2. **Share with someone** who struggles with sleep anxiety
3. **Report bugs** to make it better for everyone
4. **Star the Android feature request** to advocate for platform-level change

---

**Remember:** The goal isn't perfect sleep. It's permission to rest, data to learn from, and eventually, not needing this app at all.

*Built with care and insomnia.*
