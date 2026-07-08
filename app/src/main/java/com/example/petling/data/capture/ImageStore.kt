package com.example.petling.data.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 캡처 이미지를 앱 내부 저장소에 보관한다(온디바이스 원칙 — 서버 미전송).
 * 원본을 그대로 두지 않고 JPEG로 재압축해 용량을 줄인다.
 */
class ImageStore(private val context: Context) {

    private val dir: File by lazy {
        File(context.filesDir, "captures").apply { mkdirs() }
    }

    /** Uri의 이미지를 내부에 저장하고 파일 경로를 반환. 실패 시 null. */
    suspend fun save(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = context.contentResolver.openInputStream(uri).use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return@runCatching null
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
            file.absolutePath
        }.onFailure {
            Log.w(TAG, "이미지 저장 실패: $uri", it)
            runCatching { FirebaseCrashlytics.getInstance().recordException(it) }
        }.getOrNull()
    }

    fun delete(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
            .onFailure { Log.w(TAG, "이미지 삭제 실패: $path", it) }
    }

    private companion object {
        const val TAG = "ImageStore"
    }
}
