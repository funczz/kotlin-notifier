package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.notifier.Notifier
import java.util.concurrent.Executor

/**
 * 代入された値をNotifierでpostするプロパティの実装
 * @author funczz
 */
open class UdNotifierProperty<V : Any>(

    /**
     * 初期値
     */
    initialValue: V,

    /**
     * Notifier
     */
    override val notifier: Notifier,

    ) : NotifierProperty<V> {

    private var _value: V = initialValue

    override fun getValue(): V {
        return _value
    }

    override fun setValue(value: V, id: Regex, executor: Executor?) {
        if (_value == value) return
        _value = value
        postValue(id = id, executor = executor)
    }

    override fun postValue(id: Regex, executor: Executor?) {
        notifier.post(item = _value as Any, id = id, executor = executor)
    }

}