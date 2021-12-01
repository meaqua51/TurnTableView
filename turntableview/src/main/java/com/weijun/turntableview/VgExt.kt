package com.weijun.turntableview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

/**
 * @ClassName: VgExt
 * @Author: weijun
 * @Date: 2021/12/1
 * @Description:
 */
fun ViewGroup.getItemView(@LayoutRes layoutResId: Int): View {
    return LayoutInflater.from(this.context).inflate(layoutResId, this, false)
}