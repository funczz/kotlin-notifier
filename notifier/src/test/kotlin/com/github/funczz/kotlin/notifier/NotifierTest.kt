package com.github.funczz.kotlin.notifier

import com.github.funczz.kotlin.notifier.ex.ExSubscriber
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Suppress("NonAsciiCharacters")
class NotifierTest {

    @Test
    fun `subscribe - サブスクリプションをsubscribeする`() {
        Notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                executor = Optional.of(executor)
            )
        )
        Notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                executor = Optional.of(executor)
            )
        )
        Notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = ExSubscriber(),
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        assertEquals(3, Notifier.subscriptions.size)
    }

    @Test
    fun `subscribe - サブスクライバをsubscribeする`() {
        Notifier.subscribe(subscriber = ExSubscriber(), id = "")
        Notifier.subscribe(subscriber = ExSubscriber(), id = "")
        Notifier.subscribe(subscriber = ExSubscriber(), id = "")
        sleepMilliseconds()
        assertEquals(3, Notifier.subscriptions.size)
    }

    @Test
    fun `subscribe - サブスクライバが重複する場合はサブスクライバに例外エラー(Duplicate subscriber)を送信する`() {
        val expected = "Duplicate subscriber."

        Notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        Notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        assertEquals(expected, subscriberAsync.error.get().message)
    }

    @Test
    fun `subscribe - サブスクリプションが重複する場合は何も処理しない`() {
        Notifier.subscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        Notifier.subscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        assertEquals(false, subscriberAsync.error.isPresent)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribe - サブスクリプションをunsubscribeする`() {
        Notifier.subscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        Notifier.unsubscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(0, Notifier.subscriptions.size)
        assertEquals("complete", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribe - サブスクライバをunsubscribeする`() {
        Notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        Notifier.unsubscribe(subscriber = subscriberAsync)
        sleepMilliseconds()
        assertEquals(0, Notifier.subscriptions.size)
        assertEquals("complete", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribe - id がマッチする場合はサブスクリプションをunsubscribeする`() {
        Notifier.subscribe(subscriber = subscriberAsync, id = "foo", executor = Optional.of(executor))
        Notifier.subscribe(subscriber = subscriberSync, id = "foo", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(2, Notifier.subscriptions.size)
        val result = Notifier.unsubscribe(id = Regex("^foo"))
        sleepMilliseconds()
        assertEquals(2, result)
        assertEquals(0, Notifier.subscriptions.size)
        assertEquals("complete", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribe - id がマッチしない場合は何も処理しない`() {
        Notifier.subscribe(subscriber = subscriberAsync, id = "foo", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        val result = Notifier.unsubscribe(id = Regex("^bar"))
        sleepMilliseconds()
        assertEquals(0, result)
        assertEquals(1, Notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `unsubscribeAll - サブスクリプションを全てunsubscribeする`() {
        Notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        Notifier.subscribe(subscriber = ExSubscriber(), id = "", executor = Optional.of(executor))
        Notifier.subscribe(subscriber = ExSubscriber(), id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(3, Notifier.subscriptions.size)
        val result = Notifier.unsubscribeAll()
        sleepMilliseconds()
        assertEquals(3, result)
        assertEquals(0, Notifier.subscriptions.size)
        assertEquals("complete", subscriberAsync.actual)
    }

    @Test
    fun `cancel - サブスクリプションをキャンセルする`() {
        Notifier.subscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        Notifier.cancel(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(0, Notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancel - サブスクライバをキャンセルする`() {
        Notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        Notifier.cancel(subscriber = subscriberAsync)
        sleepMilliseconds()
        assertEquals(0, Notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancel - id がマッチする場合はサブスクリプションをキャンセルする`() {
        Notifier.subscribe(subscriber = subscriberAsync, id = "foo", executor = Optional.of(executor))
        Notifier.subscribe(subscriber = subscriberSync, id = "foo", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(2, Notifier.subscriptions.size)
        val result = Notifier.cancel(id = Regex("^foo"))
        sleepMilliseconds()
        assertEquals(2, result)
        assertEquals(0, Notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancel - id がマッチしない場合は何も処理しない`() {
        Notifier.subscribe(subscriber = subscriberAsync, id = "foo", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        val result = Notifier.cancel(id = Regex("^bar"))
        sleepMilliseconds()
        assertEquals(0, result)
        assertEquals(1, Notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancelAll - サブスクリプションを全てキャンセルする`() {
        Notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        Notifier.subscribe(subscriber = ExSubscriber(), id = "", executor = Optional.of(executor))
        Notifier.subscribe(subscriber = ExSubscriber(), id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(3, Notifier.subscriptions.size)
        val result = Notifier.cancelAll()
        sleepMilliseconds()
        assertEquals(3, result)
        assertEquals(0, Notifier.subscriptions.size)
        assertEquals("", subscriberAsync.actual)
    }

    @Test
    fun `cancel - サブスクリプションのcancelを呼び出す`() {
        val expected = ""

        Notifier.subscribe(subscription = subscriptionAsync)
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        subscriptionAsync.cancel()
        sleepMilliseconds()
        assertEquals(0, Notifier.subscriptions.size)
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - 同期`() {
        val expected = "hello world."

        Notifier.subscribe(subscriber = subscriberSync, id = "", executor = Optional.empty())
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        Notifier.post(item = expected)
        sleepMilliseconds()
        assertEquals(expected, subscriberSync.actual)
    }

    @Test
    fun `post - 非同期`() {
        val expected = "hello world."

        Notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        Notifier.post(item = expected)
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - サブスクライバのonNextメソッド処理からサブスクリプションのキャンセルを呼び出す`() {
        val expected = ""

        Notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        Notifier.post(item = "CANCEL")
        sleepMilliseconds()
        assertEquals(0, Notifier.subscriptions.size)
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - サブスクライバのonNextメソッドで例外エラーが発生する`() {
        val expected = Exception("ERROR.")

        Notifier.subscribe(subscriber = subscriberAsync, id = "", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        Notifier.post(item = expected)
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.error.get())
        assertEquals(0, Notifier.subscriptions.size)
    }

    @Test
    fun `post - id がマッチする場合はサブスクライバへアイテムを送信する`() {
        val expected = "hello world."

        Notifier.subscribe(subscriber = subscriberAsync, id = "/hello/world", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        Notifier.post(item = expected, id = Regex("^/hello/.*"))
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.actual)
    }

    @Test
    fun `post - id がマッチしない場合は何も処理しない`() {
        val expected = ""

        Notifier.subscribe(subscriber = subscriberAsync, id = "/hello/world", executor = Optional.of(executor))
        sleepMilliseconds()
        assertEquals(1, Notifier.subscriptions.size)
        Notifier.post(item = "hello world.", id = Regex("^/hello$"))
        sleepMilliseconds()
        assertEquals(expected, subscriberAsync.actual)
    }

    @BeforeEach
    fun beforeEach() {
        subscriberAsync = ExSubscriber()
        subscriptionAsync = DefaultNotifierSubscription(subscriber = subscriberAsync, executor = Optional.of(executor))
        subscriberSync = ExSubscriber()
        subscriptionSync = DefaultNotifierSubscription(subscriber = subscriberSync)
    }

    @AfterEach
    fun afterEach() {
        Notifier.unsubscribeAll()
    }

    private fun sleepMilliseconds(milliseconds: Long = 100L) {
        TimeUnit.MILLISECONDS.sleep(milliseconds)
    }

    private lateinit var subscriberAsync: ExSubscriber

    private lateinit var subscriptionAsync: NotifierSubscription

    private lateinit var subscriberSync: ExSubscriber

    private lateinit var subscriptionSync: NotifierSubscription

    companion object {

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