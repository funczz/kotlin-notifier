package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.getJULLogger
import com.github.funczz.kotlin.notifier.DefaultNotifierSubscription
import com.github.funczz.kotlin.notifier.Notifier
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Flow
import java.util.concurrent.Flow.Subscriber
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.logging.Level

@Suppress("NonAsciiCharacters")
class UdNotifierPropertyTest {


    @Test
    fun `getValue 値を取得する`() {
        val expected = "hello world."

        val actual: String
        val prop = UdNotifierProperty(
            initialValue = expected,
            notifier = notifier,
        )
        actual = prop.getValue()
        sleepMilliseconds()
        assertEquals(expected, actual)
    }

    @Test
    fun `setValue 保持する値と異なる値を代入するとpostされる`() {
        val expected = "hello world."

        var actual = ""
        val prop = UdNotifierProperty(
            initialValue = "Hello World!",
            notifier = notifier,
        )
        prop.notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = UdSubscriber<String> { actual = it },
                id = "prop",
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        prop.setValue(value = expected)
        sleepMilliseconds()
        assertEquals(expected, actual)
    }

    @Test
    fun `setValue 保持する値と同一の値を代入するとpostされない`() {
        val expected = "hello world."

        var actual = ""
        val prop = UdNotifierProperty(
            initialValue = expected,
            notifier = notifier,
        )
        prop.notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = UdSubscriber<String> { actual = it },
                id = "prop",
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        prop.setValue(value = expected)
        sleepMilliseconds()
        assertEquals("", actual)
    }


    @Test
    fun `postValue 値がpostされる`() {
        val expected = "hello world."

        var actual = ""
        val prop = UdNotifierProperty(
            initialValue = expected,
            notifier = notifier,
        )
        prop.notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = UdSubscriber<String> { actual = it },
                id = "prop",
                executor = Optional.of(executor)
            )
        )
        sleepMilliseconds()
        prop.postValue()
        sleepMilliseconds()
        assertEquals(expected, actual)
    }

    @BeforeEach
    fun beforeEach() {
        notifier = Notifier()
            .subscribeBefore {
                logger.log(
                    Level.INFO,
                    "Subscribe Before: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .subscribeAfter {
                logger.log(
                    Level.INFO,
                    "Subscribe After: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .unsubscribeBefore {
                logger.log(
                    Level.INFO,
                    "Unsubscribe Before: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .unsubscribeAfter {
                logger.log(
                    Level.INFO,
                    "Unsubscribe After: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .cancelBefore {
                logger.log(
                    Level.INFO,
                    "Cancel Before: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .cancelAfter {
                logger.log(
                    Level.INFO,
                    "Cancel After: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .postBefore {
                logger.log(
                    Level.INFO,
                    "Post Before: thread=${Thread.currentThread().name}, id=${it.id}"
                )
            }
            .postAfter {
                logger.log(
                    Level.INFO,
                    "Post After: thread=${Thread.currentThread().name}, id=${it.id}"
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

    private lateinit var notifier: Notifier

    companion object {

        private val logger = UdNotifierPropertyTest::class.java.getJULLogger()

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

    private class UdSubscriber<T>(

        private var _onNext: (T) -> Unit,

        ) : Subscriber<Any> {

        override fun onSubscribe(subscription: Flow.Subscription) {
        }

        @Suppress("UNCHECKED_CAST")
        override fun onNext(item: Any) {
            _onNext(item as T)
        }

        override fun onError(throwable: Throwable) {
        }

        override fun onComplete() {
        }
    }
}