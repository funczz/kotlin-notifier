package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.getJULLogger
import com.github.funczz.kotlin.notifier.Notifier
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class NotifierPropertyTest {

    @Test
    fun int() {
        val expected = 10

        var actual = 0
        val prop = NotifierProperty(
            initialValue = 0,
            id = "prop",
            executor = executor
        ).onNext {
            actual = it
        }
        prop.subscribe(
            notifier = notifier
        )
        sleepMilliseconds()
        notifier.post(item = expected, id = "prop".toRegex(), executor = executor)
        sleepMilliseconds()
        assertEquals(expected, actual)
    }

    @Test
    fun string() {
        val expected = "hello world."

        var actual = ""
        val prop = NotifierProperty(
            initialValue = "",
            id = "prop",
            executor = executor
        ).onNext {
            actual = it
        }
        prop.subscribe(
            notifier = notifier
        )
        sleepMilliseconds()
        notifier.post(item = expected, id = "prop".toRegex(), executor = executor)
        sleepMilliseconds()
        assertEquals(expected, actual)
    }

    @Test
    fun list() {
        val expected = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)

        val actual = mutableListOf<Int>()
        val prop = NotifierProperty(
            initialValue = listOf<Int>(),
            id = "prop",
            executor = executor
        ).onNext {
            actual.addAll(it)
        }
        prop.subscribe(
            notifier = notifier
        )
        sleepMilliseconds()
        notifier.post(item = expected, id = "prop".toRegex(), executor = executor)
        sleepMilliseconds()
        assertEquals(expected, actual)
    }

    @Test
    fun unsubscribe() {
        val prop = NotifierProperty(
            initialValue = "",
            id = "prop",
            executor = executor
        )
        prop.subscribe(
            notifier = notifier
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)

        prop.unsubscribe()
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
    }


    @Test
    fun cancel() {
        val prop = NotifierProperty(
            initialValue = "",
            id = "prop",
            executor = executor
        )
        prop.subscribe(
            notifier = notifier
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)

        prop.cancel()
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
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

        private val logger = NotifierPropertyTest::class.java.getJULLogger()

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