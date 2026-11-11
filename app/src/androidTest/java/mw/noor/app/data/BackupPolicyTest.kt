package mw.noor.app.data

import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupPolicyTest {
    @Test
    fun appBackupFlagIsDisabled() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, 0)

        assertEquals(0, applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    @Test
    fun extractionRulesExcludeSharedPreferencesAndFilesForCloudAndDeviceTransfer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resourceId = context.resources.getIdentifier("data_extraction_rules", "xml", context.packageName)
        assertTrue("Android 12+ extraction rules must be packaged", resourceId != 0)

        val parser = context.resources.getXml(resourceId)
        try {
            var section: String? = null
            var cloudPreferencesExcluded = false
            var cloudFilesExcluded = false
            var transferPreferencesExcluded = false
            var transferFilesExcluded = false
            while (parser.eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "cloud-backup", "device-transfer" -> section = parser.name
                        "exclude" -> {
                            val domain = parser.getAttributeValue(null, "domain")
                            val path = parser.getAttributeValue(null, "path")
                            if (path == ".") {
                                if (section == "cloud-backup" && domain == "sharedpref") cloudPreferencesExcluded = true
                                if (section == "cloud-backup" && domain == "file") cloudFilesExcluded = true
                                if (section == "device-transfer" && domain == "sharedpref") transferPreferencesExcluded = true
                                if (section == "device-transfer" && domain == "file") transferFilesExcluded = true
                            }
                        }
                    }
                }
                if (parser.eventType == org.xmlpull.v1.XmlPullParser.END_TAG && parser.name == section) section = null
                parser.next()
            }

            assertTrue("Cloud backup must exclude preferences", cloudPreferencesExcluded)
            assertTrue("Cloud backup must exclude app files", cloudFilesExcluded)
            assertTrue("Device transfer must exclude preferences", transferPreferencesExcluded)
            assertTrue("Device transfer must exclude app files", transferFilesExcluded)
        } finally {
            parser.close()
        }
    }
}
