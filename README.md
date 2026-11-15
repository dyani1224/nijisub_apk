# VTuber Subs — Android prototype (v0.1)

An Android native proof-of-concept: enter a YouTube link, load your own timed SRT/VTT subtitles, view subtitles over the video, toggle immersive view, and experiment with Android PiP.

## Build without a PC
1. Create a **new GitHub repository** and upload the **contents** of this ZIP to its root (including `.github/workflows/android.yml`).
2. Go to **Actions → Build APK → Run workflow** (or push files to trigger the workflow).
3. When the workflow succeeds, download the `VTuberSubs-debug-apk` artifact, unzip it, and install `app-debug.apk` on your Android phone.
4. Enable installation from the relevant source if Android asks.

## Important limitations
- This is an **unverified prototype**. The APK was not compiled/tested in this environment; GitHub Actions may reveal build or device compatibility issues.
- **YouTube playback** uses the official embedded IFrame player, not a direct video stream. Some videos may disallow embedding. YouTube account sign-in, subscription browsing, membership access, real-time AI translation, audio-to-TXT alignment and background audio playback are **not implemented**.
- **PiP is experimental**: Android can put the Activity into PiP, but the embedded YouTube player may stop or behave differently in PiP; external subtitle visibility in PiP is not guaranteed.
- YouTube video playback and background features are subject to YouTube platform rules and technical limitations. This project does not extract YouTube streams or bypass platform restrictions.
- **SRT and standard VTT timestamps only**. Plain TXT lacks timing and is not supported yet.
- The user is responsible for using their own authorized content and subtitles.
