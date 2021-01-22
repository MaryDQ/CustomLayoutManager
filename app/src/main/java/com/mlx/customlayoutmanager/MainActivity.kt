package com.mlx.customlayoutmanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var rvPic: RecyclerView
    private lateinit var tvScrollToPosition: TextView
    private lateinit var customLayoutManager: CustomLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
        initViewListener()

        val dataList = IntArray(100)

        customLayoutManager = CustomLayoutManager(RecyclerView.VERTICAL)


        rvPic.layoutManager = customLayoutManager
        rvPic.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                val textView = TextView(parent.context)
                val params = RecyclerView.LayoutParams(100, 100)
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
        tvScrollToPosition.setOnClickListener {
            rvPic.smoothScrollToPosition(50)
        }
    }

    private fun initView() {
        rvPic = findViewById(R.id.rvPic)
        tvScrollToPosition = findViewById(R.id.tvScrollToPosition)
    }
}