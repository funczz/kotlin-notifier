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
     * サブスクリプションのリスト
     */
    private val _subscriptions = CopyOnWriteArrayList<NotifierSubscription>()

    /**
     * 外部に公開するサブスクリプションのリスト
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val subscriptions: List<NotifierSubscription>
        get() = _subscriptions.toList()

    private var _subscribeFirst: (NotifierSubscription) -> Unit = {}

    private var _subscribeLast: (NotifierSubscription) -> Unit = {}

    private var _unsubscribeFirst: (NotifierSubscription) -> Unit = {}

    private var _unsubscribeLast: (NotifierSubscription) -> Unit = {}

    private var _postFirst: (NotifierSubscription) -> Unit = {}

    private var _postLast: (NotifierSubscription) -> Unit = {}

    private var _cancelFirst: (NotifierSubscription) -> Unit = {}

    private var _cancelLast: (NotifierSubscription) -> Unit = {}

    fun subscribeFirst(function: (NotifierSubscription) -> Unit): Notifier {
        _subscribeFirst = function
        return this
    }

    fun subscribeLast(function: (NotifierSubscription) -> Unit): Notifier {
        _subscribeLast = function
        return this
    }

    fun unsubscribeFirst(function: (NotifierSubscription) -> Unit): Notifier {
        _unsubscribeFirst = function
        return this
    }

    fun unsubscribeLast(function: (NotifierSubscription) -> Unit): Notifier {
        _unsubscribeLast = function
        return this
    }

    fun postFirst(function: (NotifierSubscription) -> Unit): Notifier {
        _postFirst = function
        return this
    }

    fun postLast(function: (NotifierSubscription) -> Unit): Notifier {
        _postLast = function
        return this
    }

    fun cancelFirst(function: (NotifierSubscription) -> Unit): Notifier {
        _cancelFirst = function
        return this
    }

    fun cancelLast(function: (NotifierSubscription) -> Unit): Notifier {
        _cancelLast = function
        return this
    }

    /**
     * 更新処理を行う際のロック
     */
    private val lock = ReentrantLock()

    /**
     * サブスクリプションをリストに加える
     * @param subscription サブスクリプション
     * @param executor subscribe処理するExecutor
     */
    fun subscribe(subscription: NotifierSubscription, executor: Executor? = null) = lock.withLock {
        val runnable = Runnable {
            _subscribeFirst(subscription)
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
                _subscribeLast(subscription)
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
                _postFirst(s)
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
                _postLast(s)
            }
        }
        executor?.execute(runnable) ?: runnable.run()
    }

    private fun unsubscribePredicate(subscription: NotifierSubscription, throwable: Optional<Throwable>): Boolean {
        _unsubscribeFirst(subscription)
        if (throwable.isPresent) {
            subscription.subscriber.onError(throwable.get())
        } else {
            subscription.subscriber.onComplete()
        }
        val result = cancel(subscription = subscription)
        _unsubscribeLast(subscription)
        return result
    }

    private fun cancelPredicate(subscriptions: List<NotifierSubscription>): Int {
        var result = 0
        for (s in subscriptions) {
            _cancelFirst(s)
            if (_subscriptions.removeIf { it == s }) {
                result += 1
            }
            _cancelLast(s)
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