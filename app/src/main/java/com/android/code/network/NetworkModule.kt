package com.android.code.network

import com.android.code.BuildConfig
import com.android.code.util.addFlipperInterceptor
import com.android.code.util.addStethoInterceptor
import com.android.code.util.empty
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

// API client service
internal inline fun <reified T : Any> provideAPIClientService(): T {
    return Retrofit.Builder()
        .client(
            provideOkHttpClient(
                provideHttpLoggingInterceptor(),
                provideAPIClientInterceptor()
            )
        )
        .baseUrl(BuildConfig.SERVER_URL)
        .addCallAdapterFactory(RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()))
        .addConverterFactory(
            GsonConverterFactory.create(
                GsonBuilder()
                    .registerTypeAdapterFactory(StringNullToEmptyAdapterFactory())
                    .serializeNulls()
                    .create()
            )
        )
        .build()
        .create(T::class.java)
}

private class ApiInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response =
        response(chain.proceed(request(chain.request())))

    private fun request(request: Request): Request =
        request.newBuilder()
            .addHeader("Content-Type", "application/json;charset=UTF-8")
            .url(request.url)
            .build()

    private fun response(response: Response): Response {
        return response
            .newBuilder()
            .addHeader("Content-Type", "application/json;charset=UTF-8")
            .build()
    }
}

// API client interceptor
private fun provideAPIClientInterceptor() = ApiInterceptor()

// Http logging interceptor
private fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

// Ok http client
private fun provideOkHttpClient(
    httpLoggingInterceptor: HttpLoggingInterceptor,
    apiClientInterceptor: ApiInterceptor,
): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor(httpLoggingInterceptor)
        .addInterceptor(apiClientInterceptor)
        .addStethoInterceptor()
        .addFlipperInterceptor()
        .build()


internal class StringNullToEmptyAdapterFactory : TypeAdapterFactory {

    @Suppress("UNCHECKED_CAST")
    override fun <T> create(gson: Gson?, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType as Class<T>
        return if (rawType != String::class.java) {
            null
        } else {
            StringNullToEmptyAdapter() as TypeAdapter<T>
        }
    }
}

private class StringNullToEmptyAdapter : TypeAdapter<String?>() {

    @Throws(IOException::class)
    override fun read(reader: JsonReader): String {
        if (reader.peek() === JsonToken.NULL) {
            reader.nextNull()
            return String.empty()
        }
        return reader.nextString()
    }

    @Throws(IOException::class)
    override fun write(writer: JsonWriter, value: String?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.value(value)
    }
}