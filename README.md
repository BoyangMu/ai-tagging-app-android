# AI Tagging App (Android)

An Android application that allows users to select or capture an image and generate **AI-powered tags** and an optional **AI-generated story or caption** based on the image. This project demonstrates a complete mobile workflow from image input → AI processing → user-facing results.

---

## Features

- Select an image from the device gallery (and/or capture a photo)
- Generate descriptive **AI tags** for photos
- Generate an **AI-written story or caption**
- **Sketch Tagger** to tag hand-drawn sketches
- Search and filter by tags (optionally include sketches)
- Simple multi-screen workflow:
  - **Home** – choose Photo Tagger, Sketch Tagger, or Story Teller
  - **Photo Tagger** – image capture/selection and tagging
  - **Sketch Tagger** – draw and tag sketches
  - **Story Teller** – generate stories from selected items

---

## Tech Stack

- Android (Java)
- Gradle (Kotlin DSL)
- External AI API for image understanding and text generation
- Standard Android UI components and Activities

---

## Project Structure

```text
ai-tagging-app-android/
├── app/                         # Main Android application module
│   └── src/main/java/.../
│       ├── PhotoTaggerActivity.java
│       ├── SketchTaggerActivity.java
│       └── StoryTellerActivity.java
├── docs/
│   └── screenshots/             # App screenshots used in README
├── gradle/                      # Gradle wrapper files
├── build.gradle.kts             # Project-level Gradle configuration
├── settings.gradle.kts          # Gradle settings
└── README.md
```

---

## Getting Started

### Prerequisites

- Android Studio (latest stable recommended)
- Android SDK (API level defined in Gradle config)
- An API key for the AI service used for tagging and story generation

### Running the App

1. Clone the repository:
   ```bash
   git clone https://github.com/BoyangMu/ai-tagging-app-android.git
   cd ai-tagging-app-android
   ```

2. Open the project in **Android Studio**

3. Let Gradle sync all dependencies

4. Run the app on an emulator or a physical Android device
   
---

## Permissions

Depending on your implementation and Android version, the app may require:

- `INTERNET` – for API requests
- Media access permissions – to select images
- Camera permission – if photo capture is enabled

All permissions must be declared in `AndroidManifest.xml` and requested at runtime where required.

---

## Usage

1. Launch the app
2. Choose **Photo Tagger**, **Sketch Tagger**, or **Story Teller**
3. Create/select content and generate tags or stories
4. Search and filter saved items by tags
5. View results directly in the app

---

## Known Limitations / Future Improvements

- Add loading indicators during AI requests
- Improve error handling and retry logic
- Cache results per image/sketch
- Add share/export functionality
- Refactor networking logic into a repository layer
- Add unit tests for API response parsing

---

## Author

**Boyang Mu**  
GitHub: https://github.com/BoyangMu
