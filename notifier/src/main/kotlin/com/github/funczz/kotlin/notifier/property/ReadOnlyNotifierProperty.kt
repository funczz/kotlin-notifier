package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.notifier.Notifier

/**
 * Notifierからpostされた値を受け取るプロパティのインターフェイス
 * @author funczz
 */
interface ReadOnlyNotifierProperty<V : Any> {

    val notifier: Notifier

    /**
     * 値を返却する
     * @return V
     */
    fun getValue(): V

}