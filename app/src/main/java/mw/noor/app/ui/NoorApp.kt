package mw.noor.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Whatshot
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import android.content.Intent
import mw.noor.app.BuildConfig
import mw.noor.app.R
import mw.noor.app.data.ContentCatalogEntry
import mw.noor.app.data.ContentLesson
import mw.noor.app.data.LastLesson
import mw.noor.app.data.ContentPack
import mw.noor.app.data.ContentTopic
import mw.noor.app.data.NoorRepository
import mw.noor.app.data.packUpdateAvailable
import mw.noor.app.data.StudyStreak
import mw.noor.app.data.TopicProgress
import mw.noor.app.ui.lessonCompletionFraction
import mw.noor.app.ui.theme.NoorColors
import java.util.Locale
import kotlin.math.roundToInt

private enum class NoorTab(val title: String, val icon: ImageVector) {
    TODAY("Today", Icons.Outlined.Home),
    LEARN("Learn", Icons.Outlined.MenuBook),
    REVIEW("Review", Icons.Outlined.Refresh),
}

private data class HomeLessonTarget(
    val entry: ContentCatalogEntry,
    val topic: ContentTopic,
    val lesson: ContentLesson,
)

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
                    onOpenLearn = {
                        selectedTabName = NoorTab.LEARN.name
                        navigateBack()
                    },
                )
                "report" -> if (currentPack != null && currentTopic != null && currentLesson != null) {
                    ReportIssueScreen(
                        pack = currentPack,
                        topic = currentTopic,
                        lesson = currentLesson,
                        onBack = navigateBack,
                        onShareReport = { issueType, details ->
                            val reportText = buildString {
                                appendLine("Noor content issue report")
                                appendLine("Form ${currentPack.form} · ${currentPack.subject}")
                                appendLine("Topic: ${currentTopic.title}")
                                appendLine("Lesson: ${currentLesson.title}")
                                appendLine("Content version: ${currentPack.contentVersion}")
                                appendLine("Issue: $issueType")
                                if (details.isNotBlank()) appendLine("Details: $details")
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Noor content issue: ${currentLesson.title}")
                                putExtra(Intent.EXTRA_TEXT, reportText)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Share content report")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                    )
                } else {
                    MissingContentScreen(onBack = navigateBack)
                }
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
                                } catch (error: Exception) {
                                    downloadError = error.message ?: "Download failed. Check your connection and try again."
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
                            repository.saveLastLesson(last)
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
                        onReportContent = { navigateTo("report") },
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
                            repository.recordStudyActivity()
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
                        repository.saveLastLesson(last)
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
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).background(NoorColors.Canvas).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("noor", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = NoorColors.Moss)
        Text(
            "Small steps.\nBig knowledge.",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = NoorColors.MossDark,
        )
        Text(
            "Quality study notes and practice for Form 3 students in Malawi.",
            style = MaterialTheme.typography.bodyLarge,
            color = NoorColors.Ink,
            modifier = Modifier.fillMaxWidth(0.9f),
        )
        Image(
            painter = painterResource(R.drawable.onboarding_student),
            contentDescription = "A student writing in an exercise book",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().heightIn(max = 270.dp),
        )
        DraftStatusBadge()
        PrimaryButton(text = "Get started", onClick = onContinue, minHeight = 52.dp)
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
            NavigationBar(modifier = Modifier.heightIn(min = 80.dp), containerColor = Color.White) {
                NoorTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { onSelectTab(tab.name) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = {
                            Text(
                                tab.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NoorColors.Moss,
                            selectedTextColor = NoorColors.Moss,
                            unselectedIconColor = NoorColors.MossDark,
                            unselectedTextColor = NoorColors.MossDark,
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
                dataVersion = dataVersion,
                onOpenSettings = onOpenSettings,
                onSelectTab = onSelectTab,
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
        }
    }
}

@Composable
private fun TodayScreen(
    modifier: Modifier,
    entries: List<ContentCatalogEntry>,
    repository: NoorRepository,
    dataVersion: Int,
    onOpenSettings: () -> Unit,
    onSelectTab: (String) -> Unit,
    onContinueLesson: (mw.noor.app.data.LastLesson) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showStreakInfo by rememberSaveable { mutableStateOf(false) }
    val lessonTargets = remember(entries, dataVersion) {
        entries.flatMap { entry ->
            repository.loadPack(entry)?.topics?.flatMap { topic ->
                topic.lessons.map { lesson -> HomeLessonTarget(entry, topic, lesson) }
            }.orEmpty()
        }
    }
    val query = searchQuery.trim().lowercase(Locale.ROOT)
    val searchResults = if (query.isBlank()) emptyList() else lessonTargets.filter { target ->
        listOf(target.entry.subject, target.topic.title, target.lesson.title).any { it.lowercase(Locale.ROOT).contains(query) }
    }.take(6)
    val lastLesson = repository.lastLesson()
    val continueTarget = lastLesson?.let { last ->
        lessonTargets.firstOrNull {
            it.entry.id == last.packId && it.topic.id == last.topicId && it.lesson.id == last.lessonId
        }
    }
    val startTarget = continueTarget ?: lessonTargets.firstOrNull()
    val pathTargets = startTarget?.let { anchor ->
        val subjectLessons = lessonTargets.filter { it.entry.id == anchor.entry.id }
        val startAt = if (continueTarget == null) 0 else subjectLessons.indexOfFirst { it.lesson.id == anchor.lesson.id }.coerceAtLeast(0)
        subjectLessons.drop(startAt).take(3)
    }.orEmpty()
    val completedLessons = remember(dataVersion) { repository.completedLessonIds() }
    val streak = repository.studyStreak()

    ScreenColumn(modifier, contentSpacing = 12.dp) {
        ScreenHeader(
            title = "noor",
            subtitle = "Form 3 · Biology & Physics",
            onSettings = onOpenSettings,
            largeTitle = true,
            streak = streak,
            onStreakClick = { showStreakInfo = true },
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search lessons, topics…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = NoorColors.MossDark) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = NoorColors.Moss,
                unfocusedBorderColor = NoorColors.Mint,
            ),
        )

        if (query.isNotBlank()) {
            Text("Search results", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (searchResults.isEmpty()) {
                Text("No saved lessons match.", style = MaterialTheme.typography.bodyMedium, color = NoorColors.MossDark)
            } else {
                searchResults.forEach { target ->
                    LessonTargetCard(target, completed = target.lesson.id in completedLessons) {
                        onContinueLesson(LastLesson(target.entry.id, target.topic.id, target.lesson.id))
                    }
                }
            }
        } else {
            startTarget?.let { target ->
                val completedInTopic = target.topic.lessons.count { it.id in completedLessons }
                ContinueLearningCard(
                    target = target,
                    isContinue = continueTarget != null,
                    completedLessons = completedInTopic,
                    totalLessons = target.topic.lessons.size,
                    onClick = { onContinueLesson(LastLesson(target.entry.id, target.topic.id, target.lesson.id)) },
                )
            } ?: StartLearningCard { onSelectTab(NoorTab.LEARN.name) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Today's path", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { onSelectTab(NoorTab.LEARN.name) }, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text("See all")
                }
            }
            if (pathTargets.isEmpty()) {
                InfoCard(
                    title = "Save a subject to begin",
                    body = "Open Learn to save Biology or Physics for offline study.",
                    icon = Icons.Outlined.Download,
                )
            } else {
                pathTargets.forEachIndexed { index, target ->
                    LessonPathCard(
                        number = index + 1,
                        target = target,
                        completed = target.lesson.id in completedLessons,
                    ) {
                        onContinueLesson(LastLesson(target.entry.id, target.topic.id, target.lesson.id))
                    }
                }
            }
        }
    }
    if (showStreakInfo) {
        val streakMessage = when {
            streak.studiedToday -> "Today’s study activity is recorded on this phone. Complete a lesson or finish a quiz on another day to continue your streak."
            streak.days > 0 -> "Your streak is active. Complete a lesson or finish a quiz today to keep it going."
            else -> "Complete a lesson or finish a quiz to start a study streak. Activity is stored on this phone."
        }
        AlertDialog(
            onDismissRequest = { showStreakInfo = false },
            title = { Text(if (streak.days > 0) "${countLabel(streak.days, "day")} streak" else "Study streak") },
            text = { Text(streakMessage) },
            confirmButton = { TextButton(onClick = { showStreakInfo = false }) { Text("Got it") } },
        )
    }
}

@Composable
private fun ContinueLearningCard(target: HomeLessonTarget, isContinue: Boolean, completedLessons: Int, totalLessons: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = NoorColors.MossDark),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (isContinue) "CONTINUE LEARNING" else "START LEARNING", style = MaterialTheme.typography.labelSmall, color = NoorColors.Mint)
                Text("${target.entry.subject} · ${target.topic.title}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                Text(target.lesson.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 2)
                Text(
                    "${completedLessons.coerceIn(0, totalLessons.coerceAtLeast(0))} of $totalLessons ${if (totalLessons == 1) "lesson" else "lessons"} complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = NoorColors.Mint,
                )
                LinearProgressIndicator(
                    progress = { lessonCompletionFraction(completedLessons, totalLessons) },
                    modifier = Modifier.fillMaxWidth(),
                    color = NoorColors.Mint,
                    trackColor = Color.White.copy(alpha = 0.22f),
                )
            }
            Surface(color = NoorColors.Mint, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ArrowForward, contentDescription = "Open lesson", tint = NoorColors.MossDark)
                }
            }
        }
    }
}

@Composable
private fun StartLearningCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = NoorColors.MossDark),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("START LEARNING", style = MaterialTheme.typography.labelSmall, color = NoorColors.Mint)
                Text("Choose a subject to begin", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text("Save Biology or Physics for offline study.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.88f))
            }
            Surface(color = NoorColors.Mint, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ArrowForward, contentDescription = "Browse subjects", tint = NoorColors.MossDark)
                }
            }
        }
    }
}

@Composable
private fun LessonPathCard(number: Int, target: HomeLessonTarget, completed: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NoorColors.Mint),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(color = if (completed) NoorColors.Mint else NoorColors.Canvas, shape = CircleShape, modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (completed) Icon(Icons.Outlined.Check, contentDescription = "Completed", tint = NoorColors.Moss)
                    else Text(number.toString(), color = NoorColors.MossDark, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(target.topic.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${target.entry.subject} · ${countLabel(target.lesson.readTimeMinutes, "minute")}", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = "Open ${target.topic.title}", tint = NoorColors.Moss)
        }
    }
}

@Composable
private fun LessonTargetCard(target: HomeLessonTarget, completed: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NoorColors.Mint),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (completed) Icons.Outlined.CheckCircle else Icons.Outlined.MenuBook, contentDescription = null, tint = NoorColors.Moss)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(target.lesson.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${target.entry.subject} · ${target.topic.title}", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = "Open lesson", tint = NoorColors.Moss)
        }
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
    ScreenColumn(modifier) {
        ScreenHeader("Learn", "Form 3 · Biology & Physics", onOpenSettings)
        val allSaved = entries.isNotEmpty() && entries.all { repository.loadPack(it) != null }
        InfoCard(
            title = if (allSaved) "Subjects saved offline" else "Save subjects for offline study",
            body = if (allSaved) "Study anytime, even without internet." else "Download a subject pack once to study offline.",
            icon = if (allSaved) Icons.Outlined.CheckCircle else Icons.Outlined.Download,
        )
        if (entries.any { it.curriculumStatus.contains("draft", ignoreCase = true) }) {
            InfoCard(
                title = "Draft content",
                body = "Lessons are awaiting local teacher review.",
                icon = Icons.Outlined.Info,
            )
        }
        Text("Your subjects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        entries.forEach { entry ->
            PackCard(entry, downloaded = repository.loadPack(entry) != null, onClick = { onOpenPack(entry) })
        }
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
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    entry.subject,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Form ${entry.form} · ${countLabel(entry.topicCount, "topic")}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = NoorColors.Ink.copy(alpha = 0.7f),
                )
                Text(
                    if (downloaded) "Saved offline" else formatBytes(entry.sizeBytes),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                    color = NoorColors.MossDark,
                )
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
    val storageStatus = repository.storageStatus(entry)
    val updateAvailable = pack != null && packUpdateAvailable(pack.contentVersion, entry.contentVersion)
    ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
        BackHeader(title = entry.subject, subtitle = "Form ${entry.form}", onBack = onBack)
        StatusBadge(entry.curriculumStatus)
        if (pack == null) {
            Surface(color = NoorColors.MossDark, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${countLabel(entry.topicCount, "topic")} · about ${formatBytes(entry.sizeBytes)}", color = NoorColors.Mint)
                    if (downloadingPackId == entry.id) {
                        LinearProgressIndicator(progress = { downloadPercent / 100f }, modifier = Modifier.fillMaxWidth(), color = NoorColors.Mint, trackColor = Color.White.copy(alpha = 0.24f))
                        Text(if (downloadPercent == 0) "Connecting…" else "Downloading $downloadPercent%", color = Color.White)
                    } else {
                        Button(
                            onClick = { onDownload(entry) },
                            enabled = storageStatus.canDownload,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NoorColors.Mint, contentColor = NoorColors.MossDark),
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (downloadError.isNotBlank()) "Try again" else "Download pack")
                        }
                    }
                }
            }
            if (!storageStatus.canDownload) {
                InfoCard(
                    title = "Not enough storage",
                    body = "Free up space to download this pack safely. You need about ${formatBytes(storageStatus.requiredBytes)} free.",
                    icon = Icons.Outlined.Info,
                )
            }
            if (downloadError.isNotBlank()) Text(downloadError, style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark)
        } else {
            if (updateAvailable) {
                InfoCard(
                    title = "Version ${entry.contentVersion} available",
                    body = "Your saved Version ${pack.contentVersion} stays usable while the update downloads.",
                    icon = Icons.Outlined.Download,
                )
                if (downloadingPackId == entry.id) {
                    LinearProgressIndicator(
                        progress = { downloadPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = NoorColors.Moss,
                        trackColor = NoorColors.Mint,
                    )
                    Text(if (downloadPercent == 0) "Connecting…" else "Updating ${downloadPercent}%", style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark)
                } else {
                    Button(
                        onClick = { onDownload(entry) },
                        enabled = storageStatus.canDownload,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.buttonColors(containerColor = NoorColors.Moss, contentColor = Color.White),
                    ) { Text(if (downloadError.isNotBlank()) "Try update again" else "Update pack · ${formatBytes(entry.sizeBytes)}") }
                }
                if (!storageStatus.canDownload) {
                    Text("Not enough storage. Free up space before updating; the saved version will remain available.", style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark)
                }
                if (downloadError.isNotBlank()) Text(downloadError, style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark)
            } else {
                Text("Saved offline · Version ${pack.contentVersion}", style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark)
            }
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
    val completedCount = topic.lessons.count { it.id in completedLessons }
    val totalLessons = topic.lessons.size
    val completion = lessonCompletionFraction(completedCount, totalLessons)
    val lessonLabel = if (totalLessons == 1) "lesson" else "lessons"
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, NoorColors.Mint)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(17.dp))
                .drawBehind {
                    if (completion > 0f) {
                        drawRect(
                            color = NoorColors.Mint,
                            size = androidx.compose.ui.geometry.Size(size.width * completion, size.height),
                        )
                    }
                }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "$completedCount/$totalLessons $lessonLabel complete",
                style = MaterialTheme.typography.bodySmall,
                color = NoorColors.MossDark,
            )
            topic.lessons.forEach { lesson ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = { onOpenLesson(lesson) })
                        .heightIn(min = 48.dp)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (lesson.id in completedLessons) Icons.Outlined.CheckCircle else Icons.Outlined.MenuBook,
                        contentDescription = if (lesson.id in completedLessons) "Lesson completed" else null,
                        tint = NoorColors.Moss,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(lesson.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(countLabel(lesson.readTimeMinutes, "minute"), style = MaterialTheme.typography.labelSmall, color = NoorColors.Ink.copy(alpha = 0.68f))
                    Icon(Icons.Outlined.ArrowForward, contentDescription = "Open ${lesson.title}", tint = NoorColors.Moss, modifier = Modifier.size(18.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    progress?.let {
                        "Last quiz · ${it.lastScorePercent}% · ${if (it.nextReviewAtMillis <= System.currentTimeMillis()) "Review due" else "Scheduled"}"
                    } ?: "Not practised",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = NoorColors.MossDark,
                )
                TextButton(onClick = onStartQuiz, modifier = Modifier.heightIn(min = 48.dp)) { Text("Practise topic") }
            }
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
    onReportContent: () -> Unit,
    onProgressChanged: () -> Unit,
) {
    var completed by remember(lesson.id) { mutableStateOf(lesson.id in repository.completedLessonIds()) }
    val lessonPosition = (topic.lessons.indexOfFirst { it.id == lesson.id } + 1).coerceAtLeast(0)
    val lessonTotal = topic.lessons.size.coerceAtLeast(1)
    ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
        BackHeader(title = pack.subject, subtitle = "${topic.title} · Lesson $lessonPosition of $lessonTotal", onBack = onBack)
        LinearProgressIndicator(
            progress = { (lessonPosition.toFloat() / lessonTotal).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = NoorColors.Moss,
            trackColor = NoorColors.Mint,
        )
        Text(lesson.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(countLabel(lesson.readTimeMinutes, "minute"), style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
            Text("Form ${pack.form}", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
        }
        Surface(color = NoorColors.Mint, shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = NoorColors.MossDark)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Draft content", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = NoorColors.MossDark)
                    Text(pack.curriculumStatus, style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark)
                }
            }
        }
        lesson.sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(section.heading, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = NoorColors.MossDark)
                Text(section.body, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal, lineHeight = 24.sp))
            }
        }
        if (lesson.id == "bio_cells_intro") CellStructureDiagram()
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
                    repository.recordStudyActivity()
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
        TextButton(onClick = onReportContent, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Report a content issue")
        }
    }
}

@Composable
private fun ReportIssueScreen(
    pack: ContentPack,
    topic: ContentTopic,
    lesson: ContentLesson,
    onBack: () -> Unit,
    onShareReport: (issueType: String, details: String) -> Unit,
) {
    val issueTypes = listOf("Incorrect information", "Unclear explanation", "Question or answer", "Other")
    var selectedIssue by rememberSaveable(lesson.id) { mutableStateOf("") }
    var details by rememberSaveable(lesson.id) { mutableStateOf("") }

    ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
        BackHeader("Report content issue", lesson.title, onBack)
        Text("What should be reviewed?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        issueTypes.forEach { issueType ->
            val isSelected = selectedIssue == issueType
            Card(
                onClick = { selectedIssue = issueType },
                modifier = Modifier.semantics { selected = isSelected },
                colors = CardDefaults.cardColors(containerColor = if (isSelected) NoorColors.Mint else Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) NoorColors.Moss else NoorColors.Mint),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(issueType, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (isSelected) Icon(Icons.Outlined.CheckCircle, contentDescription = "Selected", tint = NoorColors.Moss)
                }
            }
        }
        OutlinedTextField(
            value = details,
            onValueChange = { details = it.take(400) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Details (optional)") },
            minLines = 3,
            maxLines = 5,
        )
        Text("Do not include your name or contact details.", style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark)
        PrimaryButton(
            text = "Share report",
            enabled = selectedIssue.isNotBlank(),
            onClick = { onShareReport(selectedIssue, details.trim()) },
        )
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
    val otherTopics = downloadedPacks.flatMap { pack ->
        val entry = entries.firstOrNull { it.id == pack.id } ?: return@flatMap emptyList()
        pack.topics.filter { it.id !in dueIds }.map { entry to it }
    }
    ScreenColumn(modifier) {
        ScreenHeader("Review", "Your private practice queue")
        if (downloadedPacks.isEmpty()) {
            Text("No saved subjects", style = MaterialTheme.typography.titleMedium, color = NoorColors.MossDark)
        } else {
            Text("Due today · ${countLabel(dueTopics.size, "topic")}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (dueTopics.isEmpty()) {
                Text("Nothing due", style = MaterialTheme.typography.bodyMedium, color = NoorColors.MossDark)
            } else {
                dueTopics.forEach { (entry, topic) -> DueReviewCard(entry, topic, repository.topicProgress(topic.id), onStartReview) }
            }
            if (otherTopics.isNotEmpty()) {
                Text("Other saved topics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                otherTopics.forEach { (entry, topic) -> SavedTopicRow(entry, topic, onStartReview) }
            }
        }
    }
}

@Composable
private fun DueReviewCard(
    entry: ContentCatalogEntry,
    topic: ContentTopic,
    progress: TopicProgress?,
    onStartReview: (ContentCatalogEntry, ContentTopic) -> Unit,
) {
    Card(
        onClick = { onStartReview(entry, topic) },
        colors = CardDefaults.cardColors(containerColor = NoorColors.Mint),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NoorColors.Moss),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${entry.subject} · Form ${entry.form}", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
                    if (progress != null) Text("Last score ${progress.lastScorePercent}%", style = MaterialTheme.typography.labelSmall, color = NoorColors.MossDark)
                }
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = NoorColors.Moss)
            }
            Button(
                onClick = { onStartReview(entry, topic) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(containerColor = NoorColors.Moss, contentColor = Color.White),
            ) {
                Text("Review now", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SavedTopicRow(
    entry: ContentCatalogEntry,
    topic: ContentTopic,
    onStartReview: (ContentCatalogEntry, ContentTopic) -> Unit,
) {
    Card(
        onClick = { onStartReview(entry, topic) },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NoorColors.Mint),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(topic.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${entry.subject} · Form ${entry.form}", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = "Practise ${topic.title}", tint = NoorColors.Moss)
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
    onOpenLearn: () -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val savedPacks = entries.mapNotNull(repository::loadPack)
    val summary = repository.progressSummary(savedPacks)
    val accuracy = if (summary.answeredQuestions == 0) "—" else "${summary.correctAnswers * 100 / summary.answeredQuestions}%"
    ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
        BackHeader("Profile", "", onBack)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, NoorColors.Mint)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = NoorColors.Moss, shape = CircleShape, modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = Color.White)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Student", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Form ${repository.selectedForm()}", style = MaterialTheme.typography.bodySmall, color = NoorColors.MossDark)
                }
            }
        }
        Text("Learning stats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("${summary.completedLessons}/${summary.totalLessons}", "lessons complete", Modifier.weight(1f))
            MetricCard(accuracy, "practice accuracy", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(summary.quizzesCompleted.toString(), countLabel(summary.quizzesCompleted, "practice session"), Modifier.weight(1f))
            MetricCard(summary.dueTopicIds.size.toString(), "${countLabel(summary.dueTopicIds.size, "topic")} due", Modifier.weight(1f))
        }
        Card(
            onClick = onOpenLearn,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NoorColors.Mint),
        ) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Download, contentDescription = null, tint = NoorColors.Moss)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Download subjects", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Manage offline content", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
                }
                Icon(Icons.Outlined.ArrowForward, contentDescription = "Open subjects", tint = NoorColors.Moss)
            }
        }
        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "Progress stays on this phone; it is not synced and is deleted if the app is uninstalled or its data is cleared.",
            style = MaterialTheme.typography.bodySmall,
            color = NoorColors.MossDark,
        )
        SettingLine("Form", "Form ${repository.selectedForm()}")
        SettingLine("Content language", if (repository.language() == "en") "English" else repository.language())
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Text size · ${(textScale * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Slider(
                value = textScale,
                onValueChange = onTextScaleChange,
                valueRange = 0.9f..1.3f,
                modifier = Modifier.semantics {
                    contentDescription = "Text size control"
                    stateDescription = "${(textScale * 100).roundToInt()} percent"
                },
            )
        }
        Text("Saved lesson packs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        val storedBytes = repository.downloadedBytes(entries)
        Text(formatBytes(storedBytes), style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
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
            text = { Text("Progress and the study streak will be deleted. Saved lesson packs will stay.") },
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
            StatusBadge(
                text = when {
                    correctCount * 100 / questions.size >= 80 -> "Strong work"
                    correctCount * 100 / questions.size >= 60 -> "Good start"
                    else -> "Review recommended"
                },
            )
            PrimaryButton(text = "Finish review", onClick = { onFinished(correctCount, questions.size) })
        }
        return
    }

    val question = questions[questionIndex]
    ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
        BackHeader("Quick Check", "${pack.subject} · ${topic.title}", onBack)
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
    }
}

@Composable
private fun MissingContentScreen(onBack: () -> Unit) {
    ScreenColumn(Modifier.fillMaxSize(), safeDrawingInsets = true) {
        BackHeader("Content unavailable", "", onBack)
        StatusBadge("Lesson not saved")
        PrimaryButton("Back to Noor", onBack)
    }
}

@Composable
private fun ScreenColumn(
    modifier: Modifier = Modifier,
    safeDrawingInsets: Boolean = false,
    contentSpacing: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    val insets = if (safeDrawingInsets) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier
    Column(
        modifier = modifier.fillMaxSize().then(insets).background(NoorColors.Canvas).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(contentSpacing),
    ) { content() }
}

@Composable
private fun ScreenHeader(
    title: String,
    subtitle: String,
    onSettings: (() -> Unit)? = null,
    largeTitle: Boolean = false,
    streak: StudyStreak? = null,
    onStreakClick: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = if (largeTitle) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = NoorColors.Ink,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
                    color = NoorColors.MossDark,
                )
            }
        }
        if (streak != null && onStreakClick != null) {
            TextButton(
                onClick = onStreakClick,
                modifier = Modifier.heightIn(min = 48.dp).semantics {
                    contentDescription = "Study streak: ${countLabel(streak.days, "day")}${if (streak.studiedToday) ", studied today" else ""}"
                },
            ) {
                Icon(Icons.Outlined.Whatshot, contentDescription = null, tint = NoorColors.Moss)
                Spacer(Modifier.width(4.dp))
                Text(streak.days.toString(), color = NoorColors.MossDark, fontWeight = FontWeight.SemiBold)
            }
        }
        if (onSettings != null) {
            IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile and settings", tint = NoorColors.Moss, modifier = Modifier.size(24.dp))
            }
        }
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
private fun DraftStatusBadge() {
    StatusBadge("Draft · teacher review")
}

@Composable
private fun StatusBadge(text: String) {
    Surface(color = NoorColors.Mint, shape = RoundedCornerShape(50)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = NoorColors.MossDark,
        )
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, minHeight: Dp = 52.dp) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight),
        shape = RoundedCornerShape(percent = 50),
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

internal fun countLabel(count: Int, singular: String, plural: String = "${singular}s"): String =
    "$count ${if (count == 1) singular else plural}"

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
