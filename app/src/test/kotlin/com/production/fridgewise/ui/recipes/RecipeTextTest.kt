package com.production.fridgewise.ui.recipes

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeTextTest {

    @Test
    fun `run-on sentences split on dots`() {
        assertEquals(
            listOf("Cho 0.5 kg thịt", "Ướp 15 phút", "Đun nóng chảo"),
            RecipeText.stepLines("Cho 0.5 kg thịt. Ướp 15 phút. Đun nóng chảo")
        )
    }

    @Test
    fun `decimals survive but a missing space after the dot still splits`() {
        assertEquals(
            listOf("Cho 1.5 kg đường", "Ướp"),
            RecipeText.stepLines("Cho 1.5 kg đường.Ướp")
        )
    }

    @Test
    fun `numbered Buoc markers become steps`() {
        assertEquals(
            listOf("Rửa sạch", "Cắt nhỏ"),
            RecipeText.stepLines("Bước 1: Rửa sạch. Bước 2: Cắt nhỏ.")
        )
    }

    @Test
    fun `Buoc markers without periods still split`() {
        assertEquals(
            listOf("rửa sạch", "cắt nhỏ"),
            RecipeText.stepLines("bước 1: rửa sạch bước 2: cắt nhỏ")
        )
    }

    @Test
    fun `html paragraphs become lines`() {
        assertEquals(
            listOf("Rửa sạch", "Ướp 15 phút"),
            RecipeText.stepLines("<p>Rửa sạch</p><p>Ướp 15 phút</p>")
        )
    }

    @Test
    fun `br tags become lines`() {
        assertEquals(listOf("Bước 1", "Bước 2"), RecipeText.stepLines("Bước 1<br/>Bước 2"))
    }

    @Test
    fun `existing newlines and bullet prefixes are cleaned`() {
        assertEquals(listOf("Rửa sạch", "Ướp thịt"), RecipeText.stepLines("- Rửa sạch\n- Ướp thịt"))
    }

    @Test
    fun `numbered prefixes are stripped`() {
        assertEquals(listOf("Rửa sạch", "Cắt nhỏ"), RecipeText.stepLines("1. Rửa sạch\n2) Cắt nhỏ"))
    }

    @Test
    fun `quantity at line start is kept`() {
        assertEquals(listOf("2 muỗng dầu ăn"), RecipeText.stepLines("2 muỗng dầu ăn"))
    }

    @Test
    fun `single sentence stays one line`() {
        assertEquals(listOf("Chưng 20 phút"), RecipeText.stepLines("Chưng 20 phút"))
    }

    @Test
    fun `blank input yields nothing`() {
        assertEquals(emptyList<String>(), RecipeText.stepLines(null))
        assertEquals(emptyList<String>(), RecipeText.stepLines("   "))
    }
}
