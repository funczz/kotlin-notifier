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
                name = "async1-1",
                executor = Optional.of(executor),
            ),
            executor = executor
        )
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                name = "async1-2",
                executor = Optional.of(executor)
            ),
            executor = executor
        )
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                name = "async1-3",
                executor = Optional.of(executor)
            ),
            executor = executor
        )
        sleepMilliseconds()
        assertEquals(3, notifier.subscriptions.size)
    }

    @Test
    fun `unsubscribe - サブスクリプションをunsubscribeする`() {
        notifier.subscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.unsubscribe(subscription = subscriptionAsync, executor = executor)
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("complete", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribe - サブスクライバをunsubscribeする`() {
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.unsubscribe(subscriber = subscriberAsync, executor = executor)
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("complete", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribe - name がマッチする場合はサブスクリプションをunsubscribeする`() {
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                name = "foo",
                executor = Optional.of(executor)
            )
        )
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberSync,
                name = "foo",
                executor = Optional.empty()
            )
        )
        sleepMilliseconds()
        assertEquals(2, notifier.subscriptions.size)
        notifier.unsubscribe(name = Regex("^foo"), executor = executor)
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("complete", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribeAll - サブスクリプションを全てunsubscribeする`() {
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                executor = Optional.of(executor)
            )
        )
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                executor = Optional.of(executor)
            )
        )
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(3, notifier.subscriptions.size)
        notifier.unsubscribeAll(executor = executor)
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("complete", subscriberAsync.actual)
    }

    @Test
    fun `cancel - サブスクリプションをキャンセルする`() {
        notifier.subscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.cancel(subscription = subscriptionAsync, executor = executor)
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancel - サブスクライバをキャンセルする`() {
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.cancel(subscriber = subscriberAsync, executor = executor)
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancelAll - サブスクリプションを全てキャンセルする`() {
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                executor = Optional.of(executor)
            )
        )
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                executor = Optional.of(executor)
            )
        )
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(3, notifier.subscriptions.size)
        notifier.cancelAll(executor = executor)
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `post - 同期`() {
        val expected = "hello world."
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberSync,
                name = "async2",
                executor = Optional.empty()
            ),
            executor = executor
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected, executor = executor)
        sleepMilliseconds()
        assertEquals(expected, subscriberSync.actual)
    }

    @Test
    fun `post - 非同期`() {
        val expected = "hello world."

        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                name = "async3",
                executor = Optional.of(executor)
            ),
            executor = executor
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected, executor = executor)
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - サブスクライバのonNextメソッド処理からサブスクリプションのキャンセルを呼び出す`() {
        val expected = ""

        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                name = "async4",
                executor = Optional.of(executor)
            ),
            executor = executor
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = "CANCEL", executor = executor)
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - サブスクライバのonNextメソッドで例外エラーが発生する`() {
        val expected = Exception("ERROR.")

        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                name = "async5",
                executor = Optional.of(executor)
            ),
            executor = executor
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected, executor = executor)
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
            .subscribeBefore {
                logger.log(
                    Level.INFO,
                    "Subscribe Before: thread=${Thread.currentThread().name}, name=${it.name}"
                )
            }
            .subscribeAfter {
                logger.log(
                    Level.INFO,
                    "Subscribe After: thread=${Thread.currentThread().name}, name=${it.name}"
                )
            }
            .unsubscribeBefore {
                logger.log(
                    Level.INFO,
                    "Unsubscribe Before: thread=${Thread.currentThread().name}, name=${it.name}"
                )
            }
            .unsubscribeAfter {
                logger.log(
                    Level.INFO,
                    "Unsubscribe After: thread=${Thread.currentThread().name}, name=${it.name}"
                )
            }
            .cancelBefore {
                logger.log(
                    Level.INFO,
                    "Cancel Before: thread=${Thread.currentThread().name}, name=${it.name}"
                )
            }
            .cancelAfter {
                logger.log(
                    Level.INFO,
                    "Cancel After: thread=${Thread.currentThread().name}, name=${it.name}"
                )
            }
            .postBefore {
                logger.log(
                    Level.INFO,
                    "Post Before: thread=${Thread.currentThread().name}, name=${it.name}"
                )
            }
            .postAfter {
                logger.log(
                    Level.INFO,
                    "Post After: thread=${Thread.currentThread().name}, name=${it.name}"
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