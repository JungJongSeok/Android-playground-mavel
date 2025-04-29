package com.android.code.ui.search

import androidx.lifecycle.LiveData
import com.android.code.data.repository.MarvelRepository
import com.android.code.ui.BaseViewModel
import com.android.code.util.empty
import com.android.code.util.SafetyMutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class SearchBaseViewModel @Inject constructor(private val marvelRepository: MarvelRepository) :
    BaseViewModel(),
    SearchViewModelInput, SearchViewModelOutput {

    val inputs: SearchViewModelInput by lazy {
        this
    }
    val outputs: SearchViewModelOutput by lazy {
        this
    }

    private val _responseData = SafetyMutableLiveData<Pair<List<SearchData>, Boolean>>()
    override val responseData: LiveData<Pair<List<SearchData>, Boolean>>
        get() = _responseData

    private val _clickData = SafetyMutableLiveData<SearchData>()
    override val clickData: LiveData<SearchData>
        get() = _clickData

    private val _searchedData = SafetyMutableLiveData<SearchBaseData>()
    override val searchedData: LiveData<SearchBaseData>
        get() = _searchedData

    private val _searchedText = SafetyMutableLiveData<String>()
    override val searchedText: LiveData<String>
        get() = _searchedText

    private val _refreshedSwipeRefreshLayout = SafetyMutableLiveData<Boolean>()
    override val refreshedSwipeRefreshLayout: LiveData<Boolean>
        get() = _refreshedSwipeRefreshLayout

    private val initializeDataList = ArrayList<SearchData>()
    private var initializeOffset = 0
    private var initializeTotal = 0

    private var currentOffset = 0
    private var currentTotal = 0
    private var currentText: String? = null

    override fun initData(isRefreshing: Boolean) {
        launchDataLoad(
            if (isRefreshing) {
                _refreshedSwipeRefreshLayout
            } else {
                _loading
            },
            onLoad = {
                initSearchData()
                val recentSearchList =
                    marvelRepository.getRecentList().run { SearchRecentData(this) }
                val searchDataList = marvelRepository.characters().data
                    .apply {
                        currentOffset = count
                        currentTotal = total
                    }.results?.map { SearchBaseData(it) }
                    ?: emptyList()

                val totalList = if (recentSearchList.recentList.isNotEmpty()) {
                    listOf(recentSearchList) + searchDataList
                } else {
                    searchDataList
                }

                initializeDataList.clear()
                initializeDataList.addAll(searchDataList)
                initializeOffset = currentOffset
                initializeTotal = currentTotal
                _responseData.setValueSafety(totalList to true)
            },
            onError = {
                _error.setValueSafety(it)
            }
        )
    }

    private var searchTask: Deferred<List<SearchBaseData>?>? = null
    override fun search(text: String, isRefreshing: Boolean) {
        launchDataLoad(
            if (isRefreshing) {
                _refreshedSwipeRefreshLayout
            } else {
                null
            },
            onLoad = {
                searchTask?.cancel()
                initSearchData()
                currentText = text
                if (text.isEmpty()) {
                    val recentSearchList =
                        marvelRepository.getRecentList().run { SearchRecentData(this) }
                    val totalList = if (recentSearchList.recentList.isNotEmpty()) {
                        listOf(recentSearchList) + initializeDataList
                    } else {
                        initializeDataList
                    }
                    currentOffset = initializeOffset
                    currentTotal = initializeTotal
                    _searchedText.setValueSafety(String.empty())
                    _responseData.setValueSafety(totalList to true)
                    return@launchDataLoad
                }
                searchTask = async {
                    kotlin.runCatching {
                        delay(300)
                        marvelRepository.characters(nameStartsWith = text).data
                            .apply {
                                currentOffset = count
                                currentTotal = total
                            }.results?.map { SearchBaseData(it) }
                    }.getOrNull()
                }
                searchTask?.await()?.run {
                    marvelRepository.setRecentList(
                        (listOf(text) + (marvelRepository.getRecentList())).distinct()
                    )
                    val recentSearchList = marvelRepository.getRecentList().run { SearchRecentData(this) }

                    val totalList = if (recentSearchList.recentList.isNotEmpty()) {
                        listOf(recentSearchList) + this
                    } else {
                        this
                    }

                    _searchedText.setValueSafety(text)
                    _responseData.setValueSafety(totalList to true)
                }
            },
            onError = {
                if (it is CancellationException) {
                    return@launchDataLoad
                }
                _error.setValueSafety(it)
            }
        )
    }

    override fun canSearchMore(): Boolean {
        return currentOffset < currentTotal
    }

    override fun searchMore() {
        launchDataLoad(
            onLoad = {
                val searchDataList = marvelRepository.characters(
                    nameStartsWith = currentText,
                    offset = currentOffset
                ).data.apply {
                    currentOffset += count
                    currentTotal = total
                }.results?.map { SearchBaseData(it) } ?: emptyList()

                val totalList = (_responseData.value?.first ?: emptyList()) + searchDataList

                _responseData.setValueSafety(totalList to false)
            },
            onError = {
                _error.setValueSafety(it)
            }
        )
    }

    private fun initSearchData() {
        currentOffset = 0
        currentTotal = 0
        currentText = null
    }

    override fun removeRecentSearch(text: String) {
        launchDataLoad(
            onLoad = {
                val recentSearchList = marvelRepository.getRecentList().filter { it != text }
                marvelRepository.setRecentList(recentSearchList)
                if (recentSearchList.isNullOrEmpty()) {
                    val previousDataList =
                        _responseData.value?.first?.filterIsInstance<SearchBaseData>()
                            ?: emptyList()
                    _responseData.setValueSafety(previousDataList to true)
                }
            },
            onError = {
                _error.setValueSafety(it)
            }
        )
    }

    override fun clickData(searchData: SearchData) {
        _clickData.setValueSafety(searchData)
        if (searchData is SearchBaseData) {
            _searchedData.setValueSafety(searchData)
        }
    }
}

interface SearchViewModelInput {
    fun initData(isRefreshing: Boolean = false)
    fun search(text: String, isRefreshing: Boolean = false)
    fun canSearchMore(): Boolean
    fun searchMore()
    fun removeRecentSearch(text: String)
    fun clickData(searchData: SearchData)
}

interface SearchViewModelOutput {
    val responseData: LiveData<Pair<List<SearchData>, Boolean>>
    val clickData: LiveData<SearchData>
    val searchedData: LiveData<SearchBaseData>
    val searchedText: LiveData<String>
    val refreshedSwipeRefreshLayout: LiveData<Boolean>
}