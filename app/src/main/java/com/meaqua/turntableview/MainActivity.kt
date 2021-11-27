package com.meaqua.turntableview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import com.meaqua.turntableview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val navController by lazy(LazyThreadSafetyMode.NONE) { findNavController(R.id.nav_host_fragment_content_main) }

    private val mTabs by lazy(LazyThreadSafetyMode.NONE) { arrayListOf(binding.tvNav1,binding.tvNav2,binding.tvNav3,binding.tvNav4) }
    private val mNavIds = arrayListOf(R.id.firstFragment,R.id.secondFragment,R.id.thirdFragment,R.id.fourthFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initTabListener()
    }

    private fun initTabListener() {
        mTabs[0].isSelected = true
        binding.tvNav1.onClick { setCheckTab(0) }
        binding.tvNav2.onClick { setCheckTab(1) }
        binding.tvNav3.onClick { setCheckTab(2) }
        binding.tvNav4.onClick { setCheckTab(3) }
    }

    private fun setCheckTab(position:Int){
        navController.navigate(mNavIds[position])
        mTabs.forEachIndexed { index, textView ->
            textView.isSelected = index == position
        }
    }

    override fun onBackPressed() {
        if (mTabs[0].isSelected){
            super.onBackPressed()
        }else{
            setCheckTab(0)
        }
    }
}