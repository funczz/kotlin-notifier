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
     * @param executor unsubscribe処理するExecutor
     */
    fun unsubscribe(subscription: NotifierSubscription, executor: Executor? = null) = lock.withLock {
        unsubscribePredicate(subscriptions = _subscriptions.filter { it == subscription }, executor = executor)
    }

    /**
     * サブスクライバをリストから削除する
     * @param subscriber サブスクライバ
     * @param executor unsubscribe処理するExecutor
     */
    fun unsubscribe(subscriber: Flow.Subscriber<in Any>, executor: Executor? = null) = lock.withLock {
        unsubscribePredicate(subscriptions = _subscriptions.filter { it.subscriber == subscriber }, executor = executor)
    }

    /**
     * サブスクリプションをリストから削除する
     * @param name サブスクリプションnameの正規表現
     * @param executor unsubscribe処理するExecutor
     */
    fun unsubscribe(name: Regex, executor: Executor? = null) = lock.withLock {
        unsubscribePredicate(subscriptions = _subscriptions.filter { it.name.matches(name) }, executor = executor)
    }

    /**
     * 全てのサブスクリプションをリストから削除する
     * @param executor unsubscribe処理するExecutor
     */
    fun unsubscribeAll(executor: Executor? = null) = lock.withLock {
        unsubscribePredicate(subscriptions = _subscriptions.toList(), executor = executor)
    }

    /**
     * サブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     * サブスクリプションから呼び出される
     * @param subscription サブスクリプション
     */
    fun onCancel(subscription: NotifierSubscription) {
        cancel(subscription = subscription, executor = null)
    }

    /**
     * サブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     * @param subscription サブスクリプション
     * @param executor cancel処理するExecutor
     */
    fun cancel(subscription: NotifierSubscription, executor: Executor? = null) = lock.withLock {
        cancelPredicate(subscriptions = _subscriptions.filter { it == subscription }, executor = executor)
    }

    /**
     * サブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     * @param subscriber サブスクライバ
     * @param executor cancel処理するExecutor
     */
    fun cancel(subscriber: Flow.Subscriber<in Any>, executor: Executor? = null) = lock.withLock {
        cancelPredicate(subscriptions = _subscriptions.filter { it.subscriber == subscriber }, executor = executor)
    }

    /**
     * サブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     * @param name サブスクリプションnameの正規表現
     * @param executor cancel処理するExecutor
     */
    fun cancel(name: Regex, executor: Executor? = null) = lock.withLock {
        cancelPredicate(subscriptions = _subscriptions.filter { it.name.matches(name) }, executor = executor)
    }

    /**
     * 全てのサブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     * @param executor cancel処理するExecutor
     */
    fun cancelAll(executor: Executor? = null) = lock.withLock {
        cancelPredicate(subscriptions = _subscriptions.toList(), executor = executor)
    }

    /**
     * サブスクリプションnameがマッチしたサブスクリプションのサブスクライバへアイテムを送信する
     * @param item アイテム
     * @param name サブスクリプションnameの正規表現
     * @param executor post処理するExecutor
     */
    fun post(item: Any, name: Regex = Regex(".*"), executor: Executor? = null) = lock.withLock {
        val runnable = Runnable {
            for (s in _subscriptions.filter { it.name.matches(regex = name) }) {
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

    private fun unsubscribePredicate(subscriptions: List<NotifierSubscription>, executor: Executor?) {
        val runnable = Runnable {
            for (s in subscriptions) {
                unsubscribePredicate(subscription = s, throwable = Optional.empty())
            }
        }
        executor?.execute(runnable) ?: runnable.run()
    }

    private fun unsubscribePredicate(subscription: NotifierSubscription, throwable: Optional<Throwable>) {
        _unsubscribeBefore(subscription)
        if (throwable.isPresent) {
            subscription.subscriber.onError(throwable.get())
        } else {
            subscription.subscriber.onComplete()
        }
        cancel(subscription = subscription)
        _unsubscribeAfter(subscription)
    }

    private fun cancelPredicate(subscriptions: List<NotifierSubscription>, executor: Executor?) {
        val runnable = Runnable {
            for (s in subscriptions) {
                _cancelBefore(s)
                _subscriptions.removeIf { it == s }
                _cancelAfter(s)
            }
        }
        executor?.execute(runnable) ?: runnable.run()
    }

    companion object {

        @JvmStatic
        val DO_NOT_POST_PATTERN = "^(?!.).".toRegex() //マッチする文字列が存在しないパターンを指定している "\$^" "^(?!.)."

        @JvmStatic
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
