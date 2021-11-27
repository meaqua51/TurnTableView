package com.weijun.turntableview

import com.chad.library.adapter.base.entity.MultiItemEntity

/**
 * Author：weijun
 * Date：2021/11/27
 * Description：
 */
open class TurnTableModel(override val itemType: Int) : MultiItemEntity {
    var press:Boolean = false
}
