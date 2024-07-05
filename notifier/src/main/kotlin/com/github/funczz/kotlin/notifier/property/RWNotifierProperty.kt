package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.notifier.Notifier
import java.util.concurrent.Executor

/**
 * 代入された値をNotifierでpostするプロパティの実装
 * @author funczz
 */
open class RWNotifierProperty<V : Any>(

    /**
     * 初期値
     */
    initialValue: V,

    /**
     * Notifier
     */
    override val notifier: Notifier,

    ) : ReadWriteNotifierProperty<V> {

    private var _value: V = initialValue

    override fun getValue(): V {
        return _value
    }

    override fun setValue(value: V, name: Regex, executor: Executor?): Boolean {
        if (_value == value) return false
        _value = value
        postValue(name = name, executor = executor)
        return true
    }

    override fun postValue(name: Regex, executor: Executor?) {
        if (name == Notifier.DO_NOT_POST_PATTERN) return
        notifier.post(item = _value as Any, name = name, executor = executor)
    }
}