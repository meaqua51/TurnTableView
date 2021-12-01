package com.weijun.turntableview

import android.util.SparseIntArray
import android.view.ViewGroup
import androidx.annotation.LayoutRes

/**
 * @Author: weijun
 * @Date: 2021/12/1
 * @Description:
 */
abstract class BaseMultiItemSimpleAdapter<T : MultiItemModel, VH : BaseViewHolder>(data: MutableList<T>? = null)
    :BaseSimpleAdapter<T,VH>(0,data){

    private val layouts: SparseIntArray by lazy(LazyThreadSafetyMode.NONE) { SparseIntArray() }

    override fun getDefItemViewType(position: Int): Int {
        return data[position].itemType
    }

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layoutResId = layouts.get(viewType)
        require(layoutResId != 0) { "ViewType: $viewType found layoutResId，please use addItemType() first!" }
        return createBaseViewHolder(parent, layoutResId)
    }

    protected fun addItemType(type: Int, @LayoutRes layoutResId: Int) {
        layouts.put(type, layoutResId)
    }
}