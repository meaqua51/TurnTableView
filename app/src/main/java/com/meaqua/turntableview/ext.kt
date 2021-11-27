package com.meaqua.turntableview

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.Checkable
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder

/**
 * Author：weijun
 * Date：2021/11/27
 * Description：
 */
fun Context.getResDrawable(@DrawableRes resId:Int) : Drawable? = ContextCompat.getDrawable(this,resId)
fun Context.getResColor(@ColorRes resId:Int) : Int = ContextCompat.getColor(this,resId)

fun ImageView.load(url: String?, block: RequestBuilder<Drawable>.() -> Unit) =
    Glide.with(context).load(url).apply(block).into(this)

fun ImageView.load(resId: Int, block: RequestBuilder<Drawable>.() -> Unit) =
    Glide.with(context).load(resId).apply(block).into(this)

fun ImageView.loadGif(res: Int) =
    Glide.with(context).asGif().load(res).into(this)

fun Context.dp2px(dp: Float): Float = dp * resources.displayMetrics.density + 0.5f

fun Resources.dp2px(dp: Float): Float = dp * displayMetrics.density + 0.5f

inline fun Context.toast(block: () -> String){
    Toast.makeText(this,block(), Toast.LENGTH_SHORT).show()
}

var <T : View> T.lastClickTime: Long
    set(value) = setTag(1766613352, value)
    get() = getTag(1766613352) as? Long ?: 0

inline fun <T : View> T.onClick(time: Long = 1000, crossinline block: (T) -> Unit) {
    setOnClickListener {
        val currentTimeMillis = System.currentTimeMillis()
        if (currentTimeMillis - lastClickTime > time || this is Checkable) {
            lastClickTime = currentTimeMillis
            block(this)
        }
    }
}