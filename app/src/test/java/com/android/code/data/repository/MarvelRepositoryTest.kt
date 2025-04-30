package com.android.code.data.repository

import com.android.code.CoroutinesTestExtension
import com.android.code.InstantExecutorExtension
import com.android.code.getOrAwaitValue
import com.android.code.network.models.BaseResponse
import com.android.code.network.models.marvel.MarvelResult
import com.android.code.network.models.marvel.SampleResponse
import com.android.code.ui.search.SearchBaseViewModel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.system.measureTimeMillis

@kotlinx.coroutines.ExperimentalCoroutinesApi
@DisplayName("MarvelRepository 테스트")
@ExtendWith(InstantExecutorExtension::class, CoroutinesTestExtension::class)
class MarvelRepositoryTest {
    private lateinit var marvelRepository: MarvelRepository

    @Mock
    lateinit var marvelResult: MarvelResult

    @Mock
    lateinit var sampleResponse: SampleResponse

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)

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

        marvelRepository = object : MarvelRepository {
            private val recentList = mutableListOf<String>()

            override suspend fun characters(
                nameStartsWith: String?,
                offset: Int,
                limit: Int
            ): BaseResponse<SampleResponse> {
                return BaseResponse(sampleResponse)
            }

            override suspend fun setRecentList(recentList: List<String>) {
                this.recentList.clear()
                this.recentList.addAll(recentList)
            }

            override suspend fun getRecentList(): List<String> {
                return recentList
            }
        }
    }

    @Test
    fun characters() {
        runBlocking {
            assertEquals(marvelRepository.characters(), BaseResponse(sampleResponse))
        }
    }

    @Test
    fun setRecentList() {
        runBlocking {
            val list = listOf("123", "456", "789")
            marvelRepository.setRecentList(list)
            assertEquals(marvelRepository.getRecentList(), list)
        }
    }

    @Test
    fun getRecentList() {
        runBlocking {
            val list = listOf("123", "456", "789")
            marvelRepository.setRecentList(list)
            assertEquals(marvelRepository.getRecentList(), list)
        }
    }
}