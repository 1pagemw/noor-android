package mw.noor.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mw.noor.app.BuildConfig
import mw.noor.app.data.ContentCatalogEntry
import mw.noor.app.data.ContentLesson
import mw.noor.app.data.ContentPack
import mw.noor.app.data.ContentTopic
import mw.noor.app.data.NoorRepository
import mw.noor.app.data.TopicProgress
import mw.noor.app.ui.theme.NoorColors
import java.util.Locale
import kotlin.math.roundToInt

private enum class NoorTab(val title: String, val icon: ImageVector) {
    TODAY("Today", Icons.Outlined.Home),
    LEARN("Learn", Icons.Outlined.MenuBook),
    REVIEW("Review", Icons.Outlined.Refresh),
    PROGRESS("Progress", Icons.Outlined.BarChart),
}

@Composable
fun NoorApp() {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { NoorRepository(context) }
    val catalog = remember { repository.catalog() }
    val scope = rememberCoroutineScope()

    var setupComplete by remember { mutableStateOf(repository.isSetupComplete()) }
    var selectedTabName by rememberSaveable { mutableStateOf(NoorTab.TODAY.name) }
    var encodedNavigationState by rememberSaveable { mutableStateOf(RouteHistory.initial().encode()) }
    var selectedPackId by rememberSaveable { mutableStateOf("") }
    var selectedTopicId by rememberSaveable { mutableStateOf("") }
    var selectedLessonId by rememberSaveable { mutableStateOf("") }
    var dataVersion by remember { mutableIntStateOf(0) }
    var downloadingPackId by rememberSaveable { mutableStateOf("") }
    var downloadPercent by rememberSaveable { mutableIntStateOf(0) }
    var downloadError by rememberSaveable { mutableStateOf("") }
    var textScale by remember { mutableStateOf(repository.textScale()) }

    val navigationState = remember(encodedNavigationState) { RouteHistory.decode(encodedNavigationState) }
    val route = navigationState.currentRoute
    val navigateTo: (String) -> Unit = { destination ->
        encodedNavigationState = navigationState.navigateTo(destination).encode()
    }
    val navigateBack: () -> Unit = {
        encodedNavigationState = navigationState.navigateBack().encode()
    }
    BackHandler(enabled = route != "tabs") { navigateBack() }

    val baseDensity = LocalDensity.current
    val adjustedDensity = remember(baseDensity.density, baseDensity.fontScale, textScale) {
        Density(baseDensity.density, baseDensity.fontScale * textScale)
    }

    CompositionLocalProvider(LocalDensity provides adjustedDensity) {
        if (!setupComplete) {
            WelcomeScreen(onContinue = {
                repository.completeSetup(form = 3, language = "en")
                setupComplete = true
            })
        } else {
            val currentPackEntry = catalog.firstOrNull { it.id == selectedPackId }
            val currentPack = currentPackEntry?.let { repository.loadPack(it) }
            val currentTopic = currentPack?.topics?.firstOrNull { it.id == selectedTopicId }
            val currentLesson = currentTopic?.lessons?.firstOrNull { it.id == selectedLessonId }
            val downloadedPacks = catalog.mapNotNull { entry -> repository.loadPack(entry) }

            when (route) {
                "settings" -> SettingsScreen(
                    entries = catalog,
                    repository = repository,
                    textScale = textScale,
                    onTextScaleChange = {
                        textScale = it
                        repository.setTextScale(it)
                    },
                    onBack = navigateBack,
                    onDataChanged = { dataVersion += 1 },
                )
                "subject" -> if (currentPackEntry != null) {
                    SubjectScreen(
                        entry = currentPackEntry,
                        pack = currentPack,
                        repository = repository,
                        downloadingPackId = downloadingPackId,
                        downloadPercent = downloadPercent,
                        downloadError = downloadError,
                        onBack = navigateBack,
                        onDownload = { entry ->
                            downloadError = ""
                            downloadingPackId = entry.id
                            downloadPercent = 0
                            scope.launch {
                                try {
                                    repository.downloadPack(entry) { percent -> downloadPercent = percent }
                                    dataVersion += 1
                                } catch (_: Exception) {
                                    downloadError = "Download did not finish. Check your connection and try again."
                                } finally {
                                    downloadingPackId = ""
                                }
                            }
                        },
                        onOpenLesson = { topic, lesson ->
                            selectedTopicId = topic.id
                            selectedLessonId = lesson.id
                            repository.saveLastLesson(mw.noor.app.data.LastLesson(currentPackEntry.id, topic.id, lesson.id))
                            navigateTo("lesson")
                        },
                        onStartQuiz = { topic ->
                            selectedTopicId = topic.id
                            navigateTo("quiz")
                        },
                    )
                } else {
                    LearningTabs(
                        selectedTabName = selectedTabName,
                        entries = catalog,
                        repository = repository,
                        downloadedPacks = downloadedPacks,
                        dataVersion = dataVersion,
                        onSelectTab = { selectedTabName = it },
                        onOpenPack = { entry ->
                            selectedPackId = entry.id
                            navigateTo("subject")
                        },
                        onOpenSettings = { navigateTo("settings") },
                        onStartReview = { entry, topic ->
                            selectedPackId = entry.id
                            selectedTopicId = topic.id
                            selectedTabName = NoorTab.REVIEW.name
                            navigateTo("quiz")
                        },
                        onContinueLesson = { last ->
                            selectedPackId = last.packId
                            selectedTopicId = last.topicId
                            selectedLessonId = last.lessonId
                            navigateTo("lesson")
                        },
                    )
                }
                "lesson" -> if (currentTopic != null && currentLesson != null) {
                    LessonScreen(
                        pack = currentPack!!,
                        topic = currentTopic,
                        lesson = currentLesson,
                        repository = repository,
                        onBack = navigateBack,
                        onStartQuiz = {
                            navigateTo("quiz")
                        },
                        onProgressChanged = { dataVersion += 1 },
                    )
                } else {
                    MissingContentScreen(onBack = navigateBack)
                }
                "quiz" -> if (currentTopic != null && currentPack != null) {
                    QuizScreen(
                        pack = currentPack,
                        topic = currentTopic,
                        repository = repository,
                        onBack = { navigateBack() },
                        onFinished = { correct, total ->
                            repository.recordQuizResult(currentTopic.id, correct, total)
                            dataVersion += 1
                            navigateBack()
                        },
                    )
                } else {
                    MissingContentScreen(onBack = navigateBack)
                }
                else -> LearningTabs(
                    selectedTabName = selectedTabName,
                    entries = catalog,
                    repository = repository,
                    downloadedPacks = downloadedPacks,
                    dataVersion = dataVersion,
                    onSelectTab = { selectedTabName = it },
                    onOpenPack = { entry ->
                        selectedPackId = entry.id
                        navigateTo("subject")
                    },
                    onOpenSettings = { navigateTo("settings") },
                    onStartReview = { entry, topic ->
                        selectedPackId = entry.id
                        selectedTopicId = topic.id
                        selectedTabName = NoorTab.REVIEW.name
                        navigateTo("quiz")
                    },
                    onContinueLesson = { last ->
                        selectedPackId = last.packId
                        selectedTopicId = last.topicId
                        selectedLessonId = last.lessonId
                        navigateTo("lesson")
                    },
                )
            }
        }
    }
}

@Composable
private fun WelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).background(NoorColors.Canvas).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text("Noor", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = NoorColors.Moss)
        Spacer(Modifier.height(8.dp))
        Text("Your Form 3 study companion", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Learn a topic, practise questions, and review at your own pace. The pilot includes Biology and Physics lesson packs.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        InfoCard(title = "Works offline", body = "Download a subject pack once, then use its lessons and quizzes without a connection.", icon = Icons.Outlined.Download)
        InfoCard(title = "Your progress stays here", body = "No account is needed. Study progress is stored on this phone and can be cleared in Settings.", icon = Icons.Outlined.Info)
        InfoCard(title = "Pilot content", body = "These Form 3 lesson packs are drafts awaiting review by a Malawi teacher. They are not official exam advice.", icon = Icons.Outlined.Info)
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "Start with Form 3", onClick = onContinue)
        Spacer(Modifier.height(10.dp))
        Text("Language: English (pilot content)", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.7f))
    }
}

@Composable
private fun LearningTabs(
    selectedTabName: String,
    entries: List<ContentCatalogEntry>,
    repository: NoorRepository,
    downloadedPacks: List<ContentPack>,
    dataVersion: Int,
    onSelectTab: (String) -> Unit,
    onOpenPack: (ContentCatalogEntry) -> Unit,
    onOpenSettings: () -> Unit,
    onStartReview: (ContentCatalogEntry, ContentTopic) -> Unit,
    onContinueLesson: (mw.noor.app.data.LastLesson) -> Unit,
) {
    val selected = NoorTab.entries.firstOrNull { it.name == selectedTabName } ?: NoorTab.TODAY
    Scaffold(
        containerColor = NoorColors.Canvas,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NoorTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { onSelectTab(tab.name) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NoorColors.Moss,
                            selectedTextColor = NoorColors.Moss,
                            indicatorColor = NoorColors.Mint,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        when (selected) {
            NoorTab.TODAY -> TodayScreen(
                modifier = Modifier.padding(padding),
                entries = entries,
                repository = repository,
                downloadedPacks = downloadedPacks,
                onOpenPack = onOpenPack,
                onOpenSettings = onOpenSettings,
                onContinueLesson = onContinueLesson,
            )
            NoorTab.LEARN -> LearnScreen(
                modifier = Modifier.padding(padding),
                entries = entries,
                repository = repository,
                onOpenPack = onOpenPack,
                onOpenSettings = onOpenSettings,
            )
            NoorTab.REVIEW -> ReviewScreen(
                modifier = Modifier.padding(padding),
                entries = entries,
                downloadedPacks = downloadedPacks,
                repository = repository,
                onStartReview = onStartReview,
            )
            NoorTab.PROGRESS -> ProgressScreen(
                modifier = Modifier.padding(padding),
                downloadedPacks = downloadedPacks,
                repository = repository,
                dataVersion = dataVersion,
            )
        }
    }
}

@Composable
private fun TodayScreen(
    modifier: Modifier,
    entries: List<ContentCatalogEntry>,
    repository: NoorRepository,
    downloadedPacks: List<ContentPack>,
    onOpenPack: (ContentCatalogEntry) -> Unit,
    onOpenSettings: () -> Unit,
    onContinueLesson: (mw.noor.app.data.LastLesson) -> Unit,
) {
    ScreenColumn(modifier) {
        ScreenHeader(title = "Noor", subtitle = "Form 3 · Biology and Physics", onSettings = onOpenSettings)
        Text("Small steps. Strong progress.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        val lastLesson = repository.lastLesson()
        val continueEntry = lastLesson?.let { last -> entries.firstOrNull { it.id == last.packId } }
        val continuePack = continueEntry?.let(repository::loadPack)
        val continueTopic = continuePack?.topics?.firstOrNull { it.id == lastLesson?.topicId }
        val continueLesson = continueTopic?.lessons?.firstOrNull { it.id == lastLesson?.lessonId }
        if (continueEntry != null && continueTopic != null && continueLesson != null) {
            Surface(color = NoorColors.MossDark, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CONTINUE LEARNING", style = MaterialTheme.typography.labelMedium, color = NoorColors.Mint)
                    Text("${continueEntry.subject} · ${continueTopic.title}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.86f))
                    Text(continueLesson.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                    PrimaryButton(text = "Open lesson", onClick = { onContinueLesson(lastLesson!!) })
                }
            }
        } else {
            InfoCard(
                title = if (downloadedPacks.isEmpty()) "Choose a subject to begin" else "Your lessons are ready",
                body = if (downloadedPacks.isEmpty()) "Download Biology or Physics while connected. Lessons and quizzes will then work offline." else "Open a saved pack and continue with a topic.",
                icon = Icons.Outlined.Download,
            )
        }
        Text("Your Form 3 subjects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        entries.forEach { entry ->
            PackCard(entry, downloaded = repository.loadPack(entry) != null, onClick = { onOpenPack(entry) })
        }
        Text("Progress is saved on this phone. No account is required.", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
    }
}

@Composable
private fun LearnScreen(
    modifier: Modifier,
    entries: List<ContentCatalogEntry>,
    repository: NoorRepository,
    onOpenPack: (ContentCatalogEntry) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val savedSubjects = entries.filter { repository.loadPack(it) != null }
    val availableSubjects = entries.filter { repository.loadPack(it) == null }
    val downloadMessage = when {
        entries.isEmpty() -> "No subject packs are available yet."
        savedSubjects.isEmpty() -> "Download ${availableSubjects.joinToString(" or ") { it.subject }} while connected. Saved packs can then be used offline."
        availableSubjects.isEmpty() -> "All available subjects are saved offline: ${savedSubjects.joinToString(", ") { it.subject }}."
        else -> "Saved offline: ${savedSubjects.joinToString(", ") { it.subject }}. Available to download: ${availableSubjects.joinToString(", ") { it.subject }}."
    }
    ScreenColumn(modifier) {
        ScreenHeader("Learn", "Form 3 pilot packs", onOpenSettings)
        InfoCard(
            title = if (availableSubjects.isEmpty() && savedSubjects.isNotEmpty()) "All subjects ready offline" else "Offline availability",
            body = downloadMessage,
            icon = if (availableSubjects.isEmpty() && savedSubjects.isNotEmpty()) Icons.Outlined.CheckCircle else Icons.Outlined.Download,
        )
        entries.forEach { entry ->
            PackCard(entry, downloaded = repository.loadPack(entry) != null, onClick = { onOpenPack(entry) })
        }
        InfoCard("Draft content", "Topic headings are provisional. A local teacher should review each lesson before classroom use.", Icons.Outlined.Info)
    }
}

@Composable
private fun PackCard(entry: ContentCatalogEntry, downloaded: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, NoorColors.Mint),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(color = NoorColors.Moss, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (downloaded) Icons.Outlined.CheckCircle else Icons.Outlined.MenuBook, contentDescription = null, tint = Color.White)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Form ${entry.form} · ${countLabel(entry.topicCount, "topic")}", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.7f))
                Text(if (downloaded) "Saved offline" else "About ${formatBytes(entry.sizeBytes)} · download to study offline", style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark)
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = "Open ${entry.subject}", tint = NoorColors.Moss)
        }
    }
}

@Composable
private fun SubjectScreen(
    entry: ContentCatalogEntry,
    pack: ContentPack?,
    repository: NoorRepository,
    downloadingPackId: String,
    downloadPercent: Int,
    downloadError: String,
    onBack: () -> Unit,
    onDownload: (ContentCatalogEntry) -> Unit,
    onOpenLesson: (ContentTopic, ContentLesson) -> Unit,
    onStartQuiz: (ContentTopic) -> Unit,
) {
    ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
        BackHeader(title = entry.subject, subtitle = "Form ${entry.form}", onBack = onBack)
        Text(entry.description, style = MaterialTheme.typography.bodyLarge)
        InfoCard(entry.curriculumStatus, "These draft topics need local teacher review before classroom use.", Icons.Outlined.Info)
        if (pack == null) {
            Surface(color = NoorColors.MossDark, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Save this pack for offline study", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("${countLabel(entry.topicCount, "topic")} · about ${formatBytes(entry.sizeBytes)}", color = NoorColors.Mint)
                    if (downloadingPackId == entry.id) {
                        LinearProgressIndicator(progress = { downloadPercent / 100f }, modifier = Modifier.fillMaxWidth(), color = NoorColors.Mint, trackColor = Color.White.copy(alpha = 0.24f))
                        Text(if (downloadPercent == 0) "Connecting…" else "Downloading $downloadPercent%", color = Color.White)
                    } else {
                        Button(
                            onClick = { onDownload(entry) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NoorColors.Mint, contentColor = NoorColors.MossDark),
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Download ${entry.subject}")
                        }
                    }
                }
            }
            if (downloadError.isNotBlank()) InfoCard("Download not complete", downloadError, Icons.Outlined.Info)
            Text("Pack downloads use a small HTTPS file and are checked before saving.", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.7f))
        } else {
            InfoCard("Available offline", "Version ${pack.contentVersion} · ${countLabel(pack.topics.size, "topic")} · stored on this phone", Icons.Outlined.CheckCircle)
            pack.topics.forEach { topic ->
                val topicProgress = repository.topicProgress(topic.id)
                TopicCard(
                    topic = topic,
                    progress = topicProgress,
                    completedLessons = repository.completedLessonIds(),
                    onOpenLesson = { lesson -> onOpenLesson(topic, lesson) },
                    onStartQuiz = { onStartQuiz(topic) },
                )
            }
        }
    }
}

@Composable
private fun TopicCard(
    topic: ContentTopic,
    progress: TopicProgress?,
    completedLessons: Set<String>,
    onOpenLesson: (ContentLesson) -> Unit,
    onStartQuiz: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, NoorColors.Mint)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(topic.description, style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.72f))
            topic.lessons.forEach { lesson ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (lesson.id in completedLessons) Icons.Outlined.CheckCircle else Icons.Outlined.MenuBook, contentDescription = null, tint = NoorColors.Moss, modifier = Modifier.size(19.dp))
                    Text(lesson.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("${lesson.readTimeMinutes} min", style = MaterialTheme.typography.labelSmall, color = NoorColors.Ink.copy(alpha = 0.6f))
                }
                Button(
                    onClick = { onOpenLesson(lesson) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NoorColors.Mint, contentColor = NoorColors.MossDark),
                ) { Text(if (lesson.id in completedLessons) "Review lesson" else "Open lesson") }
            }
            if (progress != null) {
                Text("Last quiz: ${progress.lastScorePercent}% · ${if (progress.nextReviewAtMillis <= System.currentTimeMillis()) "review due" else "review scheduled"}", style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark)
            }
            TextButton(onClick = onStartQuiz, modifier = Modifier.align(Alignment.End)) { Text("Practise topic") }
        }
    }
}

@Composable
private fun LessonScreen(
    pack: ContentPack,
    topic: ContentTopic,
    lesson: ContentLesson,
    repository: NoorRepository,
    onBack: () -> Unit,
    onStartQuiz: () -> Unit,
    onProgressChanged: () -> Unit,
) {
    var completed by remember(lesson.id) { mutableStateOf(lesson.id in repository.completedLessonIds()) }
    ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
        BackHeader(title = pack.subject, subtitle = topic.title, onBack = onBack)
        Text(lesson.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("About ${countLabel(lesson.readTimeMinutes, "minute")} · Form ${pack.form}", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
        InfoCard(pack.curriculumStatus, "Use this draft to study, and ask a teacher to verify syllabus details.", Icons.Outlined.Info)
        lesson.sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(section.heading, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = NoorColors.MossDark)
                Text(section.body, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal, lineHeight = 24.sp))
            }
        }
        if (lesson.keyTerms.isNotEmpty()) {
            Text("Key terms", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            lesson.keyTerms.forEach { term ->
                Surface(color = Color.White, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, NoorColors.Mint)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(term.term, fontWeight = FontWeight.SemiBold, color = NoorColors.MossDark)
                        Text(term.definition, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (completed) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = NoorColors.Moss, modifier = Modifier.size(18.dp))
                Text("Lesson completed", style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark)
            }
        } else {
            TextButton(
                onClick = {
                    repository.markLessonCompleted(lesson.id)
                    completed = true
                    onProgressChanged()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Mark lesson complete")
            }
        }
        PrimaryButton(text = "Practise this topic", onClick = onStartQuiz)
    }
}

@Composable
private fun ReviewScreen(
    modifier: Modifier,
    entries: List<ContentCatalogEntry>,
    downloadedPacks: List<ContentPack>,
    repository: NoorRepository,
    onStartReview: (ContentCatalogEntry, ContentTopic) -> Unit,
) {
    val dueIds = repository.dueTopicIds()
    val dueTopics = downloadedPacks.flatMap { pack ->
        val entry = entries.firstOrNull { it.id == pack.id } ?: return@flatMap emptyList()
        pack.topics.filter { it.id in dueIds }.map { entry to it }
    }
    ScreenColumn(modifier) {
        ScreenHeader("Review", "Your private practice queue")
        if (downloadedPacks.isEmpty()) {
            InfoCard("Download a subject first", "Open Learn and save Biology or Physics to use its lessons and quizzes offline.", Icons.Outlined.Download)
        } else if (dueTopics.isEmpty()) {
            InfoCard("Nothing is due right now", "A topic becomes due after a practice session. You can still practise any saved topic below.", Icons.Outlined.CheckCircle)
        } else {
            Text("Due now · ${countLabel(dueTopics.size, "topic")}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            dueTopics.forEach { (entry, topic) -> ReviewCard(entry, topic, repository.topicProgress(topic.id), onStartReview) }
        }
        if (downloadedPacks.isNotEmpty()) {
            Text(if (dueTopics.isEmpty()) "Practise a saved topic" else "Other saved topics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            downloadedPacks.forEach { pack ->
                val entry = entries.firstOrNull { it.id == pack.id } ?: return@forEach
                pack.topics.filter { it.id !in dueIds }.forEach { topic -> ReviewCard(entry, topic, repository.topicProgress(topic.id), onStartReview) }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    entry: ContentCatalogEntry,
    topic: ContentTopic,
    progress: TopicProgress?,
    onStartReview: (ContentCatalogEntry, ContentTopic) -> Unit,
) {
    Card(
        onClick = { onStartReview(entry, topic) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NoorColors.Mint),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${entry.subject} · Form ${entry.form}", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
                if (progress != null) Text("Last score ${progress.lastScorePercent}%", style = MaterialTheme.typography.labelSmall, color = NoorColors.MossDark)
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = "Start ${topic.title} review", tint = NoorColors.Moss)
        }
    }
}

@Composable
private fun ProgressScreen(
    modifier: Modifier,
    downloadedPacks: List<ContentPack>,
    repository: NoorRepository,
    dataVersion: Int,
) {
    val summary = remember(dataVersion, downloadedPacks) { repository.progressSummary(downloadedPacks) }
    val completedLessonIds = remember(dataVersion) { repository.completedLessonIds() }
    ScreenColumn(modifier) {
        ScreenHeader("Your progress", "Stored on this phone")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("${summary.completedLessons}/${summary.totalLessons}", "lessons complete", Modifier.weight(1f))
            val accuracy = if (summary.answeredQuestions == 0) "—" else "${summary.correctAnswers * 100 / summary.answeredQuestions}%"
            MetricCard(accuracy, "practice accuracy", Modifier.weight(1f))
        }
        InfoCard(
            "${countLabel(summary.quizzesCompleted, "practice session")} completed",
            "${countLabel(summary.dueTopicIds.size, "topic")} due for another look.",
            Icons.Outlined.BarChart,
        )
        if (downloadedPacks.isEmpty()) {
            InfoCard("Progress starts here", "Download Biology or Physics to see lesson and practice progress.", Icons.Outlined.Download)
        } else {
            downloadedPacks.forEach { pack ->
                Text("${pack.subject} · Form ${pack.form}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                pack.topics.forEach { topic ->
                    val progress = repository.topicProgress(topic.id)
                    val lessonDone = topic.lessons.count { it.id in completedLessonIds }
                    val lessonTotal = topic.lessons.size
                    val fractions = topicProgressFractions(lessonDone, lessonTotal, progress?.lastScorePercent)
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(topic.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Lessons", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.72f))
                            Text("$lessonDone/$lessonTotal complete", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.72f))
                        }
                        NoorProgressBar(
                            fraction = fractions.lessonCompletion,
                            description = "Lesson completion: $lessonDone of $lessonTotal",
                        )
                        if (progress == null) {
                            Text("Quiz · Not practised", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Latest quiz", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.72f))
                                Text("${progress.lastScorePercent}%", style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark, fontWeight = FontWeight.SemiBold)
                            }
                            NoorProgressBar(
                                fraction = fractions.latestQuizAccuracy ?: 0f,
                                description = "Latest quiz score ${progress.lastScorePercent} percent",
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    entries: List<ContentCatalogEntry>,
    repository: NoorRepository,
    textScale: Float,
    onTextScaleChange: (Float) -> Unit,
    onBack: () -> Unit,
    onDataChanged: () -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
        BackHeader("Settings", "Form 3 pilot", onBack)
        InfoCard("No account", "Noor stores downloaded lessons and progress on this phone. Clearing app data or uninstalling removes them.", Icons.Outlined.Info)
        SettingLine("Form", "Form ${repository.selectedForm()} · this pilot includes Form 3")
        SettingLine("Content language", if (repository.language() == "en") "English · only language in v0.1.0" else repository.language())
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Text size · ${(textScale * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Slider(value = textScale, onValueChange = onTextScaleChange, valueRange = 0.9f..1.3f)
            Text("Uses your phone's display size, with an optional Noor adjustment.", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
        }
        Text("Saved lesson packs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        val storedBytes = repository.downloadedBytes(entries)
        Text("${formatBytes(storedBytes)} stored by Noor", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
        entries.forEach { entry ->
            val pack = repository.loadPack(entry)
            if (pack != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, NoorColors.Mint)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.subject, fontWeight = FontWeight.SemiBold)
                            Text("Version ${pack.contentVersion} · ${formatBytes(repository.packSize(entry))}", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
                        }
                        IconButton(onClick = { repository.deletePack(entry); onDataChanged() }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove ${entry.subject} download", tint = NoorColors.Moss)
                        }
                    }
                }
            }
        }
        Button(
            onClick = { confirmClear = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NoorColors.Mint, contentColor = NoorColors.MossDark),
        ) { Text("Clear learning progress") }
        Text("Noor ${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.62f))
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear learning progress?") },
            text = { Text("This removes completed lessons, quiz scores, and review schedules from this phone. Saved lesson packs are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    repository.clearProgress()
                    confirmClear = false
                    onDataChanged()
                }) { Text("Clear progress") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun QuizScreen(
    pack: ContentPack,
    topic: ContentTopic,
    repository: NoorRepository,
    onBack: () -> Unit,
    onFinished: (correct: Int, total: Int) -> Unit,
) {
    val questions = topic.lessons.flatMap { it.questions }
    var questionIndex by rememberSaveable(topic.id) { mutableIntStateOf(0) }
    var selectedAnswer by rememberSaveable(topic.id) { mutableIntStateOf(-1) }
    var correctCount by rememberSaveable(topic.id) { mutableIntStateOf(0) }
    var answered by rememberSaveable(topic.id) { mutableStateOf(false) }
    var showResult by rememberSaveable(topic.id) { mutableStateOf(false) }

    if (questions.isEmpty()) {
        MissingContentScreen(onBack = onBack)
        return
    }
    if (showResult) {
        ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
            BackHeader("Practice complete", topic.title, onBack)
            Text("${correctCount * 100 / questions.size}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = NoorColors.Moss)
            Text("$correctCount of ${questions.size} correct", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            InfoCard(
                title = when {
                    correctCount * 100 / questions.size >= 80 -> "Strong work"
                    correctCount * 100 / questions.size >= 60 -> "Good start"
                    else -> "This topic is due for another review"
                },
                body = "Your result is saved on this phone. Missed topics are scheduled sooner; strong results are spaced out over time.",
                icon = Icons.Outlined.Refresh,
            )
            PrimaryButton(text = "Finish review", onClick = { onFinished(correctCount, questions.size) })
        }
        return
    }

    val question = questions[questionIndex]
    ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
        BackHeader(pack.subject, topic.title, onBack)
        Text("Question ${questionIndex + 1} of ${questions.size}", style = MaterialTheme.typography.bodyMedium, color = NoorColors.Ink.copy(alpha = 0.68f))
        LinearProgressIndicator(progress = (questionIndex + 1).toFloat() / questions.size, modifier = Modifier.fillMaxWidth(), color = NoorColors.Moss, trackColor = NoorColors.Mint)
        Text(question.prompt, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        question.options.forEachIndexed { index, option ->
            val isSelected = index == selectedAnswer
            val isCorrectAnswer = answered && index == question.correctOptionIndex
            val isWrongSelected = answered && isSelected && !isCorrectAnswer
            Surface(
                onClick = { if (!answered) selectedAnswer = index },
                enabled = !answered,
                color = if (isSelected || isCorrectAnswer) NoorColors.Mint else Color.White,
                shape = RoundedCornerShape(17.dp),
                border = BorderStroke(if (isSelected || isCorrectAnswer) 2.dp else 1.dp, if (isWrongSelected) Color(0xFFC65A4A) else if (isSelected || isCorrectAnswer) NoorColors.Moss else NoorColors.Mint),
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = if (isSelected || isCorrectAnswer) NoorColors.Moss else NoorColors.Canvas, shape = CircleShape, modifier = Modifier.size(34.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(('A' + index).toString(), color = if (isSelected || isCorrectAnswer) Color.White else NoorColors.MossDark, fontWeight = FontWeight.Bold) }
                    }
                    Text(option, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (isCorrectAnswer) Icon(Icons.Outlined.CheckCircle, contentDescription = "Correct answer", tint = NoorColors.Moss)
                }
            }
        }
        if (answered) {
            val correct = question.isCorrect(selectedAnswer)
            InfoCard(
                title = if (correct) "That's correct" else "Not quite",
                body = question.explanation,
                icon = if (correct) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
            )
        }
        PrimaryButton(
            text = when {
                !answered -> "Check answer"
                questionIndex < questions.lastIndex -> "Next question"
                else -> "See results"
            },
            enabled = answered || selectedAnswer >= 0,
            onClick = {
                if (!answered) {
                    if (question.isCorrect(selectedAnswer)) correctCount += 1
                    answered = true
                } else if (questionIndex < questions.lastIndex) {
                    questionIndex += 1
                    selectedAnswer = -1
                    answered = false
                } else {
                    showResult = true
                }
            },
        )
        Text("Practice is saved only after you finish the session.", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.62f))
    }
}

@Composable
private fun MissingContentScreen(onBack: () -> Unit) {
    ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
        BackHeader("Content unavailable", "", onBack)
        InfoCard("This lesson is not saved", "Return to Learn and download the subject pack while connected.", Icons.Outlined.Download)
        PrimaryButton("Back to Noor", onBack)
    }
}

@Composable
private fun ScreenColumn(
    modifier: Modifier = Modifier,
    safeDrawingInsets: Boolean = false,
    content: @Composable () -> Unit,
) {
    val insets = if (safeDrawingInsets) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier
    Column(
        modifier = modifier.fillMaxSize().then(insets).background(NoorColors.Canvas).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String, onSettings: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = NoorColors.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark)
        }
        if (onSettings != null) IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = NoorColors.Moss) }
    }
}

@Composable
private fun BackHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = NoorColors.MossDark) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.66f))
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, icon: ImageVector) {
    Surface(color = NoorColors.Mint, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = NoorColors.MossDark, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = NoorColors.MossDark)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = NoorColors.Ink.copy(alpha = 0.82f))
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NoorColors.Moss, contentColor = Color.White, disabledContainerColor = NoorColors.Mint, disabledContentColor = NoorColors.MossDark),
    ) {
        Text(text, modifier = Modifier.padding(vertical = 5.dp), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Outlined.ArrowForward, contentDescription = null)
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = NoorColors.Mint, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = NoorColors.MossDark)
            Text(label, style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun SettingLine(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = NoorColors.Ink.copy(alpha = 0.72f))
    }
}

@Composable
private fun NoorProgressBar(fraction: Float, description: String) {
    val progress = normalizedProgressFraction(fraction)
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(NoorColors.Mint)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            },
    ) {
        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxSize()
                    .background(NoorColors.Moss),
            )
        }
    }
}

internal fun countLabel(count: Int, singular: String, plural: String = "${singular}s"): String =
    "$count ${if (count == 1) singular else plural}"

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
