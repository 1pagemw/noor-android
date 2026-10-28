package mw.noor.app.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/** WorkManager treats CONNECTED as either Wi-Fi or mobile data. */
object LessonDownloadWork {
    private const val PREFETCH_WORK = "noor.lesson.prefetch.next"
    private const val LESSON_ID_INPUT = "lesson_id"
    private const val PREFETCH_INPUT = "is_prefetch"
    private const val PREFETCH_TAG = "noor.lesson.prefetch"

    fun workName(lessonId: String): String = "noor.lesson.$lessonId"
    fun lessonTag(lessonId: String): String = "noor.lesson.download.$lessonId"

    fun observe(context: Context, lessonId: String): Flow<List<WorkInfo>> =
        WorkManager.getInstance(context.applicationContext).getWorkInfosByTagFlow(lessonTag(lessonId))

    fun enqueueRequested(context: Context, entry: LessonIndexEntry) {
        val manager = WorkManager.getInstance(context.applicationContext)
        // A learner opening a lesson takes priority over any different queued prefetch.
        manager.cancelUniqueWork(PREFETCH_WORK)
        manager.enqueueUniqueWork(
            workName(entry.lessonId),
            ExistingWorkPolicy.REPLACE,
            request(entry, isPrefetch = false),
        )
    }

    internal fun enqueuePrefetch(context: Context, entry: LessonIndexEntry) {
        val request = request(entry, isPrefetch = true)
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(PREFETCH_WORK, ExistingWorkPolicy.KEEP, request)
    }

    private fun request(entry: LessonIndexEntry, isPrefetch: Boolean) =
        OneTimeWorkRequestBuilder<LessonDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInputData(
                workDataOf(
                    LESSON_ID_INPUT to entry.lessonId,
                    PREFETCH_INPUT to isPrefetch,
                ),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(lessonTag(entry.lessonId))
            .addTag(if (isPrefetch) PREFETCH_TAG else "noor.lesson.requested")
            .build()

    internal fun isPrefetch(parameters: WorkerParameters): Boolean =
        parameters.inputData.getBoolean(PREFETCH_INPUT, false)

    internal fun lessonId(parameters: WorkerParameters): String? =
        parameters.inputData.getString(LESSON_ID_INPUT)
}

class LessonDownloadWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val lessonId = LessonDownloadWork.lessonId(workerParameters) ?: return Result.failure()
        val isPrefetch = LessonDownloadWork.isPrefetch(workerParameters)
        val repository = NoorRepository(applicationContext)
        val entry = repository.lessonEntry(lessonId) ?: return Result.failure()

        if (repository.loadLesson(entry) != null) {
            if (!isPrefetch) enqueueNextLesson(repository, entry)
            return Result.success()
        }

        return try {
            repository.downloadLesson(entry) { percent ->
                setProgress(workDataOf("percent" to percent))
            }
            if (!isPrefetch) enqueueNextLesson(repository, entry)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (isRetryableConnectionError(error) && runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to (error.message ?: "Download failed. Try again.")))
            }
        }
    }

    private fun enqueueNextLesson(repository: NoorRepository, completed: LessonIndexEntry) {
        val index = repository.lessonCatalog()
        val next = index.nextLesson(completed) ?: return
        if (repository.loadLesson(next) == null) {
            LessonDownloadWork.enqueuePrefetch(applicationContext, next)
        }
    }

    private fun isRetryableConnectionError(error: Throwable): Boolean {
        val chain = generateSequence(error) { it.cause }.toList()
        if (chain.any { throwable ->
                val message = throwable.message.orEmpty().lowercase()
                "enospc" in message || "no space" in message || "verification failed" in message || "does not match" in message
            }) return false
        return chain.any { it is IOException } || chain.any { throwable ->
            val message = throwable.message.orEmpty().lowercase()
            "server returned http 5" in message || "server returned http 429" in message
        }
    }

    companion object {
        private const val MAX_RETRIES = 4
    }
}
