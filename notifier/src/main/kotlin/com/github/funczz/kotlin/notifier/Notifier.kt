package com.github.funczz.kotlin.notifier

import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Flow
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * イベントバス
 * @author funczz
 */
object Notifier {

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
     * 更新処理を行う際のロック
     */
    private val lock = ReentrantLock()

    /**
     * サブスクライバをサブスクリプションのリストに加える
     * @param subscriber アイテムを処理するサブスクライバ
     * @param id サブスクリプションを識別する任意の文字列
     * @param executor サブスクライバのonNextメソッドを投入するスレッドプール
     * @return サブスクリプションを返却する
     */
    fun subscribe(
        subscriber: Flow.Subscriber<in Any>,
        id: String,
        executor: Optional<ThreadPoolExecutor> = Optional.empty()
    ): NotifierSubscription {
        val subscription = DefaultNotifierSubscription(
            subscriber = subscriber, id = id, executor = executor
        )
        subscribe(subscription = subscription)
        return subscription
    }

    /**
     * サブスクリプションをリストに加える
     * @param subscription サブスクリプション
     */
    fun subscribe(subscription: NotifierSubscription) = lock.withLock {
        try {
            if (_subscriptions.any { it != subscription && it.subscriber == subscription.subscriber }) {
                throw IllegalArgumentException("Duplicate subscriber.")
            }
            when (_subscriptions.addIfAbsent(subscription)) {
                true -> subscription.subscriber.onSubscribe(subscription)
                else -> {}
            }
        } catch (th: Throwable) {
            unsubscribePredicate(subscription = subscription, throwable = Optional.ofNullable(th))
        }
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
     * @param subscription サブスクリプション
     * @return リストから削除されたなら真を、それ以外は偽を返却する
     */
    fun cancel(subscription: NotifierSubscription): Boolean = lock.withLock {
        return _subscriptions.removeIf { it == subscription }
    }

    /**
     * サブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     * @param subscriber サブスクライバ
     * @return リストから削除されたなら真を、それ以外は偽を返却する
     */
    fun cancel(subscriber: Flow.Subscriber<in Any>): Boolean = lock.withLock {
        return _subscriptions.removeIf { it.subscriber == subscriber }
    }

    /**
     * サブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     * @param id idの正規表現
     * @return 削除されたサブスクリプションの個数を返却する
     */
    fun cancel(id: Regex): Int = lock.withLock {
        var result = 0
        for (s in _subscriptions.filter { it.id.matches(id) }) {
            if (_subscriptions.removeIf { it == s }) {
                result += 1
            }
        }
        return result
    }

    /**
     * 全てのサブスクリプションをリストから削除するが、サブスクライバに対しては何も操作を行わない
     */
    fun cancelAll(): Int = lock.withLock {
        var result = 0
        for (s in _subscriptions.toList()) {
            if (_subscriptions.removeIf { it == s }) {
                result += 1
            }
        }
        result
    }

    /**
     * idがマッチしたサブスクリプションのサブスクライバへアイテムを送信する
     * @param item アイテム
     * @param id idの正規表現
     */
    fun post(item: Any, id: Regex = Regex(".*")) = lock.withLock {
        for (s in _subscriptions.filter { it.id.matches(regex = id) }) {
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
        }
    }

    private fun unsubscribePredicate(subscription: NotifierSubscription, throwable: Optional<Throwable>): Boolean {
        if (throwable.isPresent) {
            subscription.subscriber.onError(throwable.get())
        } else {
            subscription.subscriber.onComplete()
        }
        return cancel(subscription = subscription)
    }

}