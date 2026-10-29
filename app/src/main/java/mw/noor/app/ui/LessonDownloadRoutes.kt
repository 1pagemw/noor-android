package mw.noor.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import kotlinx.coroutines.flow.flowOf
import mw.noor.app.data.ContentLesson
import mw.noor.app.data.ContentPack
import mw.noor.app.data.ContentTopic
import mw.noor.app.data.LessonContentIndex
import mw.noor.app.data.LessonDownloadWork
import mw.noor.app.data.LessonIndexEntry
import mw.noor.app.data.LessonSubjectIndex
import mw.noor.app.data.LessonTopicIndex
import mw.noor.app.data.NoorRepository
import mw.noor.app.ui.theme.NoorColors

@Composable
internal fun LessonDownloadRoute(
    subject: LessonSubjectIndex,
    topic: LessonTopicIndex,
    entry: LessonIndexEntry,
    repository: NoorRepository,
    onBack: () -> Unit,
    onStartQuiz: () -> Unit,
    onReportContent: () -> Unit,
    onProgressChanged: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val workInfos by remember(entry.lessonId) {
        LessonDownloadWork.observe(context, entry.lessonId)
    }.collectAsState(initial = emptyList())
    val work = workInfos.currentDownload()
    val lesson = remember(entry.lessonId, work?.id, work?.state) { repository.loadLesson(entry) }

    LaunchedEffect(entry.lessonId, lesson, work?.id, work?.state) {
        if (lesson == null && shouldAutomaticallyStartDownload(work?.state)) LessonDownloadWork.enqueueRequested(context, entry)
        if (lesson != null && work?.state == WorkInfo.State.SUCCEEDED) onProgressChanged()
    }

    if (lesson == null) {
        DownloadWaitingScreen(
            title = entry.title,
            work = work,
            onBack = onBack,
            onRetry = { LessonDownloadWork.enqueueRequested(context, entry) },
        )
        return
    }

    val topicContent = ContentTopic(
        id = topic.id,
        title = topic.title,
        description = topic.description,
        lessons = topic.lessons.map { lessonEntry ->
            if (lessonEntry.lessonId == entry.lessonId) lesson
            else repository.loadLesson(lessonEntry) ?: lessonEntry.placeholder()
        },
    )
    val pack = subject.toPack(listOf(topicContent))
    LessonScreen(
        pack = pack,
        topic = topicContent,
        lesson = lesson,
        repository = repository,
        onBack = onBack,
        onStartQuiz = onStartQuiz,
        onReportContent = onReportContent,
        onProgressChanged = onProgressChanged,
    )
}

@Composable
internal fun TopicQuizRoute(
    subject: LessonSubjectIndex,
    topic: LessonTopicIndex,
    repository: NoorRepository,
    onBack: () -> Unit,
    onFinished: (correct: Int, total: Int) -> Unit,
    onProgressChanged: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val firstLesson = topic.lessons.firstOrNull()
    val workFlow = remember(firstLesson?.lessonId) {
        firstLesson?.let { LessonDownloadWork.observe(context, it.lessonId) } ?: flowOf(emptyList())
    }
    val workInfos by workFlow.collectAsState(initial = emptyList())
    val work = workInfos.currentDownload()
    val cachedLessons = topic.lessons.mapNotNull { entry -> repository.loadLesson(entry) }

    LaunchedEffect(firstLesson?.lessonId, cachedLessons.size, work?.id, work?.state) {
        if (cachedLessons.isEmpty() && firstLesson != null && shouldAutomaticallyStartDownload(work?.state)) {
            LessonDownloadWork.enqueueRequested(context, firstLesson)
        }
        if (cachedLessons.isNotEmpty() && work?.state == WorkInfo.State.SUCCEEDED) onProgressChanged()
    }

    if (cachedLessons.isEmpty()) {
        if (firstLesson == null) MissingContentScreen(onBack = onBack)
        else DownloadWaitingScreen(
            title = "Practice · ${topic.title}",
            work = work,
            onBack = onBack,
            onRetry = { LessonDownloadWork.enqueueRequested(context, firstLesson) },
        )
        return
    }

    val quizTopic = ContentTopic(topic.id, topic.title, topic.description, cachedLessons)
    val pack = subject.toPack(listOf(quizTopic))
    QuizScreen(
        pack = pack,
        topic = quizTopic,
        repository = repository,
        onBack = onBack,
        onFinished = onFinished,
        onProgressChanged = onProgressChanged,
        cachedLessonCount = cachedLessons.size,
        totalLessonCount = topic.lessons.size,
    )
}

@Composable
private fun DownloadWaitingScreen(
    title: String,
    work: WorkInfo?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val state = work?.state
    val percent = work?.progress?.getInt("percent", 0)?.coerceIn(0, 100) ?: 0
    ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
        BackHeader("Preparing lesson", title, onBack)
        when (state) {
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                Text(work.outputData.getString("error") ?: "The lesson could not be downloaded.", color = NoorColors.MossDark)
                PrimaryButton(text = "Retry download", onClick = onRetry, showArrow = false)
            }
            WorkInfo.State.SUCCEEDED -> {
                Text("The lesson could not be verified on this device. Try downloading it again.", color = NoorColors.MossDark)
                PrimaryButton(text = "Try again", onClick = onRetry, showArrow = false)
            }
            else -> {
                if (state == WorkInfo.State.RUNNING && percent > 0) {
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = NoorColors.Moss,
                        trackColor = NoorColors.Mint,
                    )
                    Text("Preparing lesson · $percent%", style = MaterialTheme.typography.titleMedium)
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NoorColors.Moss, trackColor = NoorColors.Mint)
                    Text(
                        if (state == WorkInfo.State.RUNNING) "Preparing lesson…" else "Waiting for a connection",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text("Downloads use Wi‑Fi or mobile data. The lesson will be saved on this device.", style = MaterialTheme.typography.bodyMedium, color = NoorColors.MossDark)
            }
        }
    }
}

private fun List<WorkInfo>.currentDownload(): WorkInfo? =
    firstOrNull { it.state == WorkInfo.State.RUNNING }
        ?: firstOrNull { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
        ?: firstOrNull()

internal fun shouldAutomaticallyStartDownload(state: WorkInfo.State?): Boolean =
    state == null || state == WorkInfo.State.CANCELLED

private fun LessonSubjectIndex.toPack(topics: List<ContentTopic>): ContentPack = ContentPack(
    id = id,
    form = form,
    subject = subject,
    language = lessons.firstOrNull()?.language ?: "en",
    contentVersion = lessons.maxOfOrNull { it.contentVersion } ?: 1,
    curriculumStatus = curriculumStatus,
    topics = topics,
)

private fun LessonIndexEntry.placeholder(): ContentLesson = ContentLesson(
    id = lessonId,
    title = title,
    readTimeMinutes = readTimeMinutes,
    sections = emptyList(),
    keyTerms = emptyList(),
    questions = emptyList(),
    isOfflineAvailable = false,
)

internal fun LessonSubjectIndex.toContentPack(catalogEntry: mw.noor.app.data.ContentCatalogEntry, repository: NoorRepository): ContentPack {
    val topics = topics.map { topic ->
        ContentTopic(
            id = topic.id,
            title = topic.title,
            description = topic.description,
            lessons = topic.lessons.map { entry -> repository.loadLesson(entry) ?: entry.placeholder() },
        )
    }
    return ContentPack(
        id = id,
        form = form,
        subject = subject,
        language = lessons.firstOrNull()?.language ?: "en",
        contentVersion = lessons.maxOfOrNull { it.contentVersion } ?: catalogEntry.contentVersion,
        curriculumStatus = curriculumStatus,
        topics = topics,
    )
}
