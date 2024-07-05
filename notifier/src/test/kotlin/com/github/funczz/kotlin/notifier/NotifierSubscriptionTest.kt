package com.github.funczz.kotlin.notifier

import com.github.funczz.kotlin.notifier.ex.ExSubscriber
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("NonAsciiCharacters")
class NotifierSubscriptionTest {

    @Test
    fun `request - 正常終了`() {
        val expected = "1"

        var actual = ""
        subscription.requestCode {
            actual = it.toString()
        }
        subscription.request(expected.toLong())
        assertEquals(expected, actual)
    }

    @Test
    fun `request - エラー(subscription request is less than zero)`() {
        val expected = "Subscription request is less than or equal to zero: n=0"

        val actual = assertThrows<IllegalArgumentException> {
            subscription.request(0)
        }.message
        assertEquals(expected, actual)
    }

    @Test
    fun `cancel - 正常終了`() {
        val expected = ""
        subscription.cancel()
        assertEquals(expected, subscriber.actual)
    }

    @BeforeEach
    fun beforeEach() {
        executor = Executors.newCachedThreadPool()
        subscriber = ExSubscriber()
        subscription =
            DefaultNotifierSubscription(subscriber = subscriber, name = "/", executor = Optional.of(executor))

    }

    @AfterEach
    fun afterEach() {
        executor.shutdownNow()
    }

    private lateinit var executor: ExecutorService

    private lateinit var subscription: DefaultNotifierSubscription

    private lateinit var subscriber: ExSubscriber

}