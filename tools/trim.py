"""Trim the HackerTube export into the one JSON file the HNTube app ships as an asset.

Usage: python3 -I trim.py <export-dir> <out.json>

Keeps what the app shows (title, channel, summary, why HN, topics, HN stats, up to 5 recommending
comments, related videos). Drops comment bodies, embeddings and everything else.
"""
import json
import sys
from collections import defaultdict

src, out = sys.argv[1], sys.argv[2]


def rows(name):
    with open(f"{src}/{name}", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                yield json.loads(line)


taxonomy = json.load(open(f"{src}/taxonomy.json", encoding="utf-8"))
areas = [{"id": a["id"], "name": a["name"],
          "cats": [{"id": c["id"], "name": c["name"]} for c in a["categories"]]}
         for a in taxonomy["areas"]]

# Recommending comments per video, best first (strong recs, then newest).
recs = defaultdict(list)
last_month = {}
for m in rows("mentions.jsonl"):
    v = m["vid"]
    last_month[v] = max(last_month.get(v, ""), m.get("month") or "")
    if m.get("stance") in ("rec", "strong_rec"):
        recs[v].append(m)
for v in recs:
    recs[v].sort(key=lambda m: (m["stance"] != "strong_rec", -(int((m.get("month") or "0").replace("-", "")))))

related = {}
for r in rows("related.jsonl"):
    related[r["vid"]] = [x["vid"] for x in r["related"][:8]]

videos = []
for v in rows("videos.jsonl"):
    hn = v.get("hn") or {}
    q = v.get("quote") or {}
    videos.append({
        "v": v["vid"],
        "t": v["title"],
        "c": v.get("channel") or "",
        "s": v.get("summary") or "",
        "w": v.get("why_hn") or "",
        "k": v.get("content_type") or "",
        "l": v.get("level") or "",
        "n": 1 if v.get("shelf_life") == "news" else 0,
        "p": v.get("primary_topic") or "",
        "g": v.get("tags") or [],
        "a": [c["id"] for c in v.get("categories") or []],
        "q": q.get("text") or "",
        "qi": q.get("hn_comment_id") or 0,
        "qy": q.get("year") or "",
        "m": hn.get("mentions") or 0,
        "r": hn.get("recommenders") or 0,
        "y0": hn.get("first_year") or 0,
        "y1": hn.get("last_year") or 0,
        "lm": last_month.get(v["vid"], ""),
        "sc": v.get("score") or 0,
        "rel": related.get(v["vid"], []),
        "hn": [{
            "id": m["hn_comment_id"],
            "u": m.get("author") or "",
            "d": m.get("month") or "",
            "st": m.get("story_title") or "",
            "w": m.get("why") or "",
            "q": m.get("quote") or "",
            "s": 1 if m["stance"] == "strong_rec" else 0,
        } for m in recs.get(v["vid"], [])[:5]],
    })

data = {"generated": "2026-10-31", "source": "https://hackertube.org", "areas": areas, "videos": videos}
with open(out, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
print(len(videos), "videos,", sum(len(v["hn"]) for v in videos), "comments")
