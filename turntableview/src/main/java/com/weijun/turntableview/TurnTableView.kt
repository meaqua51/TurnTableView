package com.weijun.turntableview

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import java.lang.ref.WeakReference

/**
 * Author：weijun
 * Date：2021/11/27
 * Description：
 */
class TurnTableView<T:TurnTableModel> :FrameLayout {

    constructor(context: Context):super(context){
        init(context, null)
    }
    constructor(context: Context, attrs: AttributeSet):super(context, attrs){
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int):super(context, attrs, defStyleAttr)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int):super(context, attrs, defStyleAttr, defStyleRes)

    companion object{
        //item类型
        val TURNTABLE_NORMAL = 0x0 //礼物item
        val TURNTABLE_PROGRESS = 0x1 //中间进度item 不可被选择
    }

    private val TAG = "TurnTableView"

    private val DEFAULT_SIZE = 300
    private var mWidth = 0
    private var mHeight = 0

    //init
    @IntDef(8,10)
    annotation class TurnTableMode
    private val MODE_NINE = 8 //九宫格 8个奖励
    private val MODE_TEN = 10 //十二宫格 10个奖励
    private var mMode = MODE_TEN //转盘模式，默认十二宫格模式

    var mResNormalId = 0 //周围一圈的item布局
    var mResProgressId = 0 //中间的item布局
    var mMaxProgress = 0 //最大进度
    var mCurrentProgress = 0 //当前进度
    var mTurnTimes = 3 //转动的圈数
    var mTurnDuration = 3000 //动画持续时间

    //status
    var isDrawing = false // 是否正在抽奖，只允许set
        internal set
    private val mPathArr by lazy(LazyThreadSafetyMode.NONE) {
        if (mMode == MODE_TEN){
            arrayOf(0,1,2,3,6,10,9,8,7,4) //转动的顺序 默认十二宫格
        }else{
            arrayOf(0,1,2,5,8,7,6,3) //九宫格转动的顺序
        }
    }
    private var mLastArrPosition = 0 //动画执行的时候真正操作mPathArr的下标
    private var lastStopPosition = 0 //上次停留在mPathArr的位置，也就是mPathArr的下标
    private var lastProgress = -1//上次更新进度的位置

    private val MSG_TURNTABLE_UPDATE_POS = 0x12
    private val mHandler by lazy(LazyThreadSafetyMode.NONE) { TurnTableHandler(this) }

    private val mTurntableRv by lazy(LazyThreadSafetyMode.NONE) {
        RecyclerView(context).apply {
            itemAnimator?.let { it ->
                it.changeDuration = 0
                takeIf { it is SimpleItemAnimator } ?.run { (it as SimpleItemAnimator).supportsChangeAnimations = false }
            }
            layoutManager = GridLayoutManager(context, if (mMode == MODE_TEN) 4 else 3).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
                    override fun getSpanSize(position: Int): Int =
                        if (mMode == MODE_TEN){
                            when {
                                position > mAdapter.data.size -> 1
                                TURNTABLE_PROGRESS == mAdapter.data[position].itemType -> 2
                                else -> 1
                            }
                        }else{
                            1
                        }
                }
            }
            adapter = mAdapter
        }
    }
    private val mAdapter by lazy(LazyThreadSafetyMode.NONE) { TurntableAdapter() }

    var onTurnTableFinish:((pos:Int,model:T) -> Unit) ?= null

    private fun init(context: Context, attrs: AttributeSet?) {
        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.TurnTableView).apply {
                mResNormalId = getResourceId(R.styleable.TurnTableView_turntable_normal_res_id, 0)
                mResProgressId = getResourceId(R.styleable.TurnTableView_turntable_progress_res_id, 0)
                mMaxProgress = getInt(R.styleable.TurnTableView_turntable_progress_max, 100)
                mCurrentProgress = getInt(R.styleable.TurnTableView_turntable_progress_current, 0)
                mTurnTimes = getInt(R.styleable.TurnTableView_turntable_turn_times, 3)
                mTurnDuration = getInt(R.styleable.TurnTableView_turntable_turn_duration, 3000)
                mMode = getInt(R.styleable.TurnTableView_turntable_mode, MODE_TEN)
                recycle()
            }
        }

        if (mResNormalId == 0 || mResProgressId == 0){
            throw Exception("TurnTableView need config turntable_normal_res_id and turntable_progress_res_id in xml.")
        }
        if (mMode != MODE_TEN){

        }
        addView(mTurntableRv)
    }

    fun setData(list: ArrayList<T>){
        mAdapter.setList(list)
    }

    fun setCallBack(
        normalViewCallBack:(holder: BaseViewHolder, item: T, position: Int) -> Unit,
        progressViewCallBack:(holder: BaseViewHolder, item: T, position: Int) -> Unit
    ){
        mAdapter.setCallBack {
            onNormalViewCallBackFun { holder, item, position ->
                normalViewCallBack(holder, item, position)
            }
            onProgressViewCallBackValFun { holder, item, position ->
                progressViewCallBack(holder, item, position)
            }
        }
    }

    /**
     * 开始转盘
     * newProgress:最新的进度
     * stopPos:停留在adapter中的下标
     */
    fun startTurntable(newProgress:Int,stopPos:Int){
//        "startTurntable -- stopPos in adapter:$stopPos".logE(TAG)
        isDrawing = true
        mAdapter.run {
            takeIf { data.size > 5 && data[5].itemType == TURNTABLE_PROGRESS } ?.run {
                mCurrentProgress += newProgress
                if (mCurrentProgress >= mMaxProgress){
                    mCurrentProgress -= mMaxProgress
                }
                notifyItemChanged(5)
            }
        }
        if (stopPos < 0){
            isDrawing = false
            return
        }
        val realPosition = mPathArr.indexOf(stopPos) //在路径中的真实位置
        if (realPosition < 0){
            isDrawing = false
            return
        }
        if (lastStopPosition < 0){
            lastStopPosition = 0
        }
        val result = ((mTurnTimes * mMode + realPosition) - lastStopPosition).toFloat()
//        "startTurntable -- realPosition in mPathArr:$realPosition".logE(TAG)
        ValueAnimator.ofFloat(0f, result).apply {
            repeatCount = 0
            duration = mTurnDuration.toLong()
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val value = (animation.animatedValue as Float).toInt()
                if (lastProgress == value) return@addUpdateListener
                mAdapter.run {
                    updatePressState(mPathArr[mLastArrPosition])
                    mLastArrPosition++
                    takeIf { mLastArrPosition >= mPathArr.size } ?.run { mLastArrPosition = 0 }
                }
                lastProgress = value
            }
            addAnimListener(
                onEnd = {
                    //先判断是否对准  true：没对准
                    var isCorrect = false
                    mAdapter.data.forEachIndexed rlb@{ index, t ->
                        if (t.press && stopPos != index){
                            isCorrect = true
                            mHandler.sendMessageDelayed(
                                Message.obtain().apply {
                                    arg1 = stopPos
                                    arg2 = realPosition
                                    what = MSG_TURNTABLE_UPDATE_POS
                                }, 200)
                            return@rlb
                        }
                    }
                    if (!isCorrect){
                        isDrawing = false
                        onTurnTableFinish?.invoke(stopPos,mAdapter.data[stopPos])
                    }else{
//                        "没对准，执行容错".logE(TAG)
                    }
                }
            )
            start()
        }
    }

    inner class TurntableAdapter: BaseMultiItemSimpleAdapter<T, BaseViewHolder>() {

        init {
            addItemType(TURNTABLE_NORMAL, mResNormalId)
            addItemType(TURNTABLE_PROGRESS, mResProgressId)
        }

        private lateinit var mCallBack:OnTurnTableCallBack<T>
        private var mLastPosition = -1

        override fun convert(holder: BaseViewHolder, item: T) {
            holder.apply {
                item.apply {
                    when(itemType){
                        TURNTABLE_NORMAL -> mCallBack.onNormalViewCallBack(holder,item,adapterPosition)
                        TURNTABLE_PROGRESS -> mCallBack.onProgressViewCallBack(holder,item,adapterPosition)
                    }
                }
            }
        }

        fun setCallBack(listener: Builder.() -> Unit){
            mCallBack = Builder().apply { listener() }
        }

        fun updatePressState(pressed_position: Int){
            takeUnless { pressed_position >= data.size } ?.run {
                data[pressed_position].press = true
            }
            notifyItemChanged(pressed_position)
            takeIf { mLastPosition >= 0 && mLastPosition < data.size } ?.run {
                data[mLastPosition].press = false
            }
            notifyItemChanged(mLastPosition)
            mLastPosition = pressed_position
        }

        inner class Builder:OnTurnTableCallBack<T> {
            private lateinit var onNormalViewCallBackVal:(holder: BaseViewHolder, item: T, position: Int) -> Unit
            private lateinit var onProgressViewCallBackVal:(holder: BaseViewHolder, item: T, position: Int) -> Unit

            override fun onNormalViewCallBack(holder: BaseViewHolder, item: T, position: Int) {
                onNormalViewCallBackVal.invoke(holder, item, position)
            }
            fun onNormalViewCallBackFun(listener:(holder: BaseViewHolder, item: T, position: Int) -> Unit){
                onNormalViewCallBackVal = listener
            }

            override fun onProgressViewCallBack(holder: BaseViewHolder, item: T, position: Int) {
                onProgressViewCallBackVal.invoke(holder, item, position)
            }
            fun onProgressViewCallBackValFun(listener:(holder: BaseViewHolder, item: T, position: Int) -> Unit){
                onProgressViewCallBackVal = listener
            }
        }
    }

    interface OnTurnTableCallBack<T>{
        fun onNormalViewCallBack(holder: BaseViewHolder, item: T,position:Int)
        fun onProgressViewCallBack(holder: BaseViewHolder, item: T,position:Int)
    }

    private inner class TurnTableHandler(v:TurnTableView<T>):Handler(Looper.getMainLooper()){
        private val mReference: WeakReference<TurnTableView<T>> = WeakReference(v)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            mReference.get()?.run {
                takeIf { MSG_TURNTABLE_UPDATE_POS == msg.what } ?.run {
                    mAdapter.run {
                        updatePressState(mPathArr[mLastArrPosition])
                        if (msg.arg1 != mPathArr[mLastArrPosition]) {
                            mHandler.sendMessageDelayed(
                                Message.obtain().apply {
                                    arg1 = msg.arg1
                                    arg2 = msg.arg2
                                    what = MSG_TURNTABLE_UPDATE_POS
                                },
                                200
                            )
                            mLastArrPosition++
                            if (mLastArrPosition < 0 || mLastArrPosition >= mPathArr.size){
                                mLastArrPosition = 0
                            }else{

                            }
                        }else{
                            isDrawing = false
                            onTurnTableFinish?.invoke(msg.arg1,mAdapter.data[msg.arg1])
                        }
                    }
                }
            }
        }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        super.onMeasure(widthSpec, heightSpec)
        mWidth = getMySize(widthSpec)
        mHeight = getMySize(heightSpec)
        //保证控件为方形
//        val min = width.coerceAtMost(height)
//        setMeasuredDimension(min, min)
        setMeasuredDimension(mWidth, mHeight)
    }

    private fun getMySize(measureSpec: Int):Int{
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (specMode) {
            MeasureSpec.EXACTLY -> specSize //确切大小,所以将得到的尺寸给view
            MeasureSpec.AT_MOST -> DEFAULT_SIZE.coerceAtMost(specSize) //默认值为300px,此处要结合父控件给子控件的最多大小(要不然会填充父控件),所以采用最小值
            else -> DEFAULT_SIZE
        }
    }
}