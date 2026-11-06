package com.lovlop.gwangjubustaja2.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovlop.gwangjubustaja2.data.model.ChatMessage
import com.lovlop.gwangjubustaja2.domain.ArrivalProvider
import com.lovlop.gwangjubustaja2.domain.ArrivalResult
import com.lovlop.gwangjubustaja2.domain.FakeArrivalProvider
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val provider: ArrivalProvider = FakeArrivalProvider()

    private val _newMessage = MutableLiveData<ChatMessage>()
    val newMessage: LiveData<ChatMessage> = _newMessage

    private val _removeLoading = MutableLiveData<Boolean>()
    val removeLoading: LiveData<Boolean> = _removeLoading

    fun sendMessage(stationId: String) {
        val trimmed = stationId.trim()
        if (trimmed.isEmpty()) return

        _newMessage.value = ChatMessage.User(text = trimmed)
        _newMessage.value = ChatMessage.Loading

        viewModelScope.launch {
            val result = provider.getArrivals(stationId = trimmed)

                _removeLoading.value = true

            val responseText = when (result) {
                is ArrivalResult.Success -> {
                    val sb = StringBuilder("🚌 도착 예정 정보입니다:\n")
                    result.arrivals.forEach {
                        var line = "• ${it.routeName}번 : ${it.minutes}분 후"
                        if (it.isCorrected && it.correctionDelta > 0) {
                            line += " (평소보다 ${it.correctionDelta}분 더 걸려요)"
                        }
                        sb.append(line).append("\n")
                    }
                    sb.toString().trimEnd()
                }

                is ArrivalResult.Empty -> {
                    "지금은 도착 예정인 버스가 없습니다."
                }

                is ArrivalResult.Error -> {
                    "정보를 가져오지 못했습니다."
                }
            }

            _newMessage.value = ChatMessage.Bot(text = responseText)
        }
    }
}