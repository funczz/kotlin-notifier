package com.github.funczz.kotlin.notifier

import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Flow

/**
 * Notifierイベントバスのサブスクリプション
 */
interface NotifierSubscription : Flow.Subscription {

    /**
     * アイテムを処理するサブスクライバ
     */
    val subscriber: Flow.Subscriber<Any>

    /**
     * サブスクリプションの識別子
     * 任意の文字列
     */
    val id: String

    /**
     * サブスクライバのonNextメソッドを投入するスレッドプール
     */
    val executor: Optional<Executor>

    /**
     * イベントバスから呼び出される
     */
    fun onCall(notifier: Notifier)


}