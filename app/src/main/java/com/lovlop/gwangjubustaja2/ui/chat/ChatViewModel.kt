package com.lovlop.gwangjubustaja2.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lovlop.gwangjubustaja2.data.BusRoute
import com.lovlop.gwangjubustaja2.data.model.ChatMessage
import com.lovlop.gwangjubustaja2.domain.ArrivalProvider
import com.lovlop.gwangjubustaja2.domain.ArrivalResult
import com.lovlop.gwangjubustaja2.domain.RemoteArrivalProvider
import com.lovlop.gwangjubustaja2.domain.RemoteVehicleProvider
import com.lovlop.gwangjubustaja2.domain.StationDirectory
import com.lovlop.gwangjubustaja2.domain.StationMatch
import com.lovlop.gwangjubustaja2.domain.VehicleProvider
import com.lovlop.gwangjubustaja2.domain.VehicleResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    /**
     * 실제 Worker에서 받아온다.
     *
     * 서버 없이 UI만 만질 때는 FakeArrivalProvider()로 바꿔 끼우면 된다.
     * 그때는 정류소 ID 자리에 empty / error / notfound / slow 를 넣어
     * 각 분기를 재빌드 없이 확인할 수 있다.
     */
    private val provider: ArrivalProvider = RemoteArrivalProvider()

    private val vehicles: VehicleProvider = RemoteVehicleProvider()

    private val directory = StationDirectory(app)
    private val formatter = ArrivalFormatter()

    /**
     * 사전 적재를 화면이 열리자마자 시작해둔다.
     *
     * 첫 메시지를 받고 나서 받기 시작하면 수천 건을 기다리는 동안 챗봇이 먹통처럼 보인다.
     * 사용자가 뭔가 입력하는 사이에 끝나 있도록 미리 돌린다.
     */
    private val loading: Deferred<Result<Int>> =
        viewModelScope.async { runCatching { directory.ensureLoaded() } }

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
                // 코루틴 취소는 오류가 아니다. 삼키면 화면 전환 때 이상하게 동작한다.
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "응답 생성 실패 input=$trimmed", e)
                "처리 중 문제가 생겼어요. 잠시 후 다시 물어봐 주세요."
            }

            _removeLoading.value = true
            _newMessage.value = ChatMessage.Bot(text = text)
        }
    }

    private suspend fun respondTo(input: String): String {
        val loaded = loading.await()

        // 사전을 못 받았어도 원본 ID(KJB908)는 통하므로 바로 포기하지 않는다.
        if (loaded.isFailure) {
            Log.w(TAG, "사전 적재 실패", loaded.exceptionOrNull())
        }

        // "18번"처럼 뒤에 "번"을 붙이면 노선 위치를 묻는 것으로 본다.
        //
        // 정류소 기둥 번호와 노선 번호가 둘 다 숫자라 "18"만으로는 가릴 수 없다.
        // 그래서 "번"이 붙으면 노선을 먼저 보고, 안 붙으면 정류소를 먼저 본다.
        if (input.trim().endsWith("번")) {
            directory.resolveRoute(input)?.let { return locate(it) }
        }

        return when (val match = directory.resolve(input)) {
            is StationMatch.One -> {
                val result = provider.getArrivals(stationId = match.stationId)
                if (result is ArrivalResult.Error) {
                    Log.w(TAG, "조회 실패 ${match.stationId} ${result.reason} ${result.message}")
                }
                formatter.format(result, match.name)
            }

            is StationMatch.Several -> {
                // 순서대로 부르면 정류소 수만큼 기다린다. 동시에 던진다.
                val results = coroutineScope {
                    match.stations
                        .map { station -> async { station to provider.getArrivals(station.stationId) } }
                        .awaitAll()
                }
                formatter.formatMulti(input, results)
            }

            is StationMatch.Many -> formatter.formatCandidates(input, match.candidates)

            // 정류소로 못 찾았으면 노선 번호일 수 있다. "18", "419"처럼.
            is StationMatch.None -> {
                val route = directory.resolveRoute(input)
                when {
                    route != null -> locate(route)

                    loaded.isFailure ->
                        "정류소 목록을 아직 받지 못했어요. 인터넷 연결을 확인하고 다시 물어봐 주세요."

                    else -> formatter.formatNotFound(input)
                }
            }
        }
    }

    private suspend fun locate(route: BusRoute): String {
        val result = vehicles.getVehicles(route.routeId)
        if (result is VehicleResult.Error) {
            Log.w(TAG, "위치 조회 실패 ${route.routeId} ${result.reason} ${result.message}")
        }
        return formatter.formatVehicles(route.routeNo, result)
    }

    private companion object {
        const val TAG = "ChatViewModel"
    }
}
