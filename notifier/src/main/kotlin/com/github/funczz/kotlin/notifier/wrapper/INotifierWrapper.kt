package com.github.funczz.kotlin.notifier.wrapper

import com.github.funczz.kotlin.notifier.Notifier

/**
 * postされた値をラップしたオブジェクトに適用する
 * @author funczz
 */
interface INotifierWrapper<T, I> {

    /**
     * オブジェクトTにインプットデータIを適用する
     * @param input オブジェクトに適用するデータ
     */
    fun onUpdate(input: I)

    /**
     * イベントバスにsubscribeする
     * @param notifier イベントバス
     * @return 自身を返却する
     *
     */
    fun subscribe(notifier: Notifier): INotifierWrapper<T, I>

    /**
     * イベントバスからunsubscribeする
     * @return 自身を返却する
     */
    fun unsubscribe(): INotifierWrapper<T, I>

    /**
     * イベントバスからcancelする
     * @return 自身を返却する
     */
    fun cancel(): INotifierWrapper<T, I>

    /**
     * サブスクリプションのonError関数を代入する
     * @param function onError関数
     * @return 自身を返却する
     */
    fun onError(function: (Throwable) -> Unit): INotifierWrapper<T, I>

    /**
     * サブスクリプションのonComplete関数を代入する
     * @param function onComplete関数
     * @return 自身を返却する
     */
    fun onComplete(function: () -> Unit): INotifierWrapper<T, I>

}