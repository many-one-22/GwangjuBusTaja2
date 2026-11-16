package com.lovlop.gwangjubustaja2.ui.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovlop.gwangjubustaja2.data.model.ChatMessage
import com.lovlop.gwangjubustaja2.domain.ArrivalProvider
import com.lovlop.gwangjubustaja2.domain.ArrivalResult
import com.lovlop.gwangjubustaja2.domain.RemoteArrivalProvider
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    /**
     * 실제 Worker에서 받아온다.
     *
     * 서버 없이 UI만 만질 때는 FakeArrivalProvider()로 바꿔 끼우면 된다.
     * 그때는 정류소 ID 자리에 empty / error / notfound / slow 를 넣어
     * 각 분기를 재빌드 없이 확인할 수 있다.
     */
    private val provider: ArrivalProvider = RemoteArrivalProvider()

    private val formatter = ArrivalFormatter()

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

            // 오류 원인은 말풍선에 다 담기 어려우니 Logcat에도 남긴다.
            if (result is ArrivalResult.Error) {
                Log.w(TAG, "조회 실패 stationId=$trimmed ${result.reason} ${result.message}")
            }

            _removeLoading.value = true
            _newMessage.value = ChatMessage.Bot(text = formatter.format(result))
        }
    }

    private companion object {
        const val TAG = "ChatViewModel"
    }
}
