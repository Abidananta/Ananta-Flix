package com.iptv.glass

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/** Application class — also configures Coil's image cache for channel logos. */
class GlassIptvApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("coil_logos")).maxSizeBytes(100L * 1024 * 1024).build() }
            .crossfade(true)
            .build()
    }
}
