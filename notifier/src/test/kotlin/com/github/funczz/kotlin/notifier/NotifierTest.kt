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
class NotifierTest {

    @Test
    fun `subscribe - サブスクリプションをsubscribeする`() {
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
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(3, notifier.subscriptions.size)
    }

    @Test
    fun `subscribe - サブスクライバが重複する場合はサブスクライバに例外エラー(Duplicate subscriber)を送信する`() {
        val expected = "Duplicate subscriber."

        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        assertEquals(expected, subscriberAsync.error.get().message)
    }

    @Test
    fun `subscribe - サブスクリプションが重複する場合は何も処理しない`() {
        notifier.subscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.subscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        assertEquals(false, subscriberAsync.error.isPresent)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribe - サブスクリプションをunsubscribeする`() {
        notifier.subscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.unsubscribe(subscription = subscriptionAsync)
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
        notifier.unsubscribe(subscriber = subscriberAsync)
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
        notifier.unsubscribe(name = Regex("^foo"))
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("complete", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribe - name がマッチしない場合は何も処理しない`() {
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                name = "foo",
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.unsubscribe(name = Regex("^bar"))
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
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
        notifier.unsubscribeAll()
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("complete", subscriberAsync.actual)
    }

    @Test
    fun `cancel - サブスクリプションをキャンセルする`() {
        notifier.subscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.cancel(subscription = subscriptionAsync)
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
        notifier.cancel(subscriber = subscriberAsync)
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancel - name がマッチする場合はサブスクリプションをキャンセルする`() {
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
        notifier.cancel(name = Regex("^foo"))
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancel - name がマッチしない場合は何も処理しない`() {
        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                name = "foo",
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.cancel(name = Regex("^bar"))
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
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
        notifier.cancelAll()
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancel - サブスクリプションのcancelを呼び出す`() {
        val expected = ""

        notifier.subscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        subscriptionAsync.cancel()
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - 同期`() {
        val expected = "hello world."

        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberSync,
                executor = Optional.empty()
            )
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected)
        sleepMilliseconds()
        assertEquals(expected, subscriberSync.actual)
    }

    @Test
    fun `post - 非同期`() {
        val expected = "hello world."

        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected)
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - サブスクライバのonNextメソッド処理からサブスクリプションのキャンセルを呼び出す`() {
        val expected = ""

        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = "CANCEL")
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
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected)
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.error.get())
        assertEquals(0, notifier.subscriptions.size)
    }

    @Test
    fun `post - name がマッチする場合はサブスクライバへアイテムを送信する`() {
        val expected = "hello world."

        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                name = "/hello/world",
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected, name = Regex("^/hello/.*"))
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - name がマッチしない場合は何も処理しない`() {
        val expected = ""

        notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = subscriberAsync,
                name = "/hello/world",
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = "hello world.", name = Regex("^/hello$"))
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.actual)
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