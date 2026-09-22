package com.jun.parknavi.data.remote

// Repository가 던지는 실패를 종류별로 구분해서, ViewModel이 "재시도해볼 만한 문제"와
// "그냥 안내만 하면 되는 문제"를 구별해 사용자 문구를 고를 수 있게 한다. 원본 예외의
// 메시지(특히 공공 API의 원문 resultMsg)를 화면에 그대로 노출하지 않기 위한 경계이기도 하다.
sealed class ApiError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(cause: Throwable) : ApiError("네트워크 오류", cause)
    class Server(val resultCode: String, val resultMsg: String) : ApiError(resultMsg)
    class Unknown(cause: Throwable) : ApiError("알 수 없는 오류", cause)
}
