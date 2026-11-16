package com.lovlop.gwangjubustaja2.domain

enum class ChatIntent {
    BUS_ARRIVAL,
    ROUTE_SEARCH,
    STATION_SEARCH,
    UNKNOWN
}

data class ParsedQuery(
    val intent: ChatIntent,
    val targetKeyword: String,
    val tokens: List<String>
)

object IntentClassifier {

    private val arrivalKeywords = listOf("언제 와", "언제와", "도착", "몇 분", "몇분", "남았", "어디쯤")
    private val routeKeywords = listOf("가는", "가려면", "어떻게 가", "노선", "타야", "방면")
    private val stationKeywords = listOf("정류장", "승강장", "근처", "주변")

    // 불용어 (핵심 키워드 추출 시 제외할 단어)
    private val stopWords = listOf("버스", "알려줘", "알려주세요", "좀", "제발", "?", "!")

    fun classifyIntent(rawInput: String): ParsedQuery {
        val cleaned = rawInput.trim()

        // 1) 의도 판별 (우선순위: 도착시간 -> 경로탐색 -> 정류장)
        val intent = when {
            arrivalKeywords.any { cleaned.contains(it) } -> ChatIntent.BUS_ARRIVAL
            routeKeywords.any { cleaned.contains(it) } -> ChatIntent.ROUTE_SEARCH
            stationKeywords.any { cleaned.contains(it) } -> ChatIntent.STATION_SEARCH
            else -> ChatIntent.UNKNOWN
        }

        val tokens = tokenize(cleaned)

        val allIntentWords = arrivalKeywords + routeKeywords + stationKeywords + stopWords
        val targetKeyword = tokens.firstOrNull { token ->
            allIntentWords.none { keyword -> token.contains(keyword) }
        } ?: ""

        return ParsedQuery(
            intent = intent,
            targetKeyword = targetKeyword,
            tokens = tokens
        )
    }

    private fun tokenize(input: String): List<String> {
        var normalized = input

        normalized = normalized.replace("언제와", "언제 와")
        normalized = normalized.replace("몇분", "몇 분")

        val result = mutableListOf<String>()
        val words = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }

        var i = 0
        while (i < words.size) {
            if (i + 1 < words.size && "${words[i]} ${words[i + 1]}" in arrivalKeywords) {
                result.add("${words[i]} ${words[i + 1]}")
                i += 2
            } else {
                result.add(words[i])
                i++
            }
        }
        return result
    }
}