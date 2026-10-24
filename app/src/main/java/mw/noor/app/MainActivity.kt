package mw.noor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mw.noor.app.ui.theme.NoorColors
import mw.noor.app.ui.theme.NoorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoorTheme {
                NoorApp()
            }
        }
    }
}

private enum class NoorTab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Outlined.Home),
    LEARN("Learn", Icons.Outlined.MenuBook),
    REVIEW("Review", Icons.Outlined.Refresh),
    PROGRESS("Progress", Icons.Outlined.BarChart),
}

private data class Subject(
    val name: String,
    val topicCount: Int,
    val progress: Float,
    val icon: ImageVector,
)

private val sampleSubjects = listOf(
    Subject("Biology", 8, 0.50f, Icons.Outlined.Science),
    Subject("Mathematics", 12, 0.67f, Icons.Outlined.Calculate),
    Subject("Chemistry", 6, 0.33f, Icons.Outlined.Science),
    Subject("English", 7, 0.71f, Icons.Outlined.MenuBook),
    Subject("Geography", 5, 0.20f, Icons.Outlined.Public),
)

@Composable
private fun NoorApp() {
    var selectedTabName by rememberSaveable { mutableStateOf(NoorTab.TODAY.name) }
    var quizOpen by rememberSaveable { mutableStateOf(false) }
    val selectedTab = NoorTab.valueOf(selectedTabName)

    if (quizOpen) {
        QuizScreen(
            onBack = { quizOpen = false },
            onFinished = {
                quizOpen = false
                selectedTabName = NoorTab.REVIEW.name
            },
        )
    } else {
        Scaffold(
            containerColor = NoorColors.Canvas,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NoorTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTabName = tab.name },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NoorColors.Moss,
                                selectedTextColor = NoorColors.Moss,
                                indicatorColor = NoorColors.Mint,
                                unselectedIconColor = NoorColors.Ink.copy(alpha = 0.58f),
                                unselectedTextColor = NoorColors.Ink.copy(alpha = 0.66f),
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            when (selectedTab) {
                NoorTab.TODAY -> TodayScreen(
                    modifier = Modifier.padding(padding),
                    onStartQuiz = { quizOpen = true },
                    onOpenLearn = { selectedTabName = NoorTab.LEARN.name },
                )
                NoorTab.LEARN -> LearnScreen(Modifier.padding(padding))
                NoorTab.REVIEW -> ReviewScreen(
                    modifier = Modifier.padding(padding),
                    onStartQuiz = { quizOpen = true },
                )
                NoorTab.PROGRESS -> ProgressScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun ScreenColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NoorColors.Canvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        content()
    }
}

@Composable
private fun TodayScreen(
    modifier: Modifier = Modifier,
    onStartQuiz: () -> Unit,
    onOpenLearn: () -> Unit,
) {
    ScreenColumn(modifier) {
        Text("Noor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Good morning, Thoko", style = MaterialTheme.typography.titleMedium, color = NoorColors.Moss)
        Text(
            "Small steps.\nStrong progress.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = NoorColors.Ink,
        )

        Surface(color = NoorColors.MossDark, shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("CONTINUE LEARNING", style = MaterialTheme.typography.labelMedium, color = NoorColors.Mint)
                Text("Form 2 · Biology", style = MaterialTheme.typography.bodyMedium, color = NoorColors.Mint)
                Text("Cell structure", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text("8 min · Lesson 2 of 5", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.86f))
                Button(
                    onClick = onStartQuiz,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NoorColors.Sun, contentColor = NoorColors.Ink),
                ) {
                    Text("Continue learning", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.ArrowForward, contentDescription = null)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Explore subjects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("See all", color = NoorColors.Moss, modifier = Modifier.clickable(onClick = onOpenLearn))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            sampleSubjects.take(3).forEach { subject ->
                SubjectTile(subject = subject, modifier = Modifier.weight(1f))
            }
        }
        InfoCard(
            icon = Icons.Outlined.Download,
            title = "Works offline",
            body = "Download lessons and learn when you are away from a connection.",
        )
    }
}

@Composable
private fun SubjectTile(subject: Subject, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (subject.name == "Mathematics") NoorColors.Mint else NoorColors.Moss,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(subject.icon, contentDescription = null, tint = if (subject.name == "Mathematics") NoorColors.MossDark else Color.White)
            Text(
                subject.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (subject.name == "Mathematics") NoorColors.Ink else Color.White,
            )
            Text("Lessons · Practice", style = MaterialTheme.typography.labelSmall, color = if (subject.name == "Mathematics") NoorColors.Ink.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.78f))
        }
    }
}

@Composable
private fun LearnScreen(modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        Text("Learn", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Surface(color = NoorColors.Mint, shape = RoundedCornerShape(16.dp)) {
            Text("Form 2  ▾", modifier = Modifier.fillMaxWidth().padding(16.dp), fontWeight = FontWeight.SemiBold, color = NoorColors.MossDark)
        }
        Text("Choose a subject to continue.", style = MaterialTheme.typography.titleMedium, color = NoorColors.Ink.copy(alpha = 0.75f))
        sampleSubjects.forEach { subject -> SubjectRow(subject) }
        InfoCard(Icons.Outlined.Download, "Saved lessons work offline", "Download lessons to learn later.")
    }
}

@Composable
private fun SubjectRow(subject: Subject) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NoorColors.Mint),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(color = NoorColors.Moss, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(subject.icon, contentDescription = null, tint = Color.White)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${subject.topicCount} topics", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.68f))
                LinearProgressIndicator(
                    progress = subject.progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = NoorColors.Moss,
                    trackColor = NoorColors.Mint,
                )
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = NoorColors.Moss)
        }
    }
}

@Composable
private fun ReviewScreen(modifier: Modifier = Modifier, onStartQuiz: () -> Unit) {
    ScreenColumn(modifier) {
        Text("Review", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text("Small reviews help learning stick.", color = NoorColors.Ink.copy(alpha = 0.72f))
        InfoCard(Icons.Outlined.Refresh, "Due today", "2 topics · 8 questions")
        ReviewTopicCard("Cell structure", "Biology · 5 questions", onStartQuiz)
        ReviewTopicCard("Fractions", "Mathematics · 3 questions", onStartQuiz)
        Text("Review a little, often.", color = NoorColors.Moss, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ReviewTopicCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NoorColors.Mint),
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.7f))
            }
            Icon(Icons.Outlined.ArrowForward, contentDescription = "Start review", tint = NoorColors.Moss)
        }
    }
}

@Composable
private fun ProgressScreen(modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        Text("Your progress", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text("Small steps. Bright futures.", color = NoorColors.Ink.copy(alpha = 0.72f))
        Surface(color = NoorColors.Mint, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("This week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("4 of 7 days", color = NoorColors.MossDark)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = if (index < 4) NoorColors.Moss else Color.White, shape = CircleShape, modifier = Modifier.size(34.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (index < 4) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White)
                                }
                            }
                            Text(day, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("4", "lessons completed", Modifier.weight(1f), dark = true)
            MetricCard("78%", "practice score", Modifier.weight(1f), dark = false)
        }
        InfoCard(Icons.Outlined.Refresh, "Ready to review", "Cell division · 4 questions")
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier, dark: Boolean) {
    Surface(
        modifier = modifier,
        color = if (dark) NoorColors.MossDark else NoorColors.Mint,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = if (dark) Color.White else NoorColors.Ink)
            Text(label, style = MaterialTheme.typography.bodySmall, color = if (dark) Color.White.copy(alpha = 0.85f) else NoorColors.Ink.copy(alpha = 0.74f))
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String) {
    Surface(color = NoorColors.Mint, shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = NoorColors.MossDark, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(body, style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.72f))
            }
        }
    }
}

@Composable
private fun QuizScreen(onBack: () -> Unit, onFinished: () -> Unit) {
    val question = QuizQuestion(
        prompt = "What is the main function of the cell membrane?",
        options = listOf(
            "Produces energy for the cell",
            "Controls what enters and leaves the cell",
            "Stores genetic information",
            "Builds proteins",
        ),
        correctOptionIndex = 1,
    )
    var selectedOption by rememberSaveable { mutableIntStateOf(-1) }
    var answered by rememberSaveable { mutableStateOf(false) }
    val isCorrect = answered && question.isCorrect(selectedOption)

    Column(
        modifier = Modifier.fillMaxSize().background(NoorColors.Canvas).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "Back") }
            Column {
                Text("Cell structure", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Question 2 of 5", style = MaterialTheme.typography.bodySmall, color = NoorColors.Ink.copy(alpha = 0.66f))
            }
        }
        LinearProgressIndicator(progress = 0.4f, modifier = Modifier.fillMaxWidth(), color = NoorColors.Moss, trackColor = NoorColors.Mint)
        Spacer(Modifier.height(8.dp))
        Text(question.prompt, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        question.options.forEachIndexed { index, option ->
            val selected = selectedOption == index
            val correctChoice = answered && index == question.correctOptionIndex
            val wrongChoice = answered && selected && !isCorrect
            Surface(
                onClick = { if (!answered) selectedOption = index },
                enabled = !answered,
                color = when {
                    correctChoice -> NoorColors.Mint
                    selected -> NoorColors.Mint
                    else -> Color.White
                },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(if (selected || correctChoice) 2.dp else 1.dp, if (wrongChoice) NoorColors.Sun else if (selected || correctChoice) NoorColors.Moss else NoorColors.Mint),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(color = if (selected || correctChoice) NoorColors.Moss else NoorColors.Mint, shape = CircleShape, modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(('A' + index).toString(), color = if (selected || correctChoice) Color.White else NoorColors.MossDark, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(option, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (correctChoice) Icon(Icons.Outlined.CheckCircle, contentDescription = "Correct answer", tint = NoorColors.Moss)
                }
            }
        }
        if (answered) {
            InfoCard(
                icon = if (isCorrect) Icons.Outlined.Lightbulb else Icons.Outlined.Refresh,
                title = if (isCorrect) "That’s correct!" else "Not quite yet",
                body = if (isCorrect) {
                    "The cell membrane controls what enters and leaves the cell. It helps keep the cell balanced."
                } else {
                    "The correct answer is: ${question.options[question.correctOptionIndex]}. Try another review soon."
                },
            )
        }
        Button(
            onClick = { if (answered) onFinished() else answered = true },
            enabled = answered || selectedOption >= 0,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NoorColors.Moss, disabledContainerColor = NoorColors.Mint),
        ) {
            Text(if (answered) "Finish review" else "Check answer", modifier = Modifier.padding(vertical = 6.dp))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.ArrowForward, contentDescription = null)
        }
    }
}
