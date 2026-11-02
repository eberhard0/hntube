# HNTube

The YouTube videos that Hacker News keeps telling each other to watch, as an Android TV app for
the Rokid Station (watched in Rokid Air glasses) that also installs on a phone.

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

## Install on the Rokid Station

Enable Developer options on the Station (Settings → Device Preferences → About → tap Build seven
times), turn on USB/network debugging, then from a computer or phone on the same Wi-Fi:

```sh
adb connect <station-ip>
adb install -r HNTube-<version>-release.apk
```

Or use any "send files to TV" app and open the APK on the Station.
