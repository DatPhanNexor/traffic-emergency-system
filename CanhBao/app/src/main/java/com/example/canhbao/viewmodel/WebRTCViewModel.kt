package com.example.canhbao.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.canhbao.dto.CallSignalDto
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.webrtc.*
import android.content.Context
import android.media.AudioManager
import org.webrtc.audio.JavaAudioDeviceModule
import ua.naiksoftware.stomp.StompClient

class WebRTCViewModel(application: Application) : AndroidViewModel(application) {

    private val gson = Gson()
    private val _callState = MutableStateFlow("IDLE")
    val callState = _callState.asStateFlow()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private val pendingCandidates = mutableListOf<IceCandidate>()
    private var targetUserId: String? = null // Thêm biến này để lưu ID người gọi
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    private var localAudioTrack: AudioTrack? = null

    private val _callerName = MutableStateFlow("Trụ sở công an")
    val callerName = _callerName.asStateFlow()
    init {
        initWebRTC()
    }

    private fun initWebRTC() {
        val options = PeerConnectionFactory.InitializationOptions.builder(getApplication())
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        // 1. Tạo Module quản lý âm thanh phần cứng
        val audioDeviceModule = JavaAudioDeviceModule.builder(getApplication())
            .setUseHardwareAcousticEchoCanceler(true) // Quan trọng để hết vang
            .setUseHardwareNoiseSuppressor(true)      // Quan trọng để hết rè
            .createAudioDeviceModule()

        val factoryOptions = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(factoryOptions)
            .setAudioDeviceModule(audioDeviceModule) // BẮT BUỘC PHẢI CÓ DÒNG NÀY
            .createPeerConnectionFactory()

        // 2. Thêm các tham số cấu hình lọc âm
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        }

        audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("101", audioSource)
        localAudioTrack?.setEnabled(true)

        Log.d("WebRTC", "Khởi tạo thành công với Hardware AEC")
    }

    // --- 1. CHỦ ĐỘNG GỌI (GỬI OFFER) ---
    fun startCall(stompClient: StompClient, targetId: String, myId: String) {
        setupAudioConfig()
        _callState.value = "CALLING"
        if (peerConnection == null) peerConnection = createPeerConnection(targetId, stompClient, myId)

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)

                // Thay thế đoạn cũ bằng:
                val signal = CallSignalDto(type = "offer", to = targetId, from = myId, sdp = sdp?.description)
                sendSignalSafely(stompClient, signal)
            }
        }, constraints)
    }

    // --- 2. XỬ LÝ TÍN HIỆU TỪ SOCKET ---
    fun handleSignal(json: JSONObject, stompClient: StompClient, myId: String) {
        val signal = gson.fromJson(json.toString(), CallSignalDto::class.java)
        val fromUser = signal.from ?: ""

        when (signal.type?.uppercase()) {
            "BYE" -> {
                Log.d("WebRTC", "Trụ sở đã cúp máy")
                // Gọi đóng kết nối tại chỗ (không gửi ngược lại BYE)
                peerConnection?.close()
                peerConnection = null
                _callState.value = "IDLE"
                val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.mode = AudioManager.MODE_NORMAL
            }
            "OFFER" -> {
                targetUserId = fromUser // 1. Lưu ID người gọi lại ở đây
                setupAudioConfig()
                _callerName.value = fromUser.replace("TRU_SO_", "Trụ sở ").replace("_", " ")

                _callState.value = "INCOMING"
                val sdp = SessionDescription(SessionDescription.Type.OFFER, signal.sdp)
                if (peerConnection == null) {
                    peerConnection = createPeerConnection(fromUser, stompClient, myId)
                }

                peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        Log.d("WebRTC", "Đã nhận OFFER, đang chờ người dùng nhấc máy...")
                        // Thêm các candidate đã nhận trước đó vào
                        pendingCandidates.forEach { peerConnection?.addIceCandidate(it) }
                        pendingCandidates.clear()
                        // TUYỆT ĐỐI KHÔNG gọi createAnswer ở đây
                    }
                }, sdp)
            }

            "ANSWER" -> {
                val sdp = SessionDescription(SessionDescription.Type.ANSWER, signal.sdp)
                peerConnection?.setRemoteDescription(SimpleSdpObserver(), sdp)
            }
            "CANDIDATE" -> {
                val candidateData = signal.candidate as? Map<*, *>
                candidateData?.let {
                    val sdpCandidate = it["candidate"] as? String ?: ""
                    val sdpMid = it["sdpMid"] as? String ?: ""
                    val sdpMLineIndex = (it["sdpMLineIndex"] as? Number)?.toInt() ?: 0

                    val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdpCandidate)

                    // Luôn add nếu đã có remoteDescription, ngược lại thì cho vào hàng đợi
                    if (peerConnection?.remoteDescription != null) {
                        peerConnection?.addIceCandidate(candidate)
                        Log.d("WebRTC", "✅ Đã thêm Candidate: $sdpCandidate")
                    } else {
                        pendingCandidates.add(candidate)
                        Log.d("WebRTC", "⏳ Đang đợi RemoteDesc, đã lưu candidate tạm thời")
                    }
                }
            }
        }
    }

    // --- 3. HÀM CHẤP NHẬN CUỘC GỌI (DÙNG TRONG UI) ---
    fun acceptCall(stompClient: StompClient?, myId: String) {
        val currentTargetId = targetUserId // Chụp giá trị

        if (stompClient == null || peerConnection == null || currentTargetId == null) {
            Log.e("WebRTC", "Không thể nhấc máy: Thiếu thông tin")
            return
        }

        _callState.value = "CONNECTED"

        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)

                // Thay thế đoạn cũ bằng:
                val signal = CallSignalDto(type = "answer", to = currentTargetId, from = myId, sdp = sdp?.description)
                sendSignalSafely(stompClient, signal)
            }
        }, MediaConstraints())
    }

    private fun createAnswer(toUser: String, stompClient: StompClient, myId: String) {
        // Sử dụng trực tiếp tham số toUser được truyền vào hàm
        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)

                val signal = CallSignalDto(
                    type = "answer",
                    to = toUser, // Sử dụng tham số toUser thay vì currentTargetId
                    from = myId,
                    sdp = sdp?.description
                )
                sendSignalSafely(stompClient, signal)
                Log.d("WebRTC", "Đã gửi answer (chữ thường) cho Web")
            }
        }, MediaConstraints())
    }

    // --- 4. CẤU HÌNH KẾT NỐI ---
    private fun createPeerConnection(targetId: String, stompClient: StompClient, myId: String): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        val pc = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    val iceMap = mapOf("sdpMid" to it.sdpMid, "sdpMLineIndex" to it.sdpMLineIndex, "candidate" to it.sdp)
                    val signal = CallSignalDto(type = "candidate", to = targetId, from = myId, candidate = iceMap)
                    // Dùng hàm an toàn ở đây
                    sendSignalSafely(stompClient, signal)
                }
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                Log.d("WebRTC", "ICE State: $newState")
                if (newState == PeerConnection.IceConnectionState.CONNECTED) {
                    applySonyAudioFix()
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is AudioTrack) {
                    track.setEnabled(true)
                    // GIẢM TỪ 10.0 XUỐNG 1.0
                    track.setVolume(1.0)
                    Log.d("WebRTC", "Đã nhận âm thanh từ Web (Volume: 1.0)")
                }
            }

            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddStream(p0: MediaStream?) {}
        })

        localAudioTrack?.let { track ->
            pc?.addTrack(track, listOf("ARDMSv0"))
        }

        return pc
    }

    private fun applySonyAudioFix() {
        val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
        audioManager.setStreamVolume(
            AudioManager.STREAM_VOICE_CALL,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
            0
        )
    }

    private fun setupAudioConfig() {
        val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
    }
    fun toggleMic(isMuted: Boolean) {
        // isMuted = true (Người dùng bấm tắt) -> setEnabled(false) (Dừng track)
        // isMuted = false (Người dùng bấm mở) -> setEnabled(true) (Chạy track)
        localAudioTrack?.setEnabled(!isMuted)
        Log.d("WebRTC", "Trạng thái Micro: ${if (isMuted) "ĐÃ TẮT" else "ĐANG MỞ"}")
    }
    fun endCall(stompClient: StompClient? = null, myId: String? = null) {
        val currentTarget = targetUserId

        // 1. Chỉ gửi BYE nếu thực sự đang trong cuộc gọi và có thông tin
        if (stompClient != null && currentTarget != null && myId != null) {
            val signal = CallSignalDto(
                type = "BYE",
                to = currentTarget,
                from = myId
            )
            sendSignalSafely(stompClient, signal)
        }

        // 2. Dọn dẹp tài nguyên WebRTC
        try {
            peerConnection?.close()
        } catch (e: Exception) {
            Log.e("WebRTC", "Lỗi khi đóng PC: ${e.message}")
        }

        peerConnection = null
        targetUserId = null
        _callState.value = "IDLE"

        // 3. Trả Audio về chế độ bình thường
        val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
    }
    private fun sendSignalSafely(stompClient: StompClient?, signal: CallSignalDto) {
        if (stompClient != null && stompClient.isConnected) {
            stompClient.send("/app/call-signal", gson.toJson(signal))
                .subscribe({
                    Log.d("WebRTC", "Gửi signal ${signal.type} thành công")
                }, { throwable ->
                    // Hứng lỗi ở đây để không bị crash App
                    Log.e("WebRTC", "Lỗi khi gửi signal: ${throwable.message}")
                })
        } else {
            Log.w("WebRTC", "Không thể gửi signal ${signal.type} vì Socket đã ngắt kết nối")
        }
    }

    open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) { Log.e("WebRTC", "SDP Create Fail: $p0") }
        override fun onSetFailure(p0: String?) { Log.e("WebRTC", "SDP Set Fail: $p0") }
    }


    override fun onCleared() {
        // Không truyền stompClient vào đây vì thường lúc này socket đã bị ngắt
        // hoặc không còn context an toàn để gửi tin.
        // Chỉ dọn dẹp tài nguyên local để giải phóng bộ nhớ.
        endCall(null, null)

        audioSource?.dispose()
        peerConnectionFactory?.dispose()
        super.onCleared()
    }
}