package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.notifier.DefaultNotifierSubscription
import com.github.funczz.kotlin.notifier.Notifier
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Flow
import java.util.concurrent.Flow.Subscriber

/**
 * 複数のプロパティを双方向バインドする
 * @author funczz
 */
class BdNotifierProperty<V : Any>(

    /**
     * 初期値
     */
    initialValue: V,
    /**
     * Notifier
     */
    override val notifier: Notifier,

    /**
     * サブスクリプションid
     */
    id: String,

    /**
     * Notifierを実行するExecutor
     */
    executor: Optional<Executor>,

    ) : NotifierProperty<V> {

    /**
     * サブスクライバ
     */
    val subscriber = BdNotifierPropertySubscriber()

    /**
     * サブスクリプション
     */
    val subscription = DefaultNotifierSubscription(
        subscriber = subscriber,
        id = id,
        executor = executor,
    )

    /**
     * 内部で保持する値
     */
    private var _value: V = initialValue

    /***
     * init処理
     */
    init {
        notifier.subscribe(
            subscription = subscription,
            executor = if (executor.isPresent) executor.get() else null,
        )
    }

    override fun getValue(): V {
        return _value
    }

    override fun setValue(value: V, id: Regex, executor: Executor?) {
        if (!setValuePredicate(value = value)) return
        postValue(id = id, executor = executor)
    }

    override fun postValue(id: Regex, executor: Executor?) {
        notifier.post(item = _value as Any, id = id, executor = executor)
    }

    private fun setValuePredicate(value: V): Boolean {
        if (_value == value) return false
        _value = value
        return true
    }

    /**
     * サブスクライバ
     * @author funczz
     */
    inner class BdNotifierPropertySubscriber : Subscriber<Any> {

        private var _onError: (Throwable) -> Unit = {}

        private var _onComplete: () -> Unit = {}

        fun onError(function: (Throwable) -> Unit): BdNotifierPropertySubscriber {
            _onError = function
            return this
        }

        fun onComplete(function: () -> Unit): BdNotifierPropertySubscriber {
            _onComplete = function
            return this
        }

        override fun onSubscribe(subscription: Flow.Subscription) {
        }

        @Suppress("UNCHECKED_CAST")
        override fun onNext(item: Any) {
            setValuePredicate(value = item as V)
        }

        override fun onError(throwable: Throwable) {
            _onError(throwable)
        }

        override fun onComplete() {
            _onComplete()
        }

    }
}