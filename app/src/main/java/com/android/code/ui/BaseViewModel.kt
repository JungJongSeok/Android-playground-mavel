package com.android.code.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.code.util.SafetyMutableLiveData
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresActivityViewModel(val value: KClass<out BaseViewModel>)

open class BaseViewModel : ViewModel() {

    protected val _loading: SafetyMutableLiveData<Boolean> = SafetyMutableLiveData()
    val loading: LiveData<Boolean>
        get() = _loading

    protected val _error: SafetyMutableLiveData<Throwable> = SafetyMutableLiveData()
    val error: LiveData<Throwable>
        get() = _error

    protected val compositeDisposable = CompositeDisposable()

    fun launchDataLoad(
        onLoad: suspend CoroutineScope.() -> Unit,
        onError: (suspend (Exception) -> Unit)? = null,
        onFinally: (suspend CoroutineScope.() -> Unit)? = null
    ): Job {
        return launchDataLoad(_loading, onLoad, onError, onFinally)
    }

    fun launchDataLoad(
        loadingLiveData: MutableLiveData<Boolean>?,
        onLoad: suspend CoroutineScope.() -> Unit,
        onError: (suspend (Exception) -> Unit)? = null,
        onFinally: (suspend CoroutineScope.() -> Unit)? = null
    ): Job {
        return viewModelScope.launch {
            if (loadingLiveData?.value == true) {
                return@launch
            }

            try {
                loadingLiveData?.value = true
                onLoad(this)
            } catch (e: Exception) {
                onError?.invoke(e)
            } finally {
                loadingLiveData?.value = false
                onFinally?.invoke(this)
            }
        }
    }

    fun launchDataLoad(
        lock: AtomicBoolean,
        onLoad: suspend CoroutineScope.() -> Unit,
        onError: (suspend (Exception) -> Unit)? = null,
        onFinally: (suspend CoroutineScope.() -> Unit)? = null
    ): Job {
        return viewModelScope.launch {
            if (lock.getAndSet(true)) {
                return@launch
            }

            try {
                onLoad(this)
            } catch (e: Exception) {
                onError?.invoke(e)
            } finally {
                lock.set(false)
                onFinally?.invoke(this)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }
}
