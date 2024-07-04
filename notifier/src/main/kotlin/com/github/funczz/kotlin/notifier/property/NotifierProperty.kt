package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.notifier.Notifier
import java.util.concurrent.Executor

/**
 * 代入された値をNotifierでpostするプロパティのインターフェイス
 * @author funczz
 */
interface NotifierProperty<V> {

    val notifier: Notifier

    /**
     * 値を返却する
     * @return V
     */
    fun getValue(): V

    /**
     * 更新した値をpostする
     * @param value 値
     * @param id サブスクリプションidの正規表現
     * @param executor postを実行するExecutor
     */
    fun setValue(value: V, id: Regex = ".*".toRegex(), executor: Executor? = null)

    /**
     * 値をpostする
     * @param id サブスクリプションidの正規表現
     * @param executor postを実行するExecutor
     */
    fun postValue(id: Regex = ".*".toRegex(), executor: Executor? = null)

}