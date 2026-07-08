package com.example.petling.data.capture

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * ML Kit 온디바이스 텍스트 인식(한국어 스크립트, 무료).
 * 이미지에서 원시 텍스트를 추출하며, 네트워크를 타지 않는다.
 */
class OcrTextExtractor(private val context: Context) {

    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    /** 이미지 Uri에서 텍스트를 추출한다. 실패 시 빈 문자열. */
    suspend fun extract(uri: Uri): String {
        return runCatching {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image).await().text
        }.getOrElse {
            android.util.Log.w("Petling", "OCR 실패: ${it.message}")
            ""
        }
    }
}
