package mw.noor.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException

class NoorRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("noor_local_v1", Context.MODE_PRIVATE)
    private val packsDirectory = File(appContext.filesDir, "content-packs").apply { mkdirs() }
    private val lessonsDirectory = File(appContext.filesDir, "content-lessons").apply { mkdirs() }
    private val packFileInstaller = PackFileInstaller()
    private data class LegacyPackCandidate(val file: File, val rawPack: String, val pack: ContentPack)
    private val catalog: List<ContentCatalogEntry> by lazy {
        appContext.assets.open("form3_catalog.json").bufferedReader().use { reader ->
            ContentJsonParser.parseCatalog(reader.readText())
        }
    }
    private val lessonIndex: LessonContentIndex by lazy {
        appContext.assets.open("lesson_catalog.json").bufferedReader().use { reader ->
            LessonContentJsonParser.parseIndex(reader.readText())
        }
    }

    fun catalog(): List<ContentCatalogEntry> = catalog
    fun lessonCatalog(): LessonContentIndex = lessonIndex.also { migrateLegacyPacks() }

    fun lessonEntry(packId: String, topicId: String, lessonId: String): LessonIndexEntry? =
        lessonIndex.lesson(packId, topicId, lessonId)

    fun lessonEntry(lessonId: String): LessonIndexEntry? = lessonIndex.lesson(lessonId)

    fun savedLessonIds(): Set<String> {
        migrateLegacyPacks()
        return lessonIndex.lessons.filter { loadStoredPayload(it) != null }.map { it.lessonId }.toSet()
    }

    fun savedLessonCount(packId: String): Int {
        val subject = lessonIndex.subject(packId) ?: return 0
        migrateLegacyPack(packId)
        return subject.lessons.count { loadStoredPayload(it) != null }
    }

    fun savedLessonBytes(): Long {
        migrateLegacyPacks()
        return lessonIndex.lessons.sumOf { entry ->
            lessonFile(entry).takeIf { loadStoredPayload(entry) != null }?.length() ?: 0L
        }
    }

    fun lessonSize(entry: LessonIndexEntry): Long = lessonFile(entry).takeIf { it.isFile }?.length() ?: 0L

    fun lessonStorageStatus(entry: LessonIndexEntry): PackStorageStatus {
        val availableBytes = lessonsDirectory.usableSpace.takeIf { it > 0L }
        val requiredBytes = entry.sizeBytes.coerceAtLeast(0L) + DOWNLOAD_STORAGE_RESERVE_BYTES
        return PackStorageStatus(availableBytes, requiredBytes, hasDownloadSpace(availableBytes, requiredBytes))
    }

    fun loadLesson(entry: LessonIndexEntry): ContentLesson? {
        migrateLegacyPack(entry.packId)
        val file = lessonFile(entry)
        if (!file.isFile) return null
        return loadStoredPayload(entry)?.lesson
    }

    fun deleteLesson(entry: LessonIndexEntry): Boolean {
        val file = lessonFile(entry)
        packFileInstaller.recover(file)
        return !file.exists() || file.delete()
    }

    suspend fun downloadLesson(
        entry: LessonIndexEntry,
        onProgress: suspend (percent: Int) -> Unit,
    ): ContentLesson = withContext(Dispatchers.IO) {
        val storage = lessonStorageStatus(entry)
        check(storage.canDownload) {
            "Not enough free storage to save this lesson safely. Free up space and try again."
        }
        var lastError: Throwable? = null
        for (sourceUrl in entry.sourceUrls) {
            var temporaryFile: File? = null
            var connection: HttpURLConnection? = null
            try {
                val activeConnection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Noor-Android/0.4.2")
                    instanceFollowRedirects = true
                }
                connection = activeConnection
                activeConnection.connect()
                val response = activeConnection.responseCode
                check(response in 200..299) { "Server returned HTTP $response" }
                val contentLength = activeConnection.contentLengthLong
                check(contentLength <= MAX_LESSON_BYTES) { "Lesson file is larger than the allowed limit." }
                temporaryFile = File(lessonsDirectory, "${entry.packId}__${entry.lessonId}.json.tmp")
                if (temporaryFile.exists()) temporaryFile.delete()
                var received = 0L
                FileOutputStream(temporaryFile).use { output ->
                    activeConnection.inputStream.use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            received += read
                            check(received <= MAX_LESSON_BYTES) { "Lesson file is larger than the allowed limit." }
                            output.write(buffer, 0, read)
                            val percent = if (contentLength > 0L) {
                                (received * 100L / contentLength).toInt().coerceIn(0, 99)
                            } else 0
                            onProgress(percent)
                        }
                        output.fd.sync()
                    }
                }
                check(sha256(temporaryFile) == entry.sha256) { "Lesson verification failed. Check your connection and try again." }
                val payload = LessonContentJsonParser.parsePayload(temporaryFile.readText())
                validatePayload(entry, payload)
                lessonFile(entry).parentFile?.mkdirs()
                packFileInstaller.install(temporaryFile, lessonFile(entry))
                onProgress(100)
                return@withContext payload.lesson
            } catch (error: Exception) {
                temporaryFile?.delete()
                if (error is CancellationException) throw error
                lastError = error
            } finally {
                connection?.disconnect()
            }
        }
        val failureDetails = generateSequence(lastError) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase(Locale.ROOT)
        val message = when {
            "enospc" in failureDetails || "no space" in failureDetails ->
                "The phone ran out of storage while saving this lesson. Free up space and try again."
            lastError is IOException ->
                "Download interrupted. Check your connection or available phone storage and try again."
            else -> lastError?.message ?: "Unable to download this lesson. Check your connection and try again."
        }
        throw IllegalStateException(message, lastError)
    }

    fun loadPack(entry: ContentCatalogEntry): ContentPack? {
        val file = packFile(entry.id)
        packFileInstaller.recover(file)
        if (!file.isFile) return null
        return runCatching {
            ContentJsonParser.parsePack(file.readText()).also { pack ->
                require(pack.id == entry.id)
                require(pack.form == entry.form && pack.subject == entry.subject)
            }
        }.getOrNull()
    }

    fun storageStatus(entry: ContentCatalogEntry): PackStorageStatus {
        val availableBytes = packsDirectory.usableSpace.takeIf { it > 0L }
        val requiredBytes = entry.sizeBytes.coerceAtLeast(0L) + DOWNLOAD_STORAGE_RESERVE_BYTES
        return PackStorageStatus(
            availableBytes = availableBytes,
            requiredBytes = requiredBytes,
            canDownload = hasDownloadSpace(availableBytes, requiredBytes),
        )
    }

    suspend fun downloadPack(
        entry: ContentCatalogEntry,
        onProgress: (percent: Int) -> Unit,
    ): ContentPack = withContext(Dispatchers.IO) {
        val storage = storageStatus(entry)
        check(storage.canDownload) {
            "Not enough free storage to safely download this pack. Free up space and try again."
        }
        var lastError: Throwable? = null
        for (sourceUrl in entry.sourceUrls) {
            var temporaryFile: File? = null
            var connection: HttpURLConnection? = null
            try {
                val activeConnection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Noor-Android/0.4.2")
                    instanceFollowRedirects = true
                }
                connection = activeConnection
                activeConnection.connect()
                val response = activeConnection.responseCode
                check(response in 200..299) { "Server returned HTTP $response" }
                val contentLength = activeConnection.contentLengthLong
                check(contentLength <= MAX_PACK_BYTES) { "Content pack is larger than the allowed limit." }
                temporaryFile = File(packsDirectory, "${entry.id}.json.tmp")
                if (temporaryFile.exists()) temporaryFile.delete()
                var received = 0L
                FileOutputStream(temporaryFile).use { output ->
                    activeConnection.inputStream.use { input ->
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

                check(sha256(temporaryFile) == entry.sha256) { "Downloaded pack verification failed. Check your connection and try again." }
                val pack = ContentJsonParser.parsePack(temporaryFile.readText())
                check(pack.id == entry.id) { "Downloaded pack does not match the catalogue." }
                check(pack.form == entry.form && pack.subject == entry.subject) { "Downloaded pack details do not match the catalogue." }
                check(pack.contentVersion == entry.contentVersion) { "Downloaded pack version does not match the catalogue." }
                packFileInstaller.install(temporaryFile, packFile(entry.id))
                onProgress(100)
                return@withContext pack
            } catch (error: Exception) {
                temporaryFile?.delete()
                if (error is CancellationException) throw error
                lastError = error
            } finally {
                connection?.disconnect()
            }
        }
        val failureDetails = generateSequence(lastError) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase(Locale.ROOT)
        val message = when {
            "enospc" in failureDetails || "no space" in failureDetails ->
                "The phone ran out of storage during the download. Free up space and try again."
            lastError is IOException ->
                "Download interrupted. Check your connection or available phone storage and try again."
            else -> lastError?.message ?: "Unable to download this lesson pack. Check your connection and try again."
        }
        throw IllegalStateException(message, lastError)
    }

    fun deletePack(entry: ContentCatalogEntry): Boolean {
        return packFile(entry.id).let { !it.exists() || it.delete() }
    }

    @Synchronized
    fun migrateLegacyPacks() {
        lessonIndex.subjects.forEach { migrateLegacyPack(it.id) }
    }

    @Synchronized
    fun migrateLegacyPack(packId: String) {
        val subject = lessonIndex.subject(packId) ?: return
        val catalogEntry = catalog.firstOrNull { it.id == packId } ?: return
        val legacyFile = packFile(packId)
        val legacyBackup = File(legacyFile.parentFile, "${legacyFile.name}.bak")
        val candidate = listOf(legacyFile, legacyBackup)
            .mapNotNull { file -> readLegacyPackCandidate(file, subject, catalogEntry) }
            .maxWithOrNull(compareBy<LegacyPackCandidate> { it.pack.contentVersion }.thenBy { it.file == legacyFile })
            ?: return
        // Restore the ordinary backup-only crash case through the installer. If a damaged main
        // file and a valid backup coexist, leave both untouched while using the validated backup.
        if (candidate.file == legacyBackup && !legacyFile.exists()) packFileInstaller.recover(legacyFile)

        subject.lessons.forEach { entry ->
            val destination = lessonFile(entry)
            if (loadStoredPayload(entry) != null) return@forEach
            runCatching {
                val payloadJson = LessonContentJsonParser.payloadFromLegacyPack(candidate.rawPack, entry)
                val payload = LessonContentJsonParser.parsePayload(payloadJson)
                validatePayload(entry, payload, allowOlderVersion = true)
                val staged = File(lessonsDirectory, "${entry.packId}__${entry.lessonId}.migration.tmp")
                FileOutputStream(staged).use { output ->
                    output.write(payloadJson.toByteArray(Charsets.UTF_8))
                    output.fd.sync()
                }
                packFileInstaller.install(staged, destination)
            }
        }
        if (subject.lessons.all { loadStoredPayload(it) != null }) {
            legacyFile.delete()
            legacyBackup.delete()
        }
    }

    private fun readLegacyPackCandidate(
        file: File,
        subject: LessonSubjectIndex,
        catalogEntry: ContentCatalogEntry,
    ): LegacyPackCandidate? {
        if (!file.isFile || file.length() > MAX_PACK_BYTES) return null
        val rawPack = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return null
        val pack = runCatching { ContentJsonParser.parsePack(rawPack) }.getOrNull() ?: return null
        if (pack.id != subject.id || pack.form != subject.form || pack.subject != subject.subject) return null
        if (pack.contentVersion !in 1..catalogEntry.contentVersion) return null
        // The current version has a trusted hash. Historical hashes are not retained in this
        // catalogue, so older installed packs remain eligible after structural validation.
        if (pack.contentVersion == catalogEntry.contentVersion &&
            runCatching { sha256(file) }.getOrNull() != catalogEntry.sha256
        ) return null
        return LegacyPackCandidate(file, rawPack, pack)
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

    fun hasValidatedConnection(): Boolean {
        val connectivity = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun studyStreak(nowMillis: Long = System.currentTimeMillis()): StudyStreak {
        val today = localDayKey(nowMillis)
        val yesterdayCalendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterday = localDayKey(yesterdayCalendar.timeInMillis)
        return currentStudyStreak(
            lastActivityDay = preferences.getString(KEY_STREAK_LAST_ACTIVITY_DAY, null),
            storedDays = preferences.getInt(KEY_STREAK_DAYS, 0),
            today = today,
            yesterday = yesterday,
        )
    }

    fun recordStudyActivity(nowMillis: Long = System.currentTimeMillis()): StudyStreak {
        val today = localDayKey(nowMillis)
        val yesterdayCalendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val next = nextStudyStreak(
            lastActivityDay = preferences.getString(KEY_STREAK_LAST_ACTIVITY_DAY, null),
            storedDays = preferences.getInt(KEY_STREAK_DAYS, 0),
            today = today,
            yesterday = localDayKey(yesterdayCalendar.timeInMillis),
        )
        preferences.edit()
            .putString(KEY_STREAK_LAST_ACTIVITY_DAY, today)
            .putInt(KEY_STREAK_DAYS, next.days)
            .apply()
        return next
    }

    fun completedLessonIds(): Set<String> = preferences.getStringSet(KEY_COMPLETED_LESSONS, emptySet()).orEmpty().toSet()

    fun wasLessonCompletedOffline(lessonId: String): Boolean =
        lessonId in preferences.getStringSet(KEY_OFFLINE_COMPLETED_LESSONS, emptySet()).orEmpty()

    fun markLessonCompleted(lessonId: String, completedOffline: Boolean) {
        val completed = completedLessonIds().toMutableSet().apply { add(lessonId) }
        val offlineCompleted = preferences.getStringSet(KEY_OFFLINE_COMPLETED_LESSONS, emptySet()).orEmpty().toMutableSet()
        if (completedOffline) offlineCompleted.add(lessonId) else offlineCompleted.remove(lessonId)

        val editor = preferences.edit().putStringSet(KEY_COMPLETED_LESSONS, completed)
        if (offlineCompleted.isEmpty()) editor.remove(KEY_OFFLINE_COMPLETED_LESSONS)
        else editor.putStringSet(KEY_OFFLINE_COMPLETED_LESSONS, offlineCompleted)
        editor.apply()
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
        val lessonIds = packs.flatMap { pack -> pack.topics.flatMap { topic -> topic.lessons.map { it.id } } }
        val topicIds = packs.flatMap { it.topics }.map { it.id }.toSet()
        return progressSummary(lessonIds, topicIds, nowMillis)
    }

    fun progressSummary(index: LessonContentIndex, nowMillis: Long = System.currentTimeMillis()): ProgressSummary =
        progressSummary(index.lessons.map { it.lessonId }, index.subjects.flatMap { subject -> subject.topics.map { it.id } }.toSet(), nowMillis)

    private fun progressSummary(lessonIds: List<String>, topicIds: Set<String>, nowMillis: Long): ProgressSummary {
        val completed = completedLessonIds()
        var quizzes = 0
        var correct = 0
        var answered = 0
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
            .remove(KEY_OFFLINE_COMPLETED_LESSONS)
            .remove(KEY_TOPIC_STATS)
            .remove(KEY_LAST_PACK)
            .remove(KEY_LAST_TOPIC)
            .remove(KEY_LAST_LESSON)
            .remove(KEY_STREAK_LAST_ACTIVITY_DAY)
            .remove(KEY_STREAK_DAYS)
            .apply()
    }

    private fun readTopicStats(): JSONObject = runCatching {
        JSONObject(preferences.getString(KEY_TOPIC_STATS, "{}") ?: "{}")
    }.getOrDefault(JSONObject())

    private fun localDayKey(nowMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))

    private fun packFile(id: String) = File(packsDirectory, "$id.json")
    private fun lessonFile(entry: LessonIndexEntry) = File(lessonsDirectory, "${entry.packId}__${entry.lessonId}.json")

    private fun loadStoredPayload(entry: LessonIndexEntry): LessonPayload? {
        val file = lessonFile(entry)
        packFileInstaller.recover(file)
        if (!file.isFile) return null
        return runCatching {
            LessonContentJsonParser.parsePayload(file.readText()).also { validatePayload(entry, it, allowOlderVersion = true) }
        }.getOrNull()
    }

    private fun validatePayload(entry: LessonIndexEntry, payload: LessonPayload, allowOlderVersion: Boolean = false) {
        require(payload.packId == entry.packId)
        require(payload.form == entry.form && payload.subject == entry.subject)
        require(payload.topicId == entry.topicId && payload.topicTitle == entry.topicTitle)
        require(payload.lesson.id == entry.lessonId && payload.lesson.title == entry.title)
        require(if (allowOlderVersion) payload.contentVersion in 1..entry.contentVersion else payload.contentVersion == entry.contentVersion)
    }

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
        private const val KEY_OFFLINE_COMPLETED_LESSONS = "offline_completed_lessons"
        private const val KEY_TOPIC_STATS = "topic_stats"
        private const val KEY_LAST_PACK = "last_pack"
        private const val KEY_LAST_TOPIC = "last_topic"
        private const val KEY_LAST_LESSON = "last_lesson"
        private const val KEY_STREAK_LAST_ACTIVITY_DAY = "streak_last_activity_day"
        private const val KEY_STREAK_DAYS = "streak_days"
        private const val MAX_PACK_BYTES = 2L * 1024L * 1024L
        private const val MAX_LESSON_BYTES = 1L * 1024L * 1024L
        private const val DOWNLOAD_STORAGE_RESERVE_BYTES = 64L * 1024L
        private const val SUCCESS_THRESHOLD = 0.8f
    }
}

data class PackStorageStatus(
    val availableBytes: Long?,
    val requiredBytes: Long,
    val canDownload: Boolean,
)
