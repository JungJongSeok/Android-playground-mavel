package com.android.code.data.repository

import com.android.code.CoroutinesTestExtension
import com.android.code.InstantExecutorExtension
import com.android.code.network.models.BaseResponse
import com.android.code.network.models.marvel.MarvelResult
import com.android.code.network.models.marvel.SampleResponse
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@kotlinx.coroutines.ExperimentalCoroutinesApi
@DisplayName("MarvelRepository 테스트")
@ExtendWith(InstantExecutorExtension::class, CoroutinesTestExtension::class)
class MarvelRxRepositoryTest {
    private lateinit var marvelRepository: MarvelRxRepository

    @Mock
    lateinit var marvelResult: MarvelResult

    @Mock
    lateinit var sampleResponse: SampleResponse

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val immediate: Scheduler = Schedulers.io()
        RxJavaPlugins.setInitIoSchedulerHandler { immediate }
        RxJavaPlugins.setInitComputationSchedulerHandler { immediate }
        RxJavaPlugins.setInitNewThreadSchedulerHandler { immediate }
        RxJavaPlugins.setInitSingleSchedulerHandler { immediate }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { immediate }

        whenever(marvelResult.id).thenReturn(1)

        whenever(sampleResponse.count).thenReturn(20)
        whenever(sampleResponse.total).thenReturn(1000)
        whenever(sampleResponse.results).thenReturn(
            listOf(
                marvelResult,
                marvelResult,
                marvelResult
            )
        )

        marvelRepository = object : MarvelRxRepository {
            private val recentList = mutableListOf<String>()

            override fun charactersRx(
                nameStartsWith: String?,
                offset: Int,
                limit: Int
            ): Single<BaseResponse<SampleResponse>> {
                return Single.just(BaseResponse(sampleResponse))
            }

            override fun setRecentList(recentList: List<String>): Single<Unit> {
                this.recentList.clear()
                this.recentList.addAll(recentList)
                return Single.just(Unit)
            }

            override fun getRecentList(): Single<List<String>> {
                return Single.just(recentList)
            }

        }
    }

    @Test
    fun characters() {
        runBlocking {
            assertEquals(
                marvelRepository.charactersRx().blockingGet(),
                BaseResponse(sampleResponse)
            )
        }
    }

    @Test
    fun setRecentList() {
        runBlocking {
            val list = listOf("123", "456", "789")
            marvelRepository.setRecentList(list)
            assertEquals(marvelRepository.getRecentList().blockingGet(), list)
        }
    }

    @Test
    fun getRecentList() {
        runBlocking {
            val list = listOf("123", "456", "789")
            marvelRepository.setRecentList(list)
            assertEquals(marvelRepository.getRecentList().blockingGet(), list)
        }
    }
}