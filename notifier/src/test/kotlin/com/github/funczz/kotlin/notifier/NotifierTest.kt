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
    fun `subscribe - サブスクライバをsubscribeする`() {
        notifier.subscribe(subscriber = ExSubscriber(), id = "")
        notifier.subscribe(subscriber = ExSubscriber(), id = "")
        notifier.subscribe(subscriber = ExSubscriber(), id = "")
        sleepMilliseconds()
        assertEquals(3, notifier.subscriptions.size)
    }

    @Test
    fun `subscribe - サブスクライバが重複する場合はサブスクライバに例外エラー(Duplicate subscriber)を送信する`() {
        val expected = "Duplicate subscriber."

        notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
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
        notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.unsubscribe(subscriber = subscriberAsync)
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("complete", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribe - id がマッチする場合はサブスクリプションをunsubscribeする`() {
        notifier.subscribe(subscriber = subscriberAsync, id = "foo", executor = Optional.of(executor))
        notifier.subscribe(subscriber = subscriberSync, id = "foo", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(2, notifier.subscriptions.size)
        val result = notifier.unsubscribe(id = Regex("^foo"))
        sleepMilliseconds()
        assertEquals(2, result)
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("complete", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribe - id がマッチしない場合は何も処理しない`() {
        notifier.subscribe(subscriber = subscriberAsync, id = "foo", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        val result = notifier.unsubscribe(id = Regex("^bar"))
        sleepMilliseconds()
        assertEquals(0, result)
        assertEquals(1, notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribeAll - サブスクリプションを全てunsubscribeする`() {
        notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        notifier.subscribe(subscriber = ExSubscriber(), id = "", executor = Optional.of(executor))
        notifier.subscribe(subscriber = ExSubscriber(), id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(3, notifier.subscriptions.size)
        val result = notifier.unsubscribeAll()
        sleepMilliseconds()
        assertEquals(3, result)
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
        notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.cancel(subscriber = subscriberAsync)
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancel - id がマッチする場合はサブスクリプションをキャンセルする`() {
        notifier.subscribe(subscriber = subscriberAsync, id = "foo", executor = Optional.of(executor))
        notifier.subscribe(subscriber = subscriberSync, id = "foo", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(2, notifier.subscriptions.size)
        val result = notifier.cancel(id = Regex("^foo"))
        sleepMilliseconds()
        assertEquals(2, result)
        assertEquals(0, notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancel - id がマッチしない場合は何も処理しない`() {
        notifier.subscribe(subscriber = subscriberAsync, id = "foo", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        val result = notifier.cancel(id = Regex("^bar"))
        sleepMilliseconds()
        assertEquals(0, result)
        assertEquals(1, notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancelAll - サブスクリプションを全てキャンセルする`() {
        notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        notifier.subscribe(subscriber = ExSubscriber(), id = "", executor = Optional.of(executor))
        notifier.subscribe(subscriber = ExSubscriber(), id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(3, notifier.subscriptions.size)
        val result = notifier.cancelAll()
        sleepMilliseconds()
        assertEquals(3, result)
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

        notifier.subscribe(subscriber = subscriberSync, id = "", executor = Optional.empty())
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected)
        sleepMilliseconds()
        assertEquals(expected, subscriberSync.actual)
    }

    @Test
    fun `post - 非同期`() {
        val expected = "hello world."

        notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected)
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - サブスクライバのonNextメソッド処理からサブスクリプションのキャンセルを呼び出す`() {
        val expected = ""

        notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
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

        notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected)
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.error.get())
        assertEquals(0, notifier.subscriptions.size)
    }

    @Test
    fun `post - id がマッチする場合はサブスクライバへアイテムを送信する`() {
        val expected = "hello world."

        notifier.subscribe(subscriber = subscriberAsync, id = "/hello/world", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = expected, id = Regex("^/hello/.*"))
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - id がマッチしない場合は何も処理しない`() {
        val expected = ""

        notifier.subscribe(subscriber = subscriberAsync, id = "/hello/world", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)
        notifier.post(item = "hello world.", id = Regex("^/hello$"))
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