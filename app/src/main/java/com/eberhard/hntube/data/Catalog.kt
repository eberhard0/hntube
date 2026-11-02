package com.eberhard.hntube.data

import android.content.Context
import android.util.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

/** One HN comment that recommended the video. */
data class HnComment(
    val id: Long,
    val author: String,
    val month: String,
    val story: String,
    val why: String,
    val quote: String,
    val strong: Boolean,
) {
    val url: String get() = "https://news.ycombinator.com/item?id=$id"
}

data class Video(
    val id: String,
    val title: String,
    val channel: String,
    val summary: String,
    val whyHn: String,
    val kind: String,
    val level: String,
    val news: Boolean,
    val topic: String,
    val tags: List<String>,
    val categories: List<String>,
    val quote: String,
    val quoteId: Long,
    val quoteYear: String,
    val mentions: Int,
    val recommenders: Int,
    val firstYear: Int,
    val lastYear: Int,
    val lastMonth: String,
    val score: Double,
    val related: List<String>,
    val comments: List<HnComment>,
) {
    val youtubeUrl: String get() = "https://www.youtube.com/watch?v=$id"
    val embedUrl: String get() = "https://www.youtube-nocookie.com/embed/$id?autoplay=1&playsinline=1&rel=0"
    /** A frame from the middle of the video, like HackerTube shows (no API key needed). */
    val thumbnail: String get() = "https://i.ytimg.com/vi/$id/hq2.jpg"
    val quoteUrl: String get() = "https://news.ycombinator.com/item?id=$quoteId"
    /** "20 HN recommenders · 2018–2026". */
    val hnLine: String
        get() {
            val years = if (firstYear == 0) "" else if (firstYear == lastYear) " · $firstYear" else " · $firstYear–$lastYear"
            return if (recommenders == 0) "cited on HN, never recommended$years"
            else "$recommenders HN recommender${if (recommenders == 1) "" else "s"}$years"
        }
}

data class Category(val id: String, val name: String, val areaId: String, val areaName: String)
data class Area(val id: String, val name: String, val categories: List<Category>)

/** The whole dataset, loaded once from the bundled asset. */
class Catalog(val generated: String, val areas: List<Area>, val videos: List<Video>) {
    private val byId = videos.associateBy { it.id }
    val categories: Map<String, Category> = areas.flatMap { it.categories }.associateBy { it.id }

    operator fun get(id: String): Video? = byId[id]

    /** Recommended, not news-only: what most lists show. `videos` is already best-scored first. */
    private val shown: List<Video> = videos.filter { it.recommenders > 0 && !it.news }

    val popular: List<Video> get() = shown
    val recent: List<Video> by lazy { shown.sortedWith(compareByDescending<Video> { it.lastMonth }.thenByDescending { it.score }) }

    fun inCategory(id: String): List<Video> = shown.filter { id in it.categories }

    fun countIn(id: String): Int = inCategory(id).size

    fun related(v: Video): List<Video> = v.related.mapNotNull { byId[it] }

    /** Plain word search over title, channel, topic, tags and summary; also finds cited-only videos. */
    fun search(query: String): List<Video> {
        val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        return videos.mapNotNull { v ->
            val title = v.title.lowercase()
            val head = "$title ${v.channel} ${v.topic} ${v.tags.joinToString(" ")}".lowercase()
            val body = v.summary.lowercase()
            var score = 0.0
            for (w in words) {
                score += when {
                    title.contains(w) -> 3.0
                    head.contains(w) -> 2.0
                    body.contains(w) -> 1.0
                    else -> return@mapNotNull null
                }
            }
            v to score * (1 + v.score / 10)
        }.sortedByDescending { it.second }.map { it.first }
    }

    companion object {
        suspend fun load(context: Context): Catalog = withContext(Dispatchers.IO) {
            context.assets.open("hntube.json.gz").use { raw ->
                JsonReader(InputStreamReader(GZIPInputStream(raw), Charsets.UTF_8)).use { r -> read(r) }
            }
        }

        private fun read(r: JsonReader): Catalog {
            var generated = ""
            val areas = ArrayList<Area>()
            val videos = ArrayList<Video>(7000)
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "generated" -> generated = r.nextString()
                    "areas" -> {
                        r.beginArray()
                        while (r.hasNext()) areas += readArea(r)
                        r.endArray()
                    }
                    "videos" -> {
                        r.beginArray()
                        while (r.hasNext()) videos += readVideo(r)
                        r.endArray()
                    }
                    else -> r.skipValue()
                }
            }
            r.endObject()
            return Catalog(generated, areas, videos)
        }

        private fun readArea(r: JsonReader): Area {
            var id = ""
            var name = ""
            val cats = ArrayList<Pair<String, String>>()
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "id" -> id = r.nextString()
                    "name" -> name = r.nextString()
                    "cats" -> {
                        r.beginArray()
                        while (r.hasNext()) {
                            var cid = ""
                            var cname = ""
                            r.beginObject()
                            while (r.hasNext()) {
                                when (r.nextName()) {
                                    "id" -> cid = r.nextString()
                                    "name" -> cname = r.nextString()
                                    else -> r.skipValue()
                                }
                            }
                            r.endObject()
                            cats += cid to cname
                        }
                        r.endArray()
                    }
                    else -> r.skipValue()
                }
            }
            r.endObject()
            return Area(id, name, cats.map { (cid, cname) -> Category(cid, cname, id, name) })
        }

        private fun readStrings(r: JsonReader): List<String> {
            val out = ArrayList<String>()
            r.beginArray()
            while (r.hasNext()) out += r.nextString()
            r.endArray()
            return out
        }

        private fun readComments(r: JsonReader): List<HnComment> {
            val out = ArrayList<HnComment>()
            r.beginArray()
            while (r.hasNext()) {
                var id = 0L
                var u = ""
                var d = ""
                var st = ""
                var w = ""
                var q = ""
                var s = false
                r.beginObject()
                while (r.hasNext()) {
                    when (r.nextName()) {
                        "id" -> id = r.nextLong()
                        "u" -> u = r.nextString()
                        "d" -> d = r.nextString()
                        "st" -> st = r.nextString()
                        "w" -> w = r.nextString()
                        "q" -> q = r.nextString()
                        "s" -> s = r.nextInt() == 1
                        else -> r.skipValue()
                    }
                }
                r.endObject()
                out += HnComment(id, u, d, st, w, q, s)
            }
            r.endArray()
            return out
        }

        private fun readVideo(r: JsonReader): Video {
            var v = ""; var t = ""; var c = ""; var s = ""; var w = ""; var k = ""; var l = ""
            var n = false; var p = ""; var g: List<String> = emptyList(); var a: List<String> = emptyList()
            var q = ""; var qi = 0L; var qy = ""; var m = 0; var rc = 0; var y0 = 0; var y1 = 0
            var lm = ""; var sc = 0.0; var rel: List<String> = emptyList(); var hn: List<HnComment> = emptyList()
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "v" -> v = r.nextString()
                    "t" -> t = r.nextString()
                    "c" -> c = r.nextString()
                    "s" -> s = r.nextString()
                    "w" -> w = r.nextString()
                    "k" -> k = r.nextString()
                    "l" -> l = r.nextString()
                    "n" -> n = r.nextInt() == 1
                    "p" -> p = r.nextString()
                    "g" -> g = readStrings(r)
                    "a" -> a = readStrings(r)
                    "q" -> q = r.nextString()
                    "qi" -> qi = r.nextLong()
                    "qy" -> qy = r.nextString()
                    "m" -> m = r.nextInt()
                    "r" -> rc = r.nextInt()
                    "y0" -> y0 = r.nextInt()
                    "y1" -> y1 = r.nextInt()
                    "lm" -> lm = r.nextString()
                    "sc" -> sc = r.nextDouble()
                    "rel" -> rel = readStrings(r)
                    "hn" -> hn = readComments(r)
                    else -> r.skipValue()
                }
            }
            r.endObject()
            return Video(v, t, c, s, w, k, l, n, p, g, a, q, qi, qy, m, rc, y0, y1, lm, sc, rel, hn)
        }
    }
}
