package com.ayursh.android.network

import android.content.Context
import com.ayursh.android.BuildConfig
import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val TAG = "RetrofitClient"
//    private const val BASE_URL = 'https://prod.ayursh.com/'


    fun create(context: Context): MyApis {
        val client = OkHttpClient.Builder()
            .addInterceptor(ChuckerInterceptor(context))
            .build()
//        if(BASE_URL=="https://dev.ayursh.com/") {
//            val retrofit = Retrofit.Builder()
//                .baseUrl(BASE_URL) //base url will come
//                .addConverterFactory(GsonConverterFactory.create())
//                .client(client)
//                .build()
//            return retrofit.create(MyApis::class.java)
//        }
//        else{
            val retrofit= Retrofit.Builder()
                .baseUrl("https://prod.ayursh.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(MyApis::class.java)
//        }
    }

    fun createCustom(baseUrl: String, context: Context): MyApis {
        val client = OkHttpClient.Builder()
            .addInterceptor(ChuckerInterceptor(context))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(MyApis::class.java)
    }





}