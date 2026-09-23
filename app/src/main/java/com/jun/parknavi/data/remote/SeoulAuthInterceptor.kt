package com.jun.parknavi.data.remote

import com.jun.parknavi.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

// 모든 서울 버스 API 요청에 serviceKey 쿼리 파라미터를 자동으로 붙여준다.
// data.go.kr 인증키는 이미 URL-encode된 값으로 발급되므로, addEncodedQueryParameter로
// 붙여야 Retrofit이 다시 인코딩해서 "이중 인코딩"으로 인증에 실패하는 걸 막을 수 있다.
class SeoulAuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val newUrl = original.url.newBuilder()
            .addEncodedQueryParameter("serviceKey", BuildConfig.SEOUL_SERVICE_KEY)
            .build()
        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}
