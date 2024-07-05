package com.github.funczz.kotlin.notifier.property

import java.util.concurrent.Flow

/**
 * ReadOnlyNotifierPropertyのインナークラス定義に使用する
 * @author funczz
 */
open class NotifierPropertySubscriber<V : Any> : Flow.Subscriber<Any> {

    private var _onNext: (V) -> Unit = {}

    private var _onError: (Throwable) -> Unit = {}

    private var _onComplete: () -> Unit = {}

    /**
     * サブスクライバのonNextで実行する処理を定義する
     * @param function サブスクライバのonNextで実行する関数
     */
    fun onNext(function: (V) -> Unit): NotifierPropertySubscriber<V> {
        _onNext = function
        return this
    }

    /**
     * サブスクライバのonErrorで実行する処理を定義する
     * @param function サブスクライバのonErrorで実行する関数
     */
    fun onError(function: (Throwable) -> Unit): NotifierPropertySubscriber<V> {
        _onError = function
        return this
    }

    /**
     * サブスクライバのonCompleteで実行する処理を定義する
     * @param function サブスクライバのonCompleteで実行する関数
     */
    fun onComplete(function: () -> Unit): NotifierPropertySubscriber<V> {
        _onComplete = function
        return this
    }

    override fun onSubscribe(subscription: Flow.Subscription) {
    }

    @Suppress("UNCHECKED_CAST")
    override fun onNext(item: Any) {
        _onNext(item as V)
    }

    override fun onError(throwable: Throwable) {
        _onError(throwable)
    }

    override fun onComplete() {
        _onComplete()
    }

}