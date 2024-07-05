package com.github.funczz.kotlin.notifier.property

/**
 * 代入された値をNotifierでpostするプロパティのインターフェイス
 * @author funczz
 */
interface ReadWriteNotifierProperty<V : Any> : ReadOnlyNotifierProperty<V>, WriteOnlyNotifierProperty<V>