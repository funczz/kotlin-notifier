package com.github.funczz.kotlin.notifier

import com.github.funczz.kotlin.getJULLogger
import com.github.funczz.kotlin.notifier.ex.ExSubscriber
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.logging.Level

@Suppress("NonAsciiCharacters")
class NotifierAsyncTest {

    @Test
    fun `subscribe - サブスクリプションをsubscribeする`() {
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                id = "async1-1",
                executor = Optional.of(executor),
            ),
            async = executor
        )
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                id = "async1-2",
                executor = Optional.of(executor)
            ),
            async = executor
        )
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                id = "async1-3",
                executor = Optional.of(executor)
            ),
            async = executor
        )
        sleepMilliseconds()
        assertEquals(3, notifier.subscriptions.size)
    }

    @Test
    fun `subscribe - サブスクライバをsubscribeする`() {
        notifier.subscribe(subscriber = ExSubscriber(), id = "async2-1", async = executor)
        notifier.subscribe(subscriber = ExSubscriber(), id = "async2-2", async = executor)
        notifier.subscribe(subscriber = ExSubscriber(), id = "async2-3", async = executor)
        sleepMilliseconds()
        assertEquals(3, notifier.subscriptions.size)
    }

    @Test
    fun `post - 同期`() {
        val expected = "hello world."

        notifier.subscribe(
            subscriber = subscriberSync,
            id = "async3",
            executor = Optional.empty(),
            async = executor
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected, async = executor)
        sleepMilliseconds()
        assertEquals(expected, subscriberSync.actual)
    }

    @Test
    fun `post - 非同期`() {
        val expected = "hello world."

        notifier.subscribe(
            subscriber = subscriberAsync,
            id = "async4",
            executor = Optional.of(executor),
            async = executor
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected, async = executor)
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - サブスクライバのonNextメソッド処理からサブスクリプションのキャンセルを呼び出す`() {
        val expected = ""

        notifier.subscribe(
            subscriber = subscriberAsync,
            id = "async5",
            executor = Optional.of(executor),
            async = executor
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = "CANCEL", async = executor)
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - サブスクライバのonNextメソッドで例外エラーが発生する`() {
        val expected = Exception("ERROR.")

        notifier.subscribe(
            subscriber = subscriberAsync,
            id = "async6",
            executor = Optional.of(executor),
            async = executor
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected, async = executor)
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.error.get())
        assertEquals(0, notifier.subscriptions.size)
    }

    @BeforeEach
    fun beforeEach() {
        subscriberAsync = ExSubscriber()
        subscriptionAsync = DefaultNotifierSubscription(subscriber = subscriberAsync, executor = Optional.of(executor))
        subscriberSync = ExSubscriber()
        subscriptionSync = DefaultNotifierSubscription(subscriber = subscriberSync)
        notifier = Notifier()
            .subscribeFirst {
                logger.log(
                    Level.INFO,
                    "subscribe first: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .subscribeLast {
                logger.log(
                    Level.INFO,
                    "subscribe last: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .unsubscribeFirst {
                logger.log(
                    Level.INFO,
                    "unsubscribe first: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .unsubscribeLast {
                logger.log(
                    Level.INFO,
                    "unsubscribe last: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .cancelFirst {
                logger.log(
                    Level.INFO,
                    "cancel first: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .cancelLast {
                logger.log(
                    Level.INFO,
                    "cancel last: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .postFirst {
                logger.log(
                    Level.INFO,
                    "post first: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .postLast {
                logger.log(
                    Level.INFO,
                    "post last: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
    }

    @AfterEach
    fun afterEach() {
        notifier.unsubscribeAll()
    }

    private fun sleepMilliseconds(milliseconds: Long = 100L) {
        TimeUnit.MILLISECONDS.sleep(milliseconds)
    }

    private lateinit var subscriberAsync: ExSubscriber

    private lateinit var subscriptionAsync: NotifierSubscription

    private lateinit var subscriberSync: ExSubscriber

    private lateinit var subscriptionSync: NotifierSubscription

    private lateinit var notifier: Notifier

    companion object {

        private val logger = NotifierSubscriptionTest::class.java.getJULLogger()

        private lateinit var executor: ThreadPoolExecutor

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            executor = Executors.newFixedThreadPool(4) as ThreadPoolExecutor
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            executor.shutdownNow()
        }
    }
}