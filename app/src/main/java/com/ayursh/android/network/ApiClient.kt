package com.ayursh.android.network

import android.content.Context
import com.ayursh.android.BuildConfig
import com.chuckerteam.chucker.api.ChuckerInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class ApiClient {

 companion object {

     val BASE_URL = "https://fcm.googleapis.com/"
   //  val BASE_URL ="https://fcm.googleapis.com/v1/projects/myproject-b5ae1/messages:send/"
     public var retrofit: Retrofit? = null
     fun getClient(context: Context): Retrofit? {

//         if (retrofit == null) {
//             val client = OkHttpClient.Builder()
//                 .addInterceptor(ChuckerInterceptor(context))
//                 .build()
//             if(BuildConfig.BASE_URL=="https://dev.ayursh.com/") {
//                 retrofit = Retrofit.Builder()
//                     .baseUrl(BASE_URL)
//                     .client(client)
//                     .addConverterFactory(GsonConverterFactory.create())
//                     .build()
//             } else{
//                 retrofit = Retrofit.Builder()
//                     .baseUrl(BASE_URL)
//                     .addConverterFactory(GsonConverterFactory.create())
//                     .build()
//             }
//         }
         return retrofit
     }
 }
}