package com.android.code.ui.search

import com.android.code.CoroutinesTestExtension
import com.android.code.InstantExecutorExtension
import com.android.code.getOrAwaitValue
import com.android.code.network.models.BaseResponse
import com.android.code.network.models.marvel.MarvelResult
import com.android.code.network.models.marvel.SampleResponse
import com.android.code.data.repository.MarvelRxRepository
import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.internal.schedulers.ExecutorScheduler
import io.reactivex.rxjava3.internal.schedulers.TrampolineScheduler
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

@kotlinx.coroutines.ExperimentalCoroutinesApi
@DisplayName("SearchRxBaseViewModel 테스트")
@ExtendWith(InstantExecutorExtension::class, CoroutinesTestExtension::class)
internal class SearchRxBaseViewModelTest {
    private lateinit var searchBaseViewModel: SearchRxBaseViewModel

    private val recentList = mutableListOf("123", "456", "789")
    @BeforeEach
    fun setUp() {
        val immediate: Scheduler = Schedulers.io()

        RxJavaPlugins.setInitIoSchedulerHandler { immediate }
        RxJavaPlugins.setInitComputationSchedulerHandler { immediate }
        RxJavaPlugins.setInitNewThreadSchedulerHandler { immediate }
        RxJavaPlugins.setInitSingleSchedulerHandler { immediate }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { immediate }

        val marvelResult: MarvelResult = mock {
            on { id } doReturn 1
        }
        val sampleResponse: SampleResponse = mock {
            on { count } doReturn 20
            on { total } doReturn 1000
            on { results } doReturn listOf(marvelResult, marvelResult, marvelResult)
        }
        val marvelRepository: MarvelRxRepository = object : MarvelRxRepository {
            override fun charactersRx(
                nameStartsWith: String?,
                offset: Int,
                limit: Int,
            ): Single<BaseResponse<SampleResponse>> {
                return Single.fromCallable { BaseResponse(sampleResponse) }
            }

            override fun setRecentList(recentList: List<String>): Single<Unit> {
                return Single.fromCallable {
                    this@SearchRxBaseViewModelTest.recentList.clear()
                    this@SearchRxBaseViewModelTest.recentList.addAll(recentList)
                }
            }

            override fun getRecentList(): Single<List<String>> {
                return Single.fromCallable { recentList }
            }
        }
        searchBaseViewModel = SearchRxBaseViewModel(marvelRepository)
    }

    @Test
    @DisplayName("characters api 의 호출한다.")
    fun initData() {
        runBlocking {
            val totalExecutionTime = measureTimeMillis {
                searchBaseViewModel.initData()
                assertEquals(searchBaseViewModel.responseData.getOrAwaitValue().first.size, 4)
            }

            println("initData() Total Time: $totalExecutionTime")
        }
    }

    @Test
    @DisplayName("300 millis 의 threshold 의 characters api 를 검증한다.")
    fun search() {
        runBlocking {
            val totalExecutionTime = measureTimeMillis {
                searchBaseViewModel.initData()
                searchBaseViewModel.search("")
                assertEquals(searchBaseViewModel.searchedText.getOrAwaitValue(), "")
                searchBaseViewModel.search("spi")
                delay(1)
                searchBaseViewModel.search("spider")
                assertEquals(searchBaseViewModel.searchedText.getOrAwaitValue(), "")
                delay(500)
                assertEquals(searchBaseViewModel.searchedText.getOrAwaitValue(), "spider")
                searchBaseViewModel.search("spider-m")
                delay(1)
                assertEquals(searchBaseViewModel.searchedText.getOrAwaitValue(), "spider")
                searchBaseViewModel.search("spider-man")
                delay(500)
                assertEquals(searchBaseViewModel.searchedText.getOrAwaitValue(), "spider-man")
            }

            println("search() Total Time: $totalExecutionTime")
        }
    }

    @Test
    @DisplayName("characters api 의 pagination 을 검증한다.")
    fun searchMore() {
        runBlocking {
            launch {
                val totalExecutionTime = measureTimeMillis {
                    searchBaseViewModel.initData()
                    assertEquals(searchBaseViewModel.responseData.getOrAwaitValue().first.size, 4)
                    delay(500)
                    searchBaseViewModel.searchMore()
                    delay(500)
                    assertEquals(searchBaseViewModel.responseData.getOrAwaitValue().first.size, 7)
                }

                println("searchMore() Total Time: $totalExecutionTime")
            }
        }
    }

    @Test
    @DisplayName("characters api 의 pagination 이 가능한지 여부를 측정한다. Offset < Total")
    fun canSearchMore() {
        runBlocking {
            val totalExecutionTime = measureTimeMillis {
                assertEquals(searchBaseViewModel.canSearchMore(), false)
                searchBaseViewModel.initData()
                delay(500)
                assertEquals(searchBaseViewModel.canSearchMore(), true)
            }

            println("removeRecentSearch() Total Time: $totalExecutionTime")
        }
    }

    @Test
    @DisplayName("최근 검색어의 단어 하나를 제거한다.")
    fun removeRecentSearch() {
        runBlocking {
            val totalExecutionTime = measureTimeMillis {
                searchBaseViewModel.removeRecentSearch("123")
                assertEquals(recentList, listOf("456", "789"))
            }

            println("removeRecentSearch() Total Time: $totalExecutionTime")
        }
    }

    @Test
    @DisplayName("click 한 데이터를 검증한다.")
    fun clickData() {
        runBlocking {
            val searchData = mock<SearchData>()
            val totalExecutionTime = measureTimeMillis {
                searchBaseViewModel.clickData(searchData)
                assertEquals(searchBaseViewModel.clickData.getOrAwaitValue(), searchData)
            }

            println("clickData() Total Time: $totalExecutionTime")
        }
    }

    @AfterEach
    fun done() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }
}