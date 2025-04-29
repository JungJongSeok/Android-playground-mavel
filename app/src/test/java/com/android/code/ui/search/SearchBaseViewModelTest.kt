package com.android.code.ui.search

import com.android.code.CoroutinesTestExtension
import com.android.code.InstantExecutorExtension
import com.android.code.getOrAwaitValue
import com.android.code.network.models.BaseResponse
import com.android.code.network.models.marvel.MarvelResult
import com.android.code.network.models.marvel.SampleResponse
import com.android.code.data.repository.MarvelRepository
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.system.measureTimeMillis

@kotlinx.coroutines.ExperimentalCoroutinesApi
@DisplayName("SearchBaseViewModel 테스트")
@ExtendWith(InstantExecutorExtension::class, CoroutinesTestExtension::class)
internal class SearchBaseViewModelTest {
    private lateinit var searchBaseViewModel: SearchBaseViewModel

    private val recentList = mutableListOf("123", "456", "789")

    @Mock
    lateinit var marvelResult: MarvelResult

    @Mock
    lateinit var sampleResponse: SampleResponse

    @Mock
    lateinit var searchData: SearchBaseData

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        whenever(marvelResult.id).thenReturn(1)

        whenever(sampleResponse.count).thenReturn(20)
        whenever(sampleResponse.total).thenReturn(1000)
        whenever(sampleResponse.results).thenReturn(listOf(marvelResult, marvelResult, marvelResult))

        val marvelRepository: MarvelRepository = object : MarvelRepository {
            override suspend fun characters(
                nameStartsWith: String?,
                offset: Int,
                limit: Int,
            ): BaseResponse<SampleResponse> {
                return BaseResponse(sampleResponse)
            }

            override suspend fun setRecentList(recentList: List<String>) {
                this@SearchBaseViewModelTest.recentList.clear()
                this@SearchBaseViewModelTest.recentList.addAll(recentList)
            }

            override suspend fun getRecentList(): List<String> {
                return recentList
            }
        }
        searchBaseViewModel = SearchBaseViewModel(marvelRepository)
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
            val totalExecutionTime = measureTimeMillis {
                searchBaseViewModel.clickData(searchData)
                assertEquals(searchBaseViewModel.clickData.getOrAwaitValue(), searchData)
            }

            println("clickData() Total Time: $totalExecutionTime")
        }
    }
}