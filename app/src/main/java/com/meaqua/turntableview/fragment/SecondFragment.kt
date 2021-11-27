package com.meaqua.turntableview.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.meaqua.turntableview.*
import com.meaqua.turntableview.databinding.FragmentSecondBinding
import com.weijun.turntableview.TurnTableView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

/**
 * @ClassName: FirstFragment
 * @Author: weijun
 * @Date: 2021/11/24
 * @Description:
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    private lateinit var ttv: TurnTableView<TestModel>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ttv = view.findViewById<TurnTableView<TestModel>>(R.id.ttv).apply {
            setCallBack(
                normalViewCallBack = { holder,item,position ->
                    holder.apply {
                        item.apply {
                            if (turntable_detail_id != "-999"){
                                getView<ConstraintLayout>(R.id.clroot).background = context.getResDrawable(
                                    if (press) R.drawable.icon_turntable_normal_bg_select else R.drawable.icon_turntable_normal_bg
                                )
                                if (turntable_detail_extra_icon.isBlank()){
                                    getView<ImageView>(R.id.fivGift).load(R.drawable.aaaaa) {}
                                }else{
                                    getView<ImageView>(R.id.fivGift).load(turntable_detail_extra_icon) {}
                                }
                                setText(R.id.tvGiftName, turntable_detail_extra_name)
                            }
                        }
                    }
                },
                progressViewCallBack = { holder,item,position ->
                    holder.apply {
                        item.apply {
                            getView<RelativeLayout>(R.id.clroot).background = context.getResDrawable(R.drawable.icon_turntable_progress_bg)
                            setText(R.id.tvProgress, "${mCurrentProgress}抽/${mMaxProgress}抽")
                            getView<ImageView>(R.id.ivProgress).loadGif(R.drawable.icon_gif_turntable_progress)
                            getView<ImageView>(R.id.ivRight).apply {
                                layoutParams = (layoutParams as RelativeLayout.LayoutParams).apply rlBg@{
                                    this@rlBg.width = (resources.dp2px(72.0f) * (1 - mCurrentProgress.toFloat() / mMaxProgress.toFloat())).toInt()
                                    this@rlBg.addRule(RelativeLayout.CENTER_VERTICAL)
                                    this@rlBg.leftMargin = resources.dp2px(1.0f).toInt()
                                    this@rlBg.rightMargin = resources.dp2px(1.0f).toInt()
                                    this@rlBg.topMargin = resources.dp2px(1.0f).toInt()
                                    this@rlBg.bottomMargin = resources.dp2px(1.0f).toInt()
                                }
                            }
                        }
                    }
                }
            )
            onTurnTableFinish = { pos,model ->
                activity?.toast { "抽到了 -> position:$pos,name:${model.turntable_detail_extra_name}" }
                Log.e("TurnTableView","抽奖结果：$pos,${model.turntable_detail_extra_name}")
            }
            runBlocking {
                requestData().collect { setData(it) }
            }
        }
        binding.btnStart.onClick {
            takeIf { ttv.isDrawing } ?.run { return@onClick }
            ttv.startTurntable(10,getRandomInt(8))
        }
    }

    private fun requestData() = flow {
        emit(ArrayList<TestModel>().apply {
            for (i in 0 until 8){
                add(TestModel(TurnTableView.TURNTABLE_NORMAL,"${i+10}","","$i$i$i","$i"))
            }
            add(4,TestModel(TurnTableView.TURNTABLE_PROGRESS,"","","","$"))
        })
    }

    private fun getRandomInt(total:Int):Int{
        var nextInt = Random.nextInt(total)
        if (nextInt == 5){
            nextInt = getRandomInt(8)
        }
        return nextInt
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}