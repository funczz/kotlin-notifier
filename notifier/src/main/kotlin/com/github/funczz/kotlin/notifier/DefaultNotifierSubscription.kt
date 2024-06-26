package com.github.funczz.kotlin.notifier

import java.util.*
import java.util.concurrent.Flow
import java.util.concurrent.ThreadPoolExecutor

/**
 * NotifierSubscription の実装
 * @author funczz
 */
open class DefaultNotifierSubscription(

    override val subscriber: Flow.Subscriber<Any>,

    override val id: String = "",

    override val executor: Optional<ThreadPoolExecutor> = Optional.empty(),

    ) : NotifierSubscription {

    /**
     * requestメソッドが呼び出された際に実行される関数
     */
    private var _requestCode: (Long) -> Unit = {}

    /**
     * requestメソッドが呼び出された際に実行される関数を代入する
     * @param function 代入する関数
     */
    fun requestCode(function: (Long) -> Unit) {
        _requestCode = function
    }

    override fun request(n: Long) {
        if (n <= 0L) {
            throw IllegalArgumentException("Subscription request is less than or equal to zero: n=$n")
        }
        _requestCode(n)
    }

    override fun cancel() {
        Notifier.cancel(subscription = this)
    }

}