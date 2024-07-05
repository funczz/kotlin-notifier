package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.notifier.DefaultNotifierSubscription
import com.github.funczz.kotlin.notifier.Notifier
import java.util.*
import java.util.concurrent.Executor

/**
 * Notifierからpostされた値を受け取るプロパティの実装
 * @author funczz
 */
open class RONotifierProperty<V : Any>(

    /**
     * 初期値
     */
    initialValue: V,

    /**
     * Notifier
     */
    notifier: Notifier,

    /**
     * サブスクリプションid
     */
    id: String,

    /**
     * Notifierを実行するExecutor
     */
    executor: Optional<Executor>,

    ) : ReadWriteNotifierProperty<V> by RWNotifierProperty(initialValue = initialValue, notifier = notifier) {

    /**
     * サブスクライバ
     */
    val subscriber = RONotifierPropertySubscriber()

    /**
     * サブスクリプション
     */
    val subscription = DefaultNotifierSubscription(
        subscriber = subscriber,
        id = id,
        executor = executor,
    )

    /**
     * init処理
     */
    init {
        notifier.subscribe(
            subscription = subscription,
            executor = if (executor.isPresent) executor.get() else null,
        )
    }

    /**
     * (インナークラス) サブスクライバ
     * @author funczz
     */
    inner class RONotifierPropertySubscriber : NotifierPropertySubscriber<V>() {

        @Suppress("UNCHECKED_CAST")
        override fun onNext(item: Any) {
            val result = setValue(
                value = item as V,
                id = RWNotifierProperty.DO_NOT_POST_ID_PATTERN,
                executor = if (subscription.executor.isPresent) subscription.executor.get() else null
            )
            if (result) super.onNext(item)
        }
    }
}