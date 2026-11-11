package com.walter.lifelog.content.util

import org.springframework.stereotype.Component

/**
 * 업로드·스크랩으로 들어온 바이트가 서비스할 만한 HTML 인지 검사하고 title 을 뽑습니다.
 *
 * 목표는 claude.ai 에서 만든 단일 파일 아티팩트입니다. 그쪽은 inline style·script 를
 * 한 문서에 담아 내려주므로, 문서 골격만 확인하고 내용에는 손대지 않습니다.
 */
@Component
class HtmlMarkupSupporter {
    companion object {
        const val MAX_BYTES = 5 * 1024 * 1024
        private const val MIN_BYTES = 30
        private const val MAX_TITLE_LENGTH = 200
        private val ALLOWED_EXTENSIONS = setOf("html", "htm")
        private val DOCUMENT_MARKERS = listOf("<html", "<body", "<!doctype html")
        private val TITLE_PATTERN = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    }

    fun validateFileName(fileName: String?) {
        val extension = fileName?.substringAfterLast('.', "")?.lowercase()
        require(!extension.isNullOrBlank() && extension in ALLOWED_EXTENSIONS) {
            "HTML 파일만 등록할 수 있습니다 (.html, .htm)."
        }
    }

    /** 바이트 배열을 UTF-8 로 읽고 HTML 요건을 검사한 뒤 마크업을 돌려줍니다. */
    fun toValidatedMarkup(bytes: ByteArray): String {
        validateSize(bytes.size)
        require(!bytes.any { it == 0.toByte() }) { "텍스트가 아닌 파일입니다. HTML 문서를 올려주세요." }
        return validateMarkup(String(bytes, Charsets.UTF_8))
    }

    fun validateMarkup(markup: String): String {
        validateSize(markup.toByteArray(Charsets.UTF_8).size)
        val lowered = markup.lowercase()
        require(DOCUMENT_MARKERS.any { lowered.contains(it) }) {
            "HTML 문서로 보이지 않습니다. <html> 또는 <body> 가 있어야 합니다."
        }
        return markup
    }

    fun extractTitle(markup: String): String? {
        val raw = TITLE_PATTERN.find(markup)?.groupValues?.get(1) ?: return null
        val title = raw.replace(Regex("\\s+"), " ").trim()
        return title.ifBlank { null }?.take(MAX_TITLE_LENGTH)
    }

    private fun validateSize(byteSize: Int) {
        require(byteSize >= MIN_BYTES) { "내용이 비어 있거나 너무 짧습니다." }
        require(byteSize <= MAX_BYTES) { "마크업이 너무 큽니다. 최대 ${MAX_BYTES / 1024 / 1024}MB 까지 등록할 수 있습니다." }
    }
}
