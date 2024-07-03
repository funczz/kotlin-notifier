package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.notifier.Notifier

interface INotifierProperty<V> {

    /**
     * 値を返却する
     * @return V
     */
    fun getValue(): V

    /**
     * 値を更新する
     * @param value subscribeされている場合はこの値がpostされる
     */
    fun setValue(value: V)

    /**
     * イベントバスにsubscribeする
     * @param notifier イベントバス
     * @return 自身を返却する
     *
     */
    fun subscribe(notifier: Notifier): INotifierProperty<V>

    /**
     * イベントバスからunsubscribeする
     * @return 自身を返却する
     */
    fun unsubscribe(): INotifierProperty<V>

    /**
     * イベントバスからcancelする
     * @return 自身を返却する
     */
    fun cancel(): INotifierProperty<V>

    /**
     * サブスクリプションのonNext関数を代入する
     * @param function onNext関数
     * @return 自身を返却する
     */
    fun onNext(function: (V) -> Unit): INotifierProperty<V>

    /**
     * サブスクリプションのonError関数を代入する
     * @param function onError関数
     * @return 自身を返却する
     */
    fun onError(function: (Throwable) -> Unit): INotifierProperty<V>

    /**
     * サブスクリプションのonComplete関数を代入する
     * @param function onComplete関数
     * @return 自身を返却する
     */
    fun onComplete(function: () -> Unit): INotifierProperty<V>

}