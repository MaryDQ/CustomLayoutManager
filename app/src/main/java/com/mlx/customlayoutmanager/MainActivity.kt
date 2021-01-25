package com.mlx.customlayoutmanager

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mlx.customlayoutmanager.databinding.ActivityMainBinding
import java.util.concurrent.Flow

class MainActivity : AppCompatActivity() {
    private lateinit var customLayoutManager: CustomLayoutManager
    private lateinit var flowLayoutManager: FlowLayoutManager
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        initViewListener()

        val dataList = IntArray(100)

        customLayoutManager = CustomLayoutManager(RecyclerView.VERTICAL)
        flowLayoutManager = FlowLayoutManager()


        binding.rvPic.layoutManager = flowLayoutManager
        binding.rvPic.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                val textView = TextView(parent.context)
                val params = RecyclerView.LayoutParams((100 + 300 * Math.random()).toInt(), 100)
                textView.background = GradientDrawable().apply {
                    setStroke(5, Color.BLACK)
                }
                textView.layoutParams = params
                return object : RecyclerView.ViewHolder(textView) {}
            }

            override fun getItemCount(): Int {
                return dataList.size
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val tv = holder.itemView as TextView
                tv.text = "$position"
            }
        }
    }

    private fun initViewListener() {
        binding.tvScrollToPosition.setOnClickListener {

            binding.rvPic.smoothScrollToPosition(50)
        }

        binding.rvPic.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                super.getItemOffsets(outRect, view, parent, state)
                outRect.set(5,5,5,5)
            }
        })
    }

    private fun initView() {
//        rvPic = findViewById(R.id.rvPic)
//        tvScrollToPosition = findViewById(R.id.tvScrollToPosition)
    }
}