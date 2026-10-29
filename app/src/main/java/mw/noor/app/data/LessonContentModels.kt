package mw.noor.app.data

import mw.noor.app.QuizQuestion
import org.json.JSONArray
import org.json.JSONObject

/** Metadata-only hierarchy bundled with the app so the library remains browsable before downloads. */
data class LessonContentIndex(
    val catalogVersion: Int,
    val subjects: List<LessonSubjectIndex>,
) {
    val lessons: List<LessonIndexEntry> get() = subjects.flatMap { subject ->
        subject.topics.flatMap { topic -> topic.lessons }
    }

    fun subject(id: String): LessonSubjectIndex? = subjects.firstOrNull { it.id == id }
    fun lesson(id: String): LessonIndexEntry? = lessons.firstOrNull { it.lessonId == id }
    fun lesson(packId: String, topicId: String, lessonId: String): LessonIndexEntry? =
        lessons.firstOrNull { it.packId == packId && it.topicId == topicId && it.lessonId == lessonId }
    fun topic(packId: String, topicId: String): LessonTopicIndex? =
        subject(packId)?.topics?.firstOrNull { it.id == topicId }

    fun nextLesson(entry: LessonIndexEntry): LessonIndexEntry? {
        val ordered = subject(entry.packId)?.lessons.orEmpty()
        val position = ordered.indexOfFirst { it.lessonId == entry.lessonId }
        return ordered.getOrNull(position + 1)
    }
}

data class LessonSubjectIndex(
    val id: String,
    val title: String,
    val subject: String,
    val form: Int,
    val description: String,
    val curriculumStatus: String,
    val topics: List<LessonTopicIndex>,
) {
    val lessons: List<LessonIndexEntry> get() = topics.flatMap { it.lessons }
}

data class LessonTopicIndex(
    val id: String,
    val title: String,
    val description: String,
    val lessons: List<LessonIndexEntry>,
)

data class LessonIndexEntry(
    val packId: String,
    val form: Int,
    val subject: String,
    val language: String,
    val curriculumStatus: String,
    val topicId: String,
    val topicTitle: String,
    val topicDescription: String,
    val lessonId: String,
    val title: String,
    val readTimeMinutes: Int,
    val contentVersion: Int,
    val sizeBytes: Long,
    val sha256: String,
    val sourceUrls: List<String>,
    val order: Int,
)

data class LessonPayload(
    val packId: String,
    val form: Int,
    val subject: String,
    val language: String,
    val contentVersion: Int,
    val curriculumStatus: String,
    val topicId: String,
    val topicTitle: String,
    val topicDescription: String,
    val lesson: ContentLesson,
)

object LessonContentJsonParser {
    fun parseIndex(raw: String): LessonContentIndex {
        val root = JSONObject(raw)
        val subjects = root.getJSONArray("subjects").objects().map { subject ->
            val packId = subject.getString("packId")
            val form = subject.getInt("form")
            val subjectName = subject.getString("subject")
            val language = subject.optString("language", "en")
            val contentVersion = subject.getInt("contentVersion")
            val curriculumStatus = subject.getString("curriculumStatus")
            var lessonOrder = 0
            val topics = subject.getJSONArray("topics").objects().map { topic ->
                val topicId = topic.getString("topicId")
                val topicTitle = topic.getString("title")
                val topicDescription = topic.getString("description")
                val lessons = topic.getJSONArray("lessons").objects().map { lesson ->
                    LessonIndexEntry(
                        packId = packId,
                        form = form,
                        subject = subjectName,
                        language = language,
                        curriculumStatus = curriculumStatus,
                        topicId = topicId,
                        topicTitle = topicTitle,
                        topicDescription = topicDescription,
                        lessonId = lesson.getString("lessonId"),
                        title = lesson.getString("title"),
                        readTimeMinutes = lesson.getInt("readTimeMinutes"),
                        contentVersion = contentVersion,
                        sizeBytes = lesson.getLong("sizeBytes"),
                        sha256 = lesson.getString("sha256").lowercase(),
                        sourceUrls = lesson.getJSONArray("sourceUrls").strings(),
                        order = lessonOrder++,
                    )
                }
                LessonTopicIndex(topicId, topicTitle, topicDescription, lessons)
            }
            LessonSubjectIndex(
                id = packId,
                title = subjectName,
                subject = subjectName,
                form = form,
                description = subject.optString("description", ""),
                curriculumStatus = curriculumStatus,
                topics = topics,
            )
        }
        require(root.getInt("catalogVersion") > 0) { "Lesson catalogue version must be positive." }
        val lessonIds = subjects.flatMap { it.lessons }.map { it.lessonId }
        require(lessonIds.isNotEmpty() && lessonIds.distinct().size == lessonIds.size) { "Lesson IDs must be present and unique." }
        subjects.forEach { subject ->
            require(subject.form in 1..4)
            require(subject.topics.map { it.id }.distinct().size == subject.topics.size)
            subject.lessons.forEach { lesson ->
                require(lesson.contentVersion > 0)
                require(lesson.sizeBytes > 0)
                require(lesson.sha256.matches(Regex("[a-f0-9]{64}")))
                require(lesson.sourceUrls.isNotEmpty())
            }
        }
        return LessonContentIndex(root.getInt("catalogVersion"), subjects)
    }

    fun parsePayload(raw: String): LessonPayload {
        val root = JSONObject(raw)
        val lessonJson = root.getJSONObject("lesson")
        val questions = lessonJson.getJSONArray("questions").objects().map { question ->
            QuizQuestion(
                id = question.getString("id"),
                prompt = question.getString("prompt"),
                options = question.getJSONArray("options").strings(),
                correctOptionIndex = question.getInt("correctOptionIndex"),
                explanation = question.getString("explanation"),
            )
        }
        val lesson = ContentLesson(
            id = lessonJson.getString("id"),
            title = lessonJson.getString("title"),
            readTimeMinutes = lessonJson.getInt("readTimeMinutes"),
            sections = lessonJson.getJSONArray("sections").objects().map { section ->
                LessonSection(section.getString("heading"), section.getString("body"))
            },
            keyTerms = lessonJson.getJSONArray("keyTerms").objects().map { term ->
                KeyTerm(term.getString("term"), term.getString("definition"))
            },
            questions = questions,
        )
        require(root.getInt("schemaVersion") == 1) { "Unsupported lesson payload format." }
        require(root.getInt("contentVersion") > 0)
        require(lesson.id.isNotBlank() && lesson.title.isNotBlank())
        require(lesson.sections.isNotEmpty())
        require(questions.isNotEmpty())
        require(questions.map { it.id }.distinct().size == questions.size)
        questions.forEach { question ->
            require(question.options.size >= 2)
            require(question.correctOptionIndex in question.options.indices)
        }
        return LessonPayload(
            packId = root.getString("packId"),
            form = root.getInt("form"),
            subject = root.getString("subject"),
            language = root.getString("language"),
            contentVersion = root.getInt("contentVersion"),
            curriculumStatus = root.getString("curriculumStatus"),
            topicId = root.getString("topicId"),
            topicTitle = root.getString("topicTitle"),
            topicDescription = root.getString("topicDescription"),
            lesson = lesson,
        )
    }

    /** Extract one lesson from a validated legacy subject pack without changing its learning content. */
    fun payloadFromLegacyPack(rawPack: String, entry: LessonIndexEntry): String {
        val pack = JSONObject(rawPack)
        val topics = pack.getJSONArray("topics")
        val topic = (0 until topics.length()).asSequence()
            .map { topics.getJSONObject(it) }
            .firstOrNull { it.getString("id") == entry.topicId }
            ?: error("Legacy pack is missing topic ${entry.topicId}.")
        val lessons = topic.getJSONArray("lessons")
        val lesson = (0 until lessons.length()).asSequence()
            .map { lessons.getJSONObject(it) }
            .firstOrNull { it.getString("id") == entry.lessonId }
            ?: error("Legacy pack is missing lesson ${entry.lessonId}.")
        return JSONObject()
            .put("schemaVersion", 1)
            .put("packId", pack.getString("id"))
            .put("form", pack.getInt("form"))
            .put("subject", pack.getString("subject"))
            .put("language", pack.getString("language"))
            .put("contentVersion", pack.getInt("contentVersion"))
            .put("curriculumStatus", pack.getString("curriculumStatus"))
            .put("topicId", topic.getString("id"))
            .put("topicTitle", topic.getString("title"))
            .put("topicDescription", topic.getString("description"))
            .put("lesson", lesson)
            .toString()
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
}
