package mw.noor.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class NoorRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("noor_local_v1", Context.MODE_PRIVATE)
    private val packsDirectory = File(appContext.filesDir, "content-packs").apply { mkdirs() }
    private val catalog: List<ContentCatalogEntry> by lazy {
        appContext.assets.open("form3_catalog.json").bufferedReader().use { reader ->
            ContentJsonParser.parseCatalog(reader.readText())
        }
    }

    fun catalog(): List<ContentCatalogEntry> = catalog

    fun loadPack(entry: ContentCatalogEntry): ContentPack? {
        val file = packFile(entry.id)
        if (!file.isFile) return null
        return runCatching {
            ContentJsonParser.parsePack(file.readText()).also { pack ->
                require(pack.id == entry.id)
                require(pack.contentVersion >= entry.contentVersion)
            }
        }.getOrNull()
    }

    suspend fun downloadPack(
        entry: ContentCatalogEntry,
        onProgress: (percent: Int) -> Unit,
    ): ContentPack = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        for (sourceUrl in entry.sourceUrls) {
            var temporaryFile: File? = null
            try {
                val connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Noor-Android/0.1.0")
                    instanceFollowRedirects = true
                }
                connection.connect()
                try {
                    val response = connection.responseCode
                    check(response in 200..299) { "Server returned HTTP $response" }
                    val contentLength = connection.contentLengthLong
                    check(contentLength <= MAX_PACK_BYTES) { "Content pack is larger than the allowed limit." }
                    temporaryFile = File(packsDirectory, "${entry.id}.json.tmp")
                    var received = 0L
                    FileOutputStream(temporaryFile).use { output ->
                        connection.inputStream.use { input ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                received += read
                                check(received <= MAX_PACK_BYTES) { "Content pack is larger than the allowed limit." }
                                output.write(buffer, 0, read)
                                val percent = if (contentLength > 0L) {
                                    ((received * 100L / contentLength).toInt()).coerceIn(0, 99)
                                } else {
                                    0
                                }
                                onProgress(percent)
                            }
                            output.fd.sync()
                        }
                    }
                    check(sha256(temporaryFile) == entry.sha256) { "Content pack checksum does not match." }
                    val pack = ContentJsonParser.parsePack(temporaryFile.readText())
                    check(pack.id == entry.id) { "Downloaded pack does not match the catalogue." }
                    check(pack.form == entry.form && pack.subject == entry.subject) { "Downloaded pack details do not match the catalogue." }
                    check(pack.contentVersion == entry.contentVersion) { "Downloaded pack version does not match the catalogue." }
                    val destination = packFile(entry.id)
                    if (destination.exists()) destination.delete()
                    check(temporaryFile.renameTo(destination)) { "Unable to save the downloaded content pack." }
                    onProgress(100)
                    return@withContext pack
                } finally {
                    connection.disconnect()
                }
            } catch (error: Exception) {
                temporaryFile?.delete()
                lastError = error
            }
        }
        throw IllegalStateException("Unable to download this lesson pack. Check your connection and try again.", lastError)
    }

    fun deletePack(entry: ContentCatalogEntry): Boolean {
        return packFile(entry.id).let { !it.exists() || it.delete() }
    }

    fun downloadedBytes(entries: List<ContentCatalogEntry> = catalog): Long = entries.sumOf { entry ->
        packFile(entry.id).takeIf { it.isFile }?.length() ?: 0L
    }

    fun packSize(entry: ContentCatalogEntry): Long =
        packFile(entry.id).takeIf { it.isFile }?.length() ?: 0L

    fun isSetupComplete(): Boolean = preferences.getBoolean(KEY_SETUP_COMPLETE, false)

    fun completeSetup(form: Int, language: String) {
        preferences.edit()
            .putBoolean(KEY_SETUP_COMPLETE, true)
            .putInt(KEY_FORM, form)
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun selectedForm(): Int = preferences.getInt(KEY_FORM, 3)
    fun language(): String = preferences.getString(KEY_LANGUAGE, "en") ?: "en"
    fun textScale(): Float = preferences.getFloat(KEY_TEXT_SCALE, 1f).coerceIn(0.9f, 1.3f)
    fun setTextScale(scale: Float) = preferences.edit().putFloat(KEY_TEXT_SCALE, scale.coerceIn(0.9f, 1.3f)).apply()

    fun completedLessonIds(): Set<String> = preferences.getStringSet(KEY_COMPLETED_LESSONS, emptySet()).orEmpty().toSet()

    fun markLessonCompleted(lessonId: String) {
        val updated = completedLessonIds().toMutableSet().apply { add(lessonId) }
        preferences.edit().putStringSet(KEY_COMPLETED_LESSONS, updated).apply()
    }

    fun saveLastLesson(lastLesson: LastLesson) {
        preferences.edit()
            .putString(KEY_LAST_PACK, lastLesson.packId)
            .putString(KEY_LAST_TOPIC, lastLesson.topicId)
            .putString(KEY_LAST_LESSON, lastLesson.lessonId)
            .apply()
    }

    fun lastLesson(): LastLesson? {
        val packId = preferences.getString(KEY_LAST_PACK, null) ?: return null
        val topicId = preferences.getString(KEY_LAST_TOPIC, null) ?: return null
        val lessonId = preferences.getString(KEY_LAST_LESSON, null) ?: return null
        return LastLesson(packId, topicId, lessonId)
    }

    fun recordQuizResult(topicId: String, correctAnswers: Int, questionCount: Int, nowMillis: Long = System.currentTimeMillis()) {
        if (questionCount <= 0) return
        val root = readTopicStats()
        val previous = root.optJSONObject(topicId) ?: JSONObject()
        val previousStreak = previous.optInt("successfulStreak", 0)
        val score = correctAnswers.toFloat() / questionCount.toFloat()
        val newStreak = if (score >= SUCCESS_THRESHOLD) previousStreak + 1 else 0
        val nextReviewAt = ReviewScheduler.nextReviewAt(score, newStreak, nowMillis)
        val updated = JSONObject()
            .put("attempts", previous.optInt("attempts", 0) + 1)
            .put("correctAnswers", previous.optInt("correctAnswers", 0) + correctAnswers)
            .put("answeredQuestions", previous.optInt("answeredQuestions", 0) + questionCount)
            .put("successfulStreak", newStreak)
            .put("lastScorePercent", (score * 100f).toInt())
            .put("nextReviewAtMillis", nextReviewAt)
        root.put(topicId, updated)
        preferences.edit().putString(KEY_TOPIC_STATS, root.toString()).apply()
    }

    fun topicProgress(topicId: String): TopicProgress? {
        val data = readTopicStats().optJSONObject(topicId) ?: return null
        return TopicProgress(
            topicId = topicId,
            attempts = data.optInt("attempts", 0),
            totalQuestions = data.optInt("answeredQuestions", 0),
            correctAnswers = data.optInt("correctAnswers", 0),
            successfulStreak = data.optInt("successfulStreak", 0),
            nextReviewAtMillis = data.optLong("nextReviewAtMillis", 0L),
            lastScorePercent = data.optInt("lastScorePercent", 0),
        )
    }

    fun dueTopicIds(nowMillis: Long = System.currentTimeMillis()): Set<String> {
        val root = readTopicStats()
        return root.keys().asSequence().filter { topicId ->
            val nextReview = root.optJSONObject(topicId)?.optLong("nextReviewAtMillis", Long.MAX_VALUE) ?: Long.MAX_VALUE
            nextReview <= nowMillis
        }.toSet()
    }

    fun progressSummary(packs: List<ContentPack>, nowMillis: Long = System.currentTimeMillis()): ProgressSummary {
        val completed = completedLessonIds()
        val lessonIds = packs.flatMap { pack -> pack.topics.flatMap { topic -> topic.lessons.map { it.id } } }
        var quizzes = 0
        var correct = 0
        var answered = 0
        val topicIds = packs.flatMap { it.topics }.map { it.id }.toSet()
        val root = readTopicStats()
        topicIds.forEach { id ->
            val stats = root.optJSONObject(id) ?: return@forEach
            quizzes += stats.optInt("attempts", 0)
            correct += stats.optInt("correctAnswers", 0)
            answered += stats.optInt("answeredQuestions", 0)
        }
        return ProgressSummary(
            completedLessons = lessonIds.count { it in completed },
            totalLessons = lessonIds.size,
            quizzesCompleted = quizzes,
            correctAnswers = correct,
            answeredQuestions = answered,
            dueTopicIds = dueTopicIds(nowMillis).intersect(topicIds),
        )
    }

    fun clearProgress() {
        preferences.edit()
            .remove(KEY_COMPLETED_LESSONS)
            .remove(KEY_TOPIC_STATS)
            .remove(KEY_LAST_PACK)
            .remove(KEY_LAST_TOPIC)
            .remove(KEY_LAST_LESSON)
            .apply()
    }

    private fun readTopicStats(): JSONObject = runCatching {
        JSONObject(preferences.getString(KEY_TOPIC_STATS, "{}") ?: "{}")
    }.getOrDefault(JSONObject())

    private fun packFile(id: String) = File(packsDirectory, "$id.json")

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_FORM = "selected_form"
        private const val KEY_LANGUAGE = "content_language"
        private const val KEY_TEXT_SCALE = "text_scale"
        private const val KEY_COMPLETED_LESSONS = "completed_lessons"
        private const val KEY_TOPIC_STATS = "topic_stats"
        private const val KEY_LAST_PACK = "last_pack"
        private const val KEY_LAST_TOPIC = "last_topic"
        private const val KEY_LAST_LESSON = "last_lesson"
        private const val MAX_PACK_BYTES = 2L * 1024L * 1024L
        private const val SUCCESS_THRESHOLD = 0.8f
    }
}
