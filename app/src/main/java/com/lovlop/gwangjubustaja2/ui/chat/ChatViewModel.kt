package com.lovlop.gwangjubustaja2.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lovlop.gwangjubustaja2.data.model.Arrival
import com.lovlop.gwangjubustaja2.data.model.ChatMessage
import com.lovlop.gwangjubustaja2.domain.ArrivalProvider
import com.lovlop.gwangjubustaja2.domain.ArrivalResult
import com.lovlop.gwangjubustaja2.domain.ChatIntent
import com.lovlop.gwangjubustaja2.domain.FakeArrivalProvider
import com.lovlop.gwangjubustaja2.domain.IntentClassifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    // 개발 중: FakeArrivalProvider (나중에 RemoteArrivalProvider()로만 교체)
    private val provider: ArrivalProvider = FakeArrivalProvider()

    private val _newMessage = MutableLiveData<ChatMessage>()
    val newMessage: LiveData<ChatMessage> = _newMessage

    private val _removeLoading = MutableLiveData<Boolean>()
    val removeLoading: LiveData<Boolean> = _removeLoading

    fun sendMessage(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        _newMessage.value = ChatMessage.User(text = trimmed)
        _newMessage.value = ChatMessage.Loading

        viewModelScope.launch {
            val text = try {
                respondTo(trimmed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "응답 생성 실패 input=$trimmed", e)
                "지금 정보를 가져오지 못했어요. 잠시 후 다시 물어봐 주세요."
            }

            _removeLoading.value = true
            _newMessage.value = ChatMessage.Bot(text = text)
        }
    }

    private suspend fun respondTo(input: String): String {
        // 1. 의도 분류 및 문장 분리 ("상무지구 / 가는 / 버스 / 언제 와")
        val parsed = IntentClassifier.classifyIntent(input)
        val splitText = parsed.tokens.joinToString(" / ")
        val query = parsed.targetKeyword.ifBlank { input.trim() }

        Log.d(TAG, "의도 분류 결과 -> intent=${parsed.intent}, keyword=$query, tokens=$splitText")

        // 2. 의도별 분기
        return when (parsed.intent) {
            ChatIntent.BUS_ARRIVAL -> {
                val result = provider.getArrivals(stationId = query)
                formatArrivalResult(query, splitText, result)
            }

            ChatIntent.ROUTE_SEARCH -> {
                val result = provider.getArrivals(stationId = query)
                "'$query' 방면 노선을 찾아볼게요!\n(분석: $splitText)\n\n${formatArrivalResult(query, splitText, result)}"
            }

            ChatIntent.STATION_SEARCH -> {
                "'$query' 근처 정류장을 조회할게요!\n(분석: $splitText)"
            }

            ChatIntent.UNKNOWN -> {
                // "empty", "error", "slow", "notfound" 등 테스트용 ID 및 단답 정류소명 처리
                val result = provider.getArrivals(stationId = query)
                formatArrivalResult(query, splitText, result)
            }
        }
    }

    private fun formatArrivalResult(query: String, splitText: String, result: ArrivalResult): String {
        return when (result) {
            is ArrivalResult.Success -> {
                val info = result.arrivals.joinToString("\n") { arrival: Arrival ->
                    val timeText = if (arrival.isArrivingSoon) "곧 도착" else "${arrival.minutes}분 후"
                    val delayText = if (arrival.isCorrected && arrival.correctionDelta > 0) {
                        " (평소보다 ${arrival.correctionDelta}분 더 걸려요)"
                    } else {
                        ""
                    }
                    "• ${arrival.routeName}: $timeText$delayText"
                }
                "'$query' 도착 정보입니다.\n(분석: $splitText)\n\n$info"
            }

            // Empty와 Error를 명확히 분리 (명세서 필수 규칙)
            is ArrivalResult.Empty -> {
                "지금은 도착 예정인 버스가 없어요. 막차가 끊겼을 수 있어요.\n(분석: $splitText)"
            }

            is ArrivalResult.Error -> {
                if (result.reason == ArrivalResult.Error.Reason.STATION_NOT_FOUND) {
                    "'$query' 정류장을 찾지 못했어요. 정류소 이름을 다시 확인해 주세요.\n(분석: $splitText)"
                } else {
                    "지금 정보를 가져오지 못했어요. 잠시 후 다시 물어봐 주세요."
                }
            }
        }
    }

    private companion object {
        const val TAG = "ChatViewModel"
    }
}