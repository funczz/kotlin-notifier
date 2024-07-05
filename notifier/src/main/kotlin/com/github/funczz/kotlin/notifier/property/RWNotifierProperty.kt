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

    override fun setValue(value: V, id: Regex, executor: Executor?): Boolean {
        if (_value == value) return false
        _value = value
        postValue(id = id, executor = executor)
        return true
    }

    override fun postValue(id: Regex, executor: Executor?) {
        if (id == DO_NOT_POST_ID_PATTERN) return
        notifier.post(item = _value as Any, id = id, executor = executor)
    }

    companion object {
        val DO_NOT_POST_ID_PATTERN = "^(?!.).".toRegex() //マッチする文字列が存在しないパターンを指定している "\$^" "^(?!.)."
    }
}