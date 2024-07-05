package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.getJULLogger
import com.github.funczz.kotlin.notifier.Notifier
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.logging.Level

@Suppress("NonAsciiCharacters")
class RONotifierPropertyTest {

    @Test
    fun `getValue 値を取得する`() {
        val expected = "hello world."

        val actual: String
        val prop: ReadOnlyNotifierProperty<String> = RONotifierProperty(
            initialValue = expected,
            notifier = notifier,
            name = "prop",
            executor = Optional.of(executor),
        )
        actual = prop.getValue()
        sleepMilliseconds()
        assertEquals(expected, actual)
    }

    @Test
    fun `Notifier から値を受け取ると更新される`() {
        val expected = "hello world."

        val prop: ReadOnlyNotifierProperty<String> = RONotifierProperty(
            initialValue = "",
            notifier = notifier,
            name = "prop",
            executor = Optional.of(executor),
        )
        (prop as RONotifierProperty).subscriber
            .onNext { println("prop#onNext: $it") }
        sleepMilliseconds()
        notifier.post(item = expected)
        sleepMilliseconds()
        val actual = prop.getValue()
        assertEquals(expected, actual)
    }

    @Test
    fun `Notifier から保持する値と同一の値を受け取ると更新されない`() {
        val expected = "hello world."

        val prop: ReadOnlyNotifierProperty<String> = RONotifierProperty(
            initialValue = expected,
            notifier = notifier,
            name = "prop",
            executor = Optional.of(executor),
        )
        (prop as RONotifierProperty).subscriber
            .onNext { println("prop#onNext: $it") }
        sleepMilliseconds()
        notifier.post(item = expected)
        sleepMilliseconds()
        val actual = prop.getValue()
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

        private val logger = RONotifierPropertyTest::class.java.getJULLogger()

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