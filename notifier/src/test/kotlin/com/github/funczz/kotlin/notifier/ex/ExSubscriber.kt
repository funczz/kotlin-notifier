package com.github.funczz.kotlin.notifier.ex

import com.github.funczz.kotlin.notifier.DefaultNotifierSubscription
import com.github.funczz.kotlin.notifier.NotifierSubscription
import java.util.*
import java.util.concurrent.Flow
import java.util.concurrent.Flow.Subscriber

class ExSubscriber : Subscriber<Any> {

    @Suppress("MemberVisibilityCanBePrivate")
    var actual = ""
        private set

    @Suppress("MemberVisibilityCanBePrivate")
    var error: Optional<Throwable> = Optional.empty()

    private lateinit var subscription: NotifierSubscription

    override fun onSubscribe(subscription: Flow.Subscription) {
        this.subscription = subscription as DefaultNotifierSubscription
        subscription.request(Long.MAX_VALUE)
    }

    override fun onError(throwable: Throwable) {
        error = Optional.ofNullable(throwable)
    }

    override fun onComplete() {
        actual = "complete"
    }

    override fun onNext(item: Any) {
        println("Thread: %s".format(Thread.currentThread().name))
        when (item) {
            "CANCEL" -> subscription.cancel()
            is String -> actual = item
            is Throwable -> throw item
        }
        subscription.request(1L)
    }

}