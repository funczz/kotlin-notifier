package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.notifier.DefaultNotifierSubscription
import com.github.funczz.kotlin.notifier.Notifier
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Flow
import java.util.concurrent.Flow.Subscriber

/**
 * 代入された値をNotifierにpostするプロパティ
 * @author funczz
 */
open class NotifierProperty<V>(

    /**
     * 初期値
     * この値はpostされない
     */
    initialValue: V,

    /**
     * サブスクリプションのid
     */
    id: String,

    /**
     * 処理を実行するExecutor
     */
    private val executor: Executor? = null,

    /**
     * Notifier
     * インスタンスが代入されていない場合は値をpostしない
     */
    private var notifier: Notifier? = null,

    ) : INotifierProperty<V> {

    private var _value: V = initialValue

    private var _onNext: (V) -> Unit = {}

    private var _onError: (Throwable) -> Unit = {}

    private var _onComplete: () -> Unit = {}

    private val subscription = DefaultNotifierSubscription(
        subscriber = NotifierPropertySubscriber(),
        id = id,
        executor = Optional.ofNullable(executor)
    )

    override fun subscribe(notifier: Notifier): INotifierProperty<V> {
        this.notifier = notifier
        this.notifier?.subscribe(subscription = subscription, executor = executor)
        return this
    }

    override fun unsubscribe(): INotifierProperty<V> {
        notifier?.unsubscribe(subscription = subscription, executor = executor)
        notifier = null
        return this
    }

    override fun cancel(): INotifierProperty<V> {
        notifier?.cancel(subscription = subscription, executor = executor)
        notifier = null
        return this
    }

    override fun onNext(function: (V) -> Unit): NotifierProperty<V> {
        _onNext = function
        return this
    }

    override fun onError(function: (Throwable) -> Unit): NotifierProperty<V> {
        _onError = function
        return this
    }

    override fun onComplete(function: () -> Unit): NotifierProperty<V> {
        _onComplete = function
        return this
    }

    override fun getValue(): V {
        return _value
    }

    override fun setValue(value: V) {
        _value = value
        notifier?.post(
            item = _value as Any,
            id = subscription.id.toRegex(),
            executor = executor
        )
    }

    /**
     * @author funczz
     */
    inner class NotifierPropertySubscriber : Subscriber<Any> {

        @Suppress("UNCHECKED_CAST")
        override fun onNext(item: Any) {
            _onNext(item as V)
        }

        override fun onSubscribe(subscription: Flow.Subscription) {
        }

        override fun onError(throwable: Throwable) {
            _onError(throwable)
        }

        override fun onComplete() {
            _onComplete()
        }
    }
}