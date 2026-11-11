package mw.noor.app.data

import android.content.Context
import android.content.ContextWrapper
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

/** Gives each instrumentation test independent SharedPreferences and app-private files. */
internal class IsolatedNoorContext private constructor(
    val context: Context,
    private val baseContext: Context,
    private val rootDirectory: File,
    private val preferenceNames: MutableSet<String>,
) {
    fun cleanup() {
        preferenceNames.forEach { name ->
            baseContext.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
        rootDirectory.deleteRecursively()
    }

    companion object {
        fun create(testName: String): IsolatedNoorContext {
            val base = androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().targetContext.applicationContext
            val suffix = "${testName}-${System.nanoTime()}"
            val files = File(base.filesDir, "noor-p0-$suffix").apply { mkdirs() }
            val preferences = CopyOnWriteArraySet<String>()
            val wrapped = object : ContextWrapper(base) {
                override fun getApplicationContext(): Context = this
                override fun getFilesDir(): File = files
                override fun getSharedPreferences(name: String, mode: Int) =
                    base.getSharedPreferences("noor-p0-$suffix-$name", mode).also {
                        preferences.add("noor-p0-$suffix-$name")
                    }
            }
            return IsolatedNoorContext(wrapped, base, files, preferences)
        }
    }
}
