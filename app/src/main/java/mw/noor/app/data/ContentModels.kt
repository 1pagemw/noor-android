package mw.noor.app.data

import mw.noor.app.QuizQuestion
import org.json.JSONArray
import org.json.JSONObject

 data class ContentCatalogEntry(
    val id: String,
    val title: String,
    val subject: String,
    val form: Int,
    val description: String,
    val topicCount: Int,
    val contentVersion: Int,
    val sha256: String,
    val sizeBytes: Long,
    val sourceUrls: List<String>,
    val curriculumStatus: String,
)

data class ContentPack(
    val id: String,
    val form: Int,
    val subject: String,
    val language: String,
    val contentVersion: Int,
    val curriculumStatus: String,
    val topics: List<ContentTopic>,
)

data class ContentTopic(
    val id: String,
    val title: String,
    val description: String,
    val lessons: List<ContentLesson>,
)

data class ContentLesson(
    val id: String,
    val title: String,
    val readTimeMinutes: Int,
    val sections: List<LessonSection>,
    val keyTerms: List<KeyTerm>,
    val questions: List<QuizQuestion>,
    val isOfflineAvailable: Boolean = true,
)

data class LessonSection(val heading: String, val body: String)
data class KeyTerm(val term: String, val definition: String)
data class LastLesson(val packId: String, val topicId: String, val lessonId: String)
data class TopicProgress(
    val topicId: String,
    val attempts: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val successfulStreak: Int,
    val nextReviewAtMillis: Long,
    val lastScorePercent: Int,
)
data class ProgressSummary(
    val completedLessons: Int,
    val totalLessons: Int,
    val quizzesCompleted: Int,
    val correctAnswers: Int,
    val answeredQuestions: Int,
    val dueTopicIds: Set<String>,
)

object ContentJsonParser {
    fun parseCatalog(raw: String): List<ContentCatalogEntry> {
        val packs = JSONObject(raw).getJSONArray("packs")
        return (0 until packs.length()).map { index ->
            val item = packs.getJSONObject(index)
            ContentCatalogEntry(
                id = item.getString("id"),
                title = item.getString("title"),
                subject = item.getString("subject"),
                form = item.getInt("form"),
                description = item.getString("description"),
                topicCount = item.getInt("topicCount"),
                contentVersion = item.getInt("contentVersion"),
                sha256 = item.getString("sha256").lowercase(),
                sizeBytes = item.getLong("sizeBytes"),
                sourceUrls = item.getJSONArray("sourceUrls").toStringList(),
                curriculumStatus = item.getString("curriculumStatus"),
            )
        }
    }

    fun parsePack(raw: String): ContentPack {
        val root = JSONObject(raw)
        val pack = ContentPack(
            id = root.getString("id"),
            form = root.getInt("form"),
            subject = root.getString("subject"),
            language = root.getString("language"),
            contentVersion = root.getInt("contentVersion"),
            curriculumStatus = root.getString("curriculumStatus"),
            topics = root.getJSONArray("topics").objects().map { topic ->
                ContentTopic(
                    id = topic.getString("id"),
                    title = topic.getString("title"),
                    description = topic.getString("description"),
                    lessons = topic.getJSONArray("lessons").objects().map { lesson ->
                        ContentLesson(
                            id = lesson.getString("id"),
                            title = lesson.getString("title"),
                            readTimeMinutes = lesson.getInt("readTimeMinutes"),
                            sections = lesson.getJSONArray("sections").objects().map { section ->
                                LessonSection(section.getString("heading"), section.getString("body"))
                            },
                            keyTerms = lesson.getJSONArray("keyTerms").objects().map { term ->
                                KeyTerm(term.getString("term"), term.getString("definition"))
                            },
                            questions = lesson.getJSONArray("questions").objects().map { question ->
                                QuizQuestion(
                                    id = question.getString("id"),
                                    prompt = question.getString("prompt"),
                                    options = question.getJSONArray("options").toStringList(),
                                    correctOptionIndex = question.getInt("correctOptionIndex"),
                                    explanation = question.getString("explanation"),
                                )
                            },
                        )
                    },
                )
            },
        )
        validate(pack)
        return pack
    }

    private fun validate(pack: ContentPack) {
        require(pack.form in 1..4) { "Pack has an invalid form." }
        require(pack.contentVersion > 0) { "Pack version must be positive." }
        require(pack.topics.isNotEmpty()) { "Pack must contain at least one topic." }
        require(pack.topics.map { it.id }.distinct().size == pack.topics.size) { "Topic IDs must be unique." }
        val lessonIds = pack.topics.flatMap { it.lessons }.map { it.id }
        require(lessonIds.isNotEmpty() && lessonIds.distinct().size == lessonIds.size) { "Lesson IDs must be present and unique." }
        val questions = pack.topics.flatMap { it.lessons }.flatMap { it.questions }
        require(questions.isNotEmpty() && questions.map { it.id }.distinct().size == questions.size) { "Question IDs must be present and unique." }
        questions.forEach { question ->
            require(question.options.size >= 2) { "Each question must have at least two options." }
            require(question.correctOptionIndex in question.options.indices) { "Question answer index is invalid." }
        }
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.toStringList(): List<String> = (0 until length()).map { getString(it) }
}
