package app.wireframephoto

import android.app.Application
import android.content.ComponentCallbacks2
import app.wireframephoto.render.BitmapLoader
import app.wireframephoto.render.CollageRenderer

class WireframeApp : Application() {
    /** Shared across activity recreation (fold/unfold) so previews don't need re-decoding. */
    val loader by lazy { BitmapLoader(contentResolver) }
    val renderer by lazy { CollageRenderer(loader) }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) loader.clear()
    }
}
