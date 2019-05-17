package com.dichotome.locator.api

import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface APIService {

    @GET(".")
    fun fetchVenues(
        @Query("limit") limit: Int,
        @Query("ll") ll: String,
        @Query("query") query: String,
        @Query("client_id") clientId: String = "4HWKLQAR1YAP2300PKZDTCO3TSJXKBKN24UV1WEV5XFLM2PX",
        @Query("client_secret") clientSecret: String = "GII13UVUXHXWMWDUE2WJ1LJQNLG2PII3SXS3P5UUL5ZD4YJW",
        @Query("v") v: Int = 20180323
    ): Observable<APIModel.VenueResult>

    @GET(".")
    fun fetchPhotos(
        @Query("client_id") clientId: String = "4HWKLQAR1YAP2300PKZDTCO3TSJXKBKN24UV1WEV5XFLM2PX",
        @Query("client_secret") clientSecret: String = "GII13UVUXHXWMWDUE2WJ1LJQNLG2PII3SXS3P5UUL5ZD4YJW",
        @Query("v") v: Int = 20180323
    ): Observable<APIModel.PictureResult>

    companion object {
        fun create(category: String) = Retrofit.Builder()
            .baseUrl("https://api.foursquare.com/v2/venues/$category/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(APIService::class.java)
    }
}