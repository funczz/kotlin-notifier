package com.github.funczz.kotlin.notifier.wrapper

import com.github.funczz.kotlin.notifier.DefaultNotifierSubscription
import com.github.funczz.kotlin.notifier.Notifier
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Flow

/**
 * @author funczz
 */
open class NotifierWrapper<T, I>(

    /**
     * オブジェクト T
     */
    private val target: T,

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

    ) : INotifierWrapper<T, I> {

    private var _onUpdate: (T, I) -> Unit = { _, _ -> }

    private var _onError: (Throwable) -> Unit = {}

    private var _onComplete: () -> Unit = {}

    private val subscription = DefaultNotifierSubscription(
        subscriber = NotifierWrapperSubscriber(),
        id = id,
        executor = Optional.ofNullable(executor)
    )

    /**
     * オブジェクトTに適用する関数を代入する
     * @param function オブジェクトTにインプットデータIを適用する関数
     * @return 自身を返却する
     */
    fun onUpdate(function: (T, I) -> Unit): NotifierWrapper<T, I> {
        _onUpdate = function
        return this
    }

    override fun onUpdate(input: I) {
        _onUpdate(target, input)
    }

    override fun subscribe(notifier: Notifier): NotifierWrapper<T, I> {
        this.notifier = notifier
        this.notifier?.subscribe(subscription = subscription, executor = executor)
        return this
    }

    override fun unsubscribe(): NotifierWrapper<T, I> {
        notifier?.unsubscribe(subscription = subscription, executor = executor)
        notifier = null
        return this
    }

    override fun cancel(): NotifierWrapper<T, I> {
        notifier?.cancel(subscription = subscription, executor = executor)
        notifier = null
        return this
    }

    override fun onError(function: (Throwable) -> Unit): NotifierWrapper<T, I> {
        _onError = function
        return this
    }

    override fun onComplete(function: () -> Unit): NotifierWrapper<T, I> {
        _onComplete = function
        return this
    }


    /**
     * @author funczz
     */
    inner class NotifierWrapperSubscriber : Flow.Subscriber<Any> {

        @Suppress("UNCHECKED_CAST")
        override fun onNext(item: Any) {
            onUpdate(input = item as I)
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