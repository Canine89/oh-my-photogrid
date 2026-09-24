package app.wireframephoto.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.IntentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdatePhase {
    data object Idle : UpdatePhase
    data class Downloading(val progress: Float) : UpdatePhase
    /** Handed to the system installer; waiting for the user to confirm. */
    data object Installing : UpdatePhase
    data object Failed : UpdatePhase
}

/**
 * Downloads the release APK and installs it over this app with [PackageInstaller]. The system
 * shows its own "update this app?" confirmation, and only accepts an APK signed with the same key.
 * Session results arrive as a broadcast ([StatusReceiver]); broadcasts, unlike activity
 * intents, aren't blocked when the installer's dialog is in front of the app.
 */
object UpdateInstaller {
    private val _phase = MutableStateFlow<UpdatePhase>(UpdatePhase.Idle)
    val phase: StateFlow<UpdatePhase> = _phase

    suspend fun downloadAndInstall(context: Context, apkUrl: String) {
        val app = context.applicationContext
        _phase.value = UpdatePhase.Downloading(0f)
        runCatching {
            val apk = download(app, apkUrl)
            // Set before committing: the receiver may report back (e.g. aborted) right away.
            _phase.value = UpdatePhase.Installing
            withContext(Dispatchers.IO) { commit(app, apk) }
        }.onFailure { _phase.value = UpdatePhase.Failed }
    }

    /** Receives the session's result: shows the system confirmation, or reports the outcome. */
    class StatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                // Arrives right after commit, while the app is still in front.
                PackageInstaller.STATUS_PENDING_USER_ACTION ->
                    IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                        ?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                // On success the system replaces this process; nothing to do.
                PackageInstaller.STATUS_SUCCESS -> Unit
                // The user backed out (or left for the "allow this source" setting): offer the button again.
                PackageInstaller.STATUS_FAILURE_ABORTED -> _phase.value = UpdatePhase.Idle
                else -> _phase.value = UpdatePhase.Failed
            }
        }
    }

    private suspend fun download(context: Context, url: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { deleteRecursively(); mkdirs() }
        val out = File(dir, "update.apk")
        // GitHub answers with a redirect to its asset CDN (https → https), which this follows.
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            check(conn.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${conn.responseCode}" }
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        if (total > 0) _phase.value = UpdatePhase.Downloading(done.toFloat() / total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        out
    }

    private fun commit(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            // Android 12+: when this app is already the installer of record (it updated itself
            // before), the system may install without asking again. Otherwise it still asks.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val status = PendingIntent.getBroadcast(
                context, id, Intent(context, StatusReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(status.intentSender)
        }
        apk.delete()
    }
}
