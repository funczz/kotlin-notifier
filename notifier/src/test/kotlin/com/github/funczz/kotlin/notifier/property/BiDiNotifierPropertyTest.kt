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
class BiDiNotifierPropertyTest {

    @Test
    fun `getValue 値を取得する`() {
        val expected = ""
        val actual = property1.getValue()
        assertEquals(expected, actual)
    }

    @Test
    fun `setValue property1で更新した値をproperty2から取得する`() {
        val expected = "hello world."

        //property1
        property1.setValue(value = expected)
        sleepMilliseconds()
        //property2
        val actual = property2.getValue()
        assertEquals(expected, actual)
    }

    @Test
    fun `setValue property2で更新した値をproperty1から取得する`() {
        val expected = "hello world."

        //property2
        property2.setValue(value = expected)
        sleepMilliseconds()
        //property1
        val actual = property1.getValue()
        assertEquals(expected, actual)
    }

    @Test
    fun `setValue 保持する値と同一の値を代入するとpostされない`() {
        var expected = "hello world."

        var actual: String
        //property3
        val property3: ReadWriteNotifierProperty<String> = BiDiNotifierProperty(
            initialValue = expected,
            notifier = notifier,
            id = "property3",
            executor = Optional.of(executor),
        )
        (property3 as BiDiNotifierProperty).subscriber
            .onNext { println("property3#onNext: $it") }
        sleepMilliseconds()

        property1.setValue(value = "")
        sleepMilliseconds()
        //property3
        actual = property3.getValue()
        assertEquals(expected, actual)

        /**
         * property3がproperty1とバインドされている事を確認する
         */
        expected = "property3がproperty1とバインドされている事を確認するpost."
        //property1
        property1.setValue(value = expected)
        sleepMilliseconds()
        //property3
        actual = property3.getValue()
        assertEquals(expected, actual)
    }

    @Test
    fun `postValue property3の値をproperty1から取得する`() {
        var expected = "hello world."
        //property3
        val property3: ReadWriteNotifierProperty<String> = BiDiNotifierProperty(
            initialValue = expected,
            notifier = notifier,
            id = "property3",
            executor = Optional.of(executor),
        )
        (property3 as BiDiNotifierProperty).subscriber
            .onNext { println("property3#onNext: $it") }
        sleepMilliseconds()

        //property3
        property3.postValue()
        sleepMilliseconds()
        //property1
        var actual = property1.getValue()
        assertEquals(expected, actual)

        /**
         * property3がproperty1とバインドされている事を確認する
         */
        expected = "property3がproperty1とバインドされている事を確認するpost."
        //property1
        property1.setValue(value = expected)
        sleepMilliseconds()
        //property3
        actual = property3.getValue()
        assertEquals(expected, actual)
    }

    @BeforeEach
    fun beforeEach() {
        //notifier
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

        //sleep
        sleepMilliseconds()

        //property1
        property1 = BiDiNotifierProperty(
            initialValue = "",
            notifier = notifier,
            id = "property1",
            executor = Optional.of(executor),
        )
        (property1 as BiDiNotifierProperty).subscriber
            .onNext { println("property1#onNext: $it") }
            .onError { it.printStackTrace() }
            .onComplete { println("property1::onComplete") }

        //property2
        property2 = BiDiNotifierProperty(
            initialValue = "",
            notifier = notifier,
            id = "property2",
            executor = Optional.of(executor),
        )
        (property2 as BiDiNotifierProperty).subscriber
            .onNext { println("property2#onNext: $it") }
            .onError { it.printStackTrace() }
            .onComplete { println("property2::onComplete") }

        //sleep
        sleepMilliseconds()
    }

    @AfterEach
    fun afterEach() {
        notifier.unsubscribeAll()
    }

    private fun sleepMilliseconds(milliseconds: Long = 100L) {
        TimeUnit.MILLISECONDS.sleep(milliseconds)
    }

    private lateinit var property1: ReadWriteNotifierProperty<String>

    private lateinit var property2: ReadWriteNotifierProperty<String>
    private lateinit var notifier: Notifier

    companion object {

        private val logger = BiDiNotifierPropertyTest::class.java.getJULLogger()

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