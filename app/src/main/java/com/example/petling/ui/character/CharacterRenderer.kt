package com.example.petling.ui.character

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.example.petling.domain.model.CharacterSpec

/**
 * 캐릭터 렌더링 추상화. UI 화면들은 이 인터페이스만 참조하므로
 * 이후 Lottie/Rive 렌더러로 교체해도 화면 코드는 바뀌지 않는다.
 */
interface CharacterRenderer {
    @Composable
    fun Render(spec: CharacterSpec, modifier: Modifier)
}

/** 현재 캐릭터 렌더러. 기본값은 Canvas 구현. */
val LocalCharacterRenderer = staticCompositionLocalOf<CharacterRenderer> {
    CanvasCharacterRenderer()
}
