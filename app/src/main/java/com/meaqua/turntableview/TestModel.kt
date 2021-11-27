package com.meaqua.turntableview

import com.weijun.turntableview.TurnTableModel

/**
 * Author：weijun
 * Date：2021/11/27
 * Description：
 */
data class TestModel(
    override val itemType: Int,
    var turntable_detail_id:String,
    var turntable_detail_extra_icon:String,
    var turntable_detail_extra_name:String,
    var turntable_detail_extra_number:String,
) : TurnTableModel(itemType)