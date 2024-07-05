package com.github.funczz.kotlin.notifier.property

import com.github.funczz.kotlin.notifier.Notifier
import java.util.concurrent.Executor

/**
 * 代入された値をNotifierでpostするが、自身の値は参照できないプロパティのインターフェイス
 * @author funczz
 */
interface WriteOnlyNotifierProperty<V : Any> {

    val notifier: Notifier

    /**
     * 更新した値をpostする
     * @param value 値
     * @param id サブスクリプションidの正規表現
     * @param executor postを実行するExecutor
     * @return 値が更新されたなら真、それ以外は偽
     */
    fun setValue(value: V, id: Regex = ".*".toRegex(), executor: Executor? = null): Boolean

    /**
     * 値をpostする
     * @param id サブスクリプションidの正規表現
     * @param executor postを実行するExecutor
     */
    fun postValue(id: Regex = ".*".toRegex(), executor: Executor? = null)

}