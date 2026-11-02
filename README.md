# HNTube

The YouTube videos that Hacker News keeps telling each other to watch, as an app for the Rokid
Glasses (installed on the glasses themselves, next to SmartTube), Android TV boxes and phones.

Browse by Popular, Recently recommended, 150 topics, Save for later and Search. Every video shows
why HN recommends it, a summary and the HN comments that said so. **Play** hands the video to
SmartTube when it is installed, otherwise to the YouTube app, and as a last resort to YouTube's
embedded player.

Everything is on the device: no account, no server, and the browsing marks (watched, saved) stay
in the app's own storage.

## Data

The videos, topics and HN comments come from [HackerTube](https://hackertube.org) by Andrej
Karpathy (export of 2026-10-31, licence ODC-By 1.0, which in turn credits the
[open-index/hacker-news](https://huggingface.co/datasets/open-index/hacker-news) archive).
`tools/trim.py` turns the export into `app/src/main/assets/hntube.json.gz`; `tools/DATA.md` is
HackerTube's description of the export.

To refresh: download the export from hackertube.org, then

```sh
python3 -I tools/trim.py <export-dir> hntube.json && gzip -9 -c hntube.json > app/src/main/assets/hntube.json.gz
```

## Build

GitHub Actions (`.github/workflows/android.yml`) builds a signed release APK on every push to
`main` and attaches it to a GitHub Release on `v*` tags. Signing needs the four repository secrets
named in the workflow.

## Install on the Rokid Glasses

Same as SmartTube: put the APK on the phone, then Hi Rokid → Toolbox → Glasses App Management →
install. The glasses run Android 12 Go with a 480×640 viewport; HNTube uses a compact one-column
layout there. Navigation: single tap = select, two-finger swipe forward/back = next/previous item,
side-button double-click = back. SmartTube (package `org.smarttube.stable`) must be on the glasses
for playback; otherwise HNTube falls back to the YouTube app or its own web player.

## Install on an Android TV box

`adb connect <box-ip>` then `adb install -r HNTube-<version>-release.apk`, or any "send files to
TV" app.
