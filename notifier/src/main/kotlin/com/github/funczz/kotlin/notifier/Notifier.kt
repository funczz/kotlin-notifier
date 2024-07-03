package com.github.funczz.kotlin.notifier

import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.Flow
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * イベントバス
 * @author funczz
 */
class Notifier {

    /**
     * 更新処理を行う際のロック
     */
    private val lock = ReentrantLock()

    /**
     * サブスクリプションのリスト
     */
    private val _subscriptions = CopyOnWriteArrayList<NotifierSubscription>()

    /**
     * 外部に公開するサブスクリプションのリスト
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val subscriptions: List<NotifierSubscription>
        get() = _subscriptions.toList()

    /**
     * subscribe前に実行する関数
     */
    private var _subscribeBefore: (NotifierSubscription) -> Unit = {}

    /**
     * subscribe後に実行する関数
     */
    private var _subscribeAfter: (NotifierSubscription) -> Unit = {}

    /**
     * unsubscribe前に実行する関数
     */
    private var _unsubscribeBefore: (NotifierSubscription) -> Unit = {}

    /**
     * unsubscribe後に実行する関数
     */
    private var _unsubscribeAfter: (NotifierSubscription) -> Unit = {}

    /**
     * post前に実行する関数
     */
    private var _postBefore: (NotifierSubscription) -> Unit = {}

    /**
     * post後に実行する関数
     */
    private var _postAfter: (NotifierSubscription) -> Unit = {}

    /**
     * cancel前に実行する関数
     */
    private var _cancelBefore: (NotifierSubscription) -> Unit = {}

    /**
     * cancel後に実行する関数
     */
    private var _cancelAfter: (NotifierSubscription) -> Unit = {}

    /**
     * subscribe前に実行する関数を代入する
     * @param function 関数
     * @return Notifier
     */
    fun subscribeBefore(function: (NotifierSubscription) -> Unit): Notifier {
        _subscribeBefore = function
        return this
    }

    /**
     * subscribe後に実行する関数を代入する
     * @param function 関数
     * @return Notifier
     */
    fun subscribeAfter(function: (NotifierSubscription) -> Unit): Notifier {
        _subscribeAfter = function
        return this
    }

    /**
     * unsubscribe前に実行する関数を代入する
     * @param function 関数
     * @return Notifier
     */
    fun unsubscribeBefore(function: (NotifierSubscription) -> Unit): Notifier {
        _unsubscribeBefore = function
        return this
    }

    /**
     * unsubscribe後に実行する関数を代入する
     * @param function 関数
     * @return Notifier
     */
    fun unsubscribeAfter(function: (NotifierSubscription) -> Unit): Notifier {
        _unsubscribeAfter = function
        return this
    }

    /**
     * post前に実行する関数を代入する
     * @param function 関数
     * @return Notifier
     */
    fun postBefore(function: (NotifierSubscription) -> Unit): Notifier {
        _postBefore = function
        return this
    }

    /**
     * post後に実行する関数を代入する
     * @param function 関数
     * @return Notifier
     */
    fun postAfter(function: (NotifierSubscription) -> Unit): Notifier {
        _postAfter = function
        return this
    }

    /**
     * cancel前に実行する関数を代入する
     * @param function 関数
     * @return Notifier
     */
    fun cancelBefore(function: (NotifierSubscription) -> Unit): Notifier {
        _cancelBefore = function
        return this
    }

    /**
     * cancel後に実行する関数を代入する
     * @param function 関数
     * @return Notifier
     */
    fun cancelAfter(function: (NotifierSubscription) -> Unit): Notifier {
        _cancelAfter = function
        return this
    }

    /**
     * サブスクリプションをリストに加える
     * @param subscription サブスクリプション
     * @param executor subscribe処理するExecutor
     */
    fun subscribe(subscription: NotifierSubscription, executor: Executor? = null) = lock.withLock {
        val runnable = Runnable {
            _subscribeBefore(subscription)
            try {
                if (_subscriptions.any { it != subscription && it.subscriber == subscription.subscriber }) {
                    throw IllegalArgumentException("Duplicate subscriber.")
                }
                when (_subscriptions.addIfAbsent(subscription)) {
                    true -> {
                        subscription.onCall(notifier = this)
                        subscription.subscriber.onSubscribe(subscription)
                    }

                    else -> {}
                }
            } catch (th: Throwable) {
                unsubscribePredicate(subscription = subscription, throwable = Optional.ofNullable(th))
            } finally {
                _subscribeAfter(subscription)
            }
        }
        executor?.execute(runnable) ?: runnable.run()
    }

    /**
     * サブスクリプションをリストから削除する
     * @param subscription サブスクリプション
     * @return リストから削除されたなら真を、それ以外は偽を返却する
     */
    fun unsubscribe(subscription: NotifierSubscription): Boolean = lock.withLock {
        var result = false
        for (s in _subscriptions.filter { it == subscription }) {
            result = unsubscribePredicate(subscription = s, throwable = Optional.empty())
            break
        }
        result
    }

    /**
     * サブスクライバをリストから削除する
     * @param subscriber サブスクライバ
     * @return リストから削除されたなら真を、それ以外は偽を返却する
     */
    fun unsubscribe(subscriber: Flow.Subscriber<in Any>): Boolean = lock.withLock {
        var result = false
        for (s in _subscriptions.filter { it.subscriber == subscriber }) {
            result = unsubscribePredicate(subscription = s, throwable = Optional.empty())
            break
        }
        result
    }

    /**
     * サブスクリプションをリストから削除する
     * @param id 対象を抽出するidの正規表現
     * @return 削除されたサブスクリプションの個数を返却する
     */
    fun unsubscribe(id: Regex): Int = lock.withLock {
        var result = 0
        for (s in _subscriptions.filter { it.id.matches(id) }) {
            if (unsubscribePredicate(subscription = s, throwable = Optional.empty())) {
                result += 1
            }
        }
        result
    }

    /**
     * 全てのサブスクリプションをリストから削除する
     * @return 削除されたサブスクリプションの個数を返却する
     */
    fun unsubscribeAll(): Int = lock.withLock {
        var result = 0
        for (s in _subscriptions.toList()) {
            if (unsubscribePredicate(subscription = s, throwable = Optional.empty())) {
                result += 1
            }
        }
        result
    }

    /**
     * サブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     * サブスクリプションから呼び出される
     * @param subscription サブスクリプション
     */
    fun onCancel(subscription: NotifierSubscription) {
        cancel(subscription = subscription)
    }

    /**
     * サブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     * @param subscription サブスクリプション
     * @return リストから削除されたなら真を、それ以外は偽を返却する
     */
    fun cancel(subscription: NotifierSubscription): Boolean = lock.withLock {
        val counter = cancelPredicate(subscriptions = _subscriptions.filter { it == subscription })
        counter > 0
    }

    /**
     * サブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     * @param subscriber サブスクライバ
     * @return リストから削除されたなら真を、それ以外は偽を返却する
     */
    fun cancel(subscriber: Flow.Subscriber<in Any>): Boolean = lock.withLock {
        val counter = cancelPredicate(subscriptions = _subscriptions.filter { it.subscriber == subscriber })
        counter > 0
    }

    /**
     * サブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     * @param id idの正規表現
     * @return 削除されたサブスクリプションの個数を返却する
     */
    fun cancel(id: Regex): Int = lock.withLock {
        cancelPredicate(subscriptions = _subscriptions.filter { it.id.matches(id) })
    }

    /**
     * 全てのサブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     */
    fun cancelAll(): Int = lock.withLock {
        cancelPredicate(subscriptions = _subscriptions.toList())
    }

    /**
     * idがマッチしたサブスクリプションのサブスクライバへアイテムを送信する
     * @param item アイテム
     * @param id idの正規表現
     * @param executor post処理するExecutor
     */
    fun post(item: Any, id: Regex = Regex(".*"), executor: Executor? = null) = lock.withLock {
        val runnable = Runnable {
            for (s in _subscriptions.filter { it.id.matches(regex = id) }) {
                _postBefore(s)
                val runnable = Runnable {
                    try {
                        s.subscriber.onNext(item)
                    } catch (th: Throwable) {
                        unsubscribePredicate(subscription = s, throwable = Optional.ofNullable(th))
                    }
                }
                if (s.executor.isPresent) {
                    s.executor.get().execute(runnable)
                } else {
                    runnable.run()
                }
                _postAfter(s)
            }
        }
        executor?.execute(runnable) ?: runnable.run()
    }

    private fun unsubscribePredicate(subscription: NotifierSubscription, throwable: Optional<Throwable>): Boolean {
        _unsubscribeBefore(subscription)
        if (throwable.isPresent) {
            subscription.subscriber.onError(throwable.get())
        } else {
            subscription.subscriber.onComplete()
        }
        val result = cancel(subscription = subscription)
        _unsubscribeAfter(subscription)
        return result
    }

    private fun cancelPredicate(subscriptions: List<NotifierSubscription>): Int {
        var result = 0
        for (s in subscriptions) {
            _cancelBefore(s)
            if (_subscriptions.removeIf { it == s }) {
                result += 1
            }
            _cancelAfter(s)
        }
        return result
    }

    companion object {

        private var instance: Notifier? = null

        /**
         * Notifierのシングルトン
         */
        @Suppress("Unused")
        @JvmStatic
        fun getDefault() = instance ?: synchronized(this) {
            instance ?: Notifier().also {
                instance = it
            }
        }

    }

}
