package com.production.fridgewise.ui.recipes

/**
 * Biến 1 đoạn văn liền mạch (data crawl) thành danh sách bước/dòng để render.
 *
 * Thứ tự xử lý:
 * 1. Thẻ xuống dòng (br/p/div/li) -> "\n", bóc tag còn lại, giải mã HTML entity.
 * 2. Bỏ marker "Bước 2:" viết sẵn trong câu.
 * 3. Tách ranh giới câu ở dấu . ! ? (chặn số thập phân "0.5", giữ nguyên dấu câu).
 * 4. Bỏ marker đầu dòng: "-", "•", "1.", "2)".
 *
 * Thuần Kotlin (không phụ thuộc Android) nên chạy được trong unit test JVM.
 */
internal object RecipeText {

    /** "Bước 2:" / "B2." viết liền trong câu -> xuống dòng (bắt buộc có dấu phân cách). */
    private val STEP_MARKER = Regex("""\s*(?:bước|b)\s*\d+\s*[:.)\-]\s*""", RegexOption.IGNORE_CASE)

    /**
     * Ranh giới câu: chỉ khớp phần khoảng trắng, giữ nguyên dấu câu.
     * - `(?<=\d\.)\s+` cho "bước 2. Rửa sạch".
     * - `(?<=[^\d\s][.!?])\s*` cho "sạch. Ướp" và "sạch.Ướp".
     * - "0.5"/"1.5" không khớp: dấu chấm kẹp giữa 2 chữ số.
     *
     * ponytail: ranh giới dựa vào chữ HOA đầu câu — câu mở đầu bằng chữ thường sẽ không
     * được tách. Nếu gặp data thật như vậy, thêm danh sách động từ mệnh lệnh ("cho|đun|rửa…")
     * hoặc tách theo số lượng tối thiểu để bù.
     */
    private val SENTENCE_BOUNDARY = Regex("""(?<=\d[.!?])\s+(?=\p{Lu})|(?<=[^\d\s][.!?])\s*(?=\p{Lu})""")

    /** Marker đánh số/đầu dòng còn sót ở đầu mỗi bước. */
    private val LINE_PREFIX = Regex(
        """^(?:[\-–—•*+·]\s*|\d{1,2}\s*[.)\-:](?!\d)\s*|(?:bước|b)\s*\d+\s*[:.)\-]\s*)+""",
        RegexOption.IGNORE_CASE
    )

    private val TAG = Regex("""<[^>]*>""")
    private val ENTITY = Regex("""&(#x?[0-9A-Fa-f]+|[A-Za-z]+);""")
    private val NAMED_ENTITIES = mapOf(
        "nbsp" to " ", "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
        "lsquo" to "\u2018", "rsquo" to "\u2019", "ldquo" to "\u201C", "rdquo" to "\u201D",
        "hellip" to "\u2026", "ndash" to "\u2013", "mdash" to "\u2014", "deg" to "\u00B0",
    )

    fun stepLines(raw: String?): List<String> {
        val input = raw?.takeIf { it.isNotBlank() } ?: return emptyList()
        val plain = decodeEntities(
            input
                .replace(Regex("""(?i)<br\s*/?>"""), "\n")
                .replace(Regex("""(?i)<li[^>]*>"""), "\n")
                .replace(Regex("""(?i)</?(?:p|div|li|h[1-6]|tr|ul|ol|table)\s*[^>]*>"""), "\n")
                .replace(TAG, "")
        )
        val normalized = plain.replace('\u00A0', ' ').replace(Regex("""[ \t]+"""), " ")

        return normalized
            .replace(STEP_MARKER, "\n")
            .replace(SENTENCE_BOUNDARY, "\n")
            .split('\n')
            .map { LINE_PREFIX.replace(it, "").trim().removeSuffix(".") }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun decodeEntities(value: String): String = ENTITY.replace(value) { match ->
        val entity = match.groupValues[1]
        when {
            entity.startsWith("#x", ignoreCase = true) ->
                entity.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: match.value
            entity.startsWith("#") ->
                entity.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: match.value
            else -> NAMED_ENTITIES[entity.lowercase()] ?: match.value
        }
    }
}
