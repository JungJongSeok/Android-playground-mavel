package com.android.code.ui.main

import androidx.lifecycle.LiveData
import com.android.code.ui.BaseViewModel
import com.android.code.ui.search.SearchBaseData
import com.android.code.ui.search.SearchData
import com.android.code.util.SafetyMutableLiveData

class MainViewModel : BaseViewModel(),
    MainViewModelInput, MainViewModelOutput {

    val inputs: MainViewModelInput = this
    val outputs: MainViewModelOutput = this

    private val _scrollToTop = SafetyMutableLiveData<Int>()
    override val scrollToTop: LiveData<Int>
        get() = _scrollToTop

    private val _clickData = SafetyMutableLiveData<SearchBaseData>()
    override val clickData: LiveData<SearchBaseData>
        get() = _clickData

    override fun scrollToTop(@MainActivity.Page page: Int) {
        _scrollToTop.setValueSafety(page)
    }

    override fun clickData(searchData: SearchData) {
        if (searchData is SearchBaseData) {
            _clickData.setValueSafety(searchData)
        }
    }
}

interface MainViewModelInput {
    fun scrollToTop(@MainActivity.Page page: Int)
    fun clickData(searchData: SearchData)
}

interface MainViewModelOutput {
    val scrollToTop: LiveData<Int>
    val clickData: LiveData<SearchBaseData>
}