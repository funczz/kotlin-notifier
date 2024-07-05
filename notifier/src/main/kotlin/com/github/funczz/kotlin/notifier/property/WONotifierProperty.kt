package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.notifier.Notifier

/**
 * 代入された値をNotifierでpostするが、自身の値は参照できないプロパティの実装
 * @author funczz
 */
open class WONotifierProperty<V : Any>(

    /**
     * 初期値
     */
    initialValue: V,

    /**
     * Notifier
     */
    override val notifier: Notifier,

    ) : WriteOnlyNotifierProperty<V> by RWNotifierProperty(
    initialValue = initialValue,
    notifier = notifier
)