package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.notifier.DefaultNotifierSubscription
import com.github.funczz.kotlin.notifier.Notifier
import java.util.*
import java.util.concurrent.Executor

/**
 * 複数のプロパティを双方向バインドするプロパティ
 * @author funczz
 */
open class BiDiNotifierProperty<V : Any>(

    /**
     * 初期値
     */
    initialValue: V,

    /**
     * Notifier
     */
    notifier: Notifier,

    /**
     * サブスクリプションのname
     */
    name: String,

    /**
     * Notifierを実行するExecutor
     */
    executor: Optional<Executor>,

    ) : ReadWriteNotifierProperty<V> by RWNotifierProperty(initialValue = initialValue, notifier = notifier) {

    /**
     * サブスクライバ
     */
    val subscriber = BiDiNotifierPropertySubscriber()

    /**
     * サブスクリプション
     */
    val subscription = DefaultNotifierSubscription(
        subscriber = subscriber,
        name = name,
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
    inner class BiDiNotifierPropertySubscriber : NotifierPropertySubscriber<V>() {

        @Suppress("UNCHECKED_CAST")
        override fun onNext(item: Any) {
            val result = setValue(
                value = item as V,
                name = RWNotifierProperty.DO_NOT_POST_PATTERN,
                executor = if (subscription.executor.isPresent) subscription.executor.get() else null
            )
            if (result) super.onNext(item)
        }
    }
}