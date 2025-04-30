package com.android.code.util

import android.os.Looper
import androidx.core.util.Consumer
import androidx.lifecycle.MutableLiveData

/**
 * 안전하게 LiveData 를 사용한다.
 * setValueSafety 를 이용하면 thread 상태에 따라 알맞게 적용 가능
 */
class SafetyMutableLiveData<T> : MutableLiveData<T>(), Consumer<T>, (T) -> Unit {

    fun setValueSafety(value: T) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            setValue(value)
        } else {
            postValue(value)
        }
    }

    override fun accept(value: T) {
        setValueSafety(value)
    }

    override fun invoke(value: T) {
        setValueSafety(value)
    }
}