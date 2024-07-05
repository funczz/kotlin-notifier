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
class WONotifierPropertyTest {

    @Test
    fun `setValue 保持する値と異なる値を代入するとpostされる`() {
        val expected = "hello world."

        var actual = ""
        val prop: WriteOnlyNotifierProperty<String> = WONotifierProperty(
            initialValue = "Hello World!",
            notifier = notifier,
        )
        prop.notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = WOSubscriber<String> { actual = it },
                name = "prop",
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
        val prop: WriteOnlyNotifierProperty<String> = WONotifierProperty(
            initialValue = expected,
            notifier = notifier,
        )
        prop.notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = WOSubscriber<String> { actual = it },
                name = "prop",
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
        val prop: WriteOnlyNotifierProperty<String> = WONotifierProperty(
            initialValue = expected,
            notifier = notifier,
        )
        prop.notifier.subscribe(
            subscription = DefaultNotifierSubscription(
                subscriber = WOSubscriber<String> { actual = it },
                name = "prop",
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

    private lateinit var notifier: Notifier

    companion object {

        private val logger = WONotifierPropertyTest::class.java.getJULLogger()

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

    private class WOSubscriber<T>(

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