package com.github.funczz.kotlin.notifier.wrapper

import com.github.funczz.kotlin.getJULLogger
import com.github.funczz.kotlin.notifier.Notifier
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.logging.Level

@Suppress("NonAsciiCharacters")
class NotifierWrapperTest {

    @Test
    fun `onUpdate String値を代入する`() {
        val expected = "hello, hello world."

        val eo = ExObject(name = "ex", number = 0, text = "hello world.")
        val wrap = NotifierWrapper<ExObject, String>(
            target = eo,
            id = eo.name,
            executor = executor
        ).onUpdate { t, i ->
            t.text = i
        }
        wrap.subscribe(
            notifier = notifier
        )
        sleepMilliseconds()
        notifier.post(item = expected, id = eo.name.toRegex(), executor = executor)
        sleepMilliseconds()
        assertEquals(expected, eo.text)
    }

    @Test
    fun `onUpdate Any値を代入する`() {
        val expectedString = "hello, hello world."
        val expectedInt = 1

        val eo = ExObject(name = "ex", number = 0, text = "hello world.")
        val wrap = NotifierWrapper<ExObject, Any>(
            target = eo,
            id = eo.name,
            executor = executor
        ).onUpdate { t, i ->
            when (i) {
                is String -> t.text = i
                is Int -> t.number = i
                else -> throw IllegalArgumentException("$i")
            }
        }
        wrap.subscribe(
            notifier = notifier
        )
        sleepMilliseconds()
        notifier.post(item = expectedString, id = eo.name.toRegex(), executor = executor)
        notifier.post(item = expectedInt, id = eo.name.toRegex(), executor = executor)
        sleepMilliseconds()
        assertEquals(expectedString, eo.text)
        assertEquals(expectedInt, eo.number)
    }

    @Test
    fun unsubscribe() {
        val eo = ExObject(name = "ex", number = 0, text = "hello world.")
        val wrap = NotifierWrapper<ExObject, String>(
            target = eo,
            id = eo.name,
            executor = executor
        )
        wrap.subscribe(
            notifier = notifier
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)

        wrap.unsubscribe()
        sleepMilliseconds()
        assertEquals(0, notifier.subscriptions.size)
    }


    @Test
    fun cancel() {
        val eo = ExObject(name = "ex", number = 0, text = "hello world.")
        val wrap = NotifierWrapper<ExObject, String>(
            target = eo,
            id = eo.name,
            executor = executor
        )
        wrap.subscribe(
            notifier = notifier
        )
        sleepMilliseconds()
        assertEquals(1, notifier.subscriptions.size)

        wrap.cancel()
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

        private val logger = NotifierWrapperTest::class.java.getJULLogger()

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

    private data class ExObject(
        val name: String,
        var number: Int,
        var text: String,
    )
}