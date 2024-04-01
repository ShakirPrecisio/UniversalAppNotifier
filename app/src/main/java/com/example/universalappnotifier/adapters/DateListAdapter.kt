package com.example.universalappnotifier.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.universalappnotifier.R
import com.example.universalappnotifier.models.DateItemModel
import com.google.android.material.card.MaterialCardView

class DateListAdapter(private val dataList: ArrayList<DateItemModel>,
                      private val context: Context,
                      private val onDateSelectedListener: OnDateSelectedListener) :
    RecyclerView.Adapter<DateListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.date_list_item, parent, false)
        return ViewHolder(view, this, onDateSelectedListener)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataList[position]
        holder.bind(data, context, position, dataList)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun clear() {
        dataList.clear()
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View, private val adapter: DateListAdapter, private val onDateSelectedListener: OnDateSelectedListener) : RecyclerView.ViewHolder(itemView) {
        private val mcvParentLayout: MaterialCardView = itemView.findViewById(R.id.mcv_parent_layout)
        private val tvDay: TextView = itemView.findViewById(R.id.tv_day)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val viewBottomBar: View = itemView.findViewById(R.id.view_bottom_bar)

        fun bind(data: DateItemModel, context: Context, position: Int, dataList: ArrayList<DateItemModel>) {

            val normalColor = context.resources.getColor(R.color.sky_blue_1, null)
            val selectedColor = context.resources.getColor(R.color.red, null)

            if (data.is_selected) {
                mcvParentLayout.setCardBackgroundColor(Color.WHITE)
                viewBottomBar.setBackgroundColor(selectedColor)
            } else {
                mcvParentLayout.setCardBackgroundColor(normalColor)
                viewBottomBar.setBackgroundColor(normalColor)
            }
            mcvParentLayout.setOnClickListener {
                // Iterate through all items and deselect them
                dataList.forEach { it.is_selected = false }
                // Select the clicked item
                data.is_selected = true
                // Notify adapter that data has changed
                adapter.notifyDataSetChanged()
                onDateSelectedListener.onDateSelected(data)
            }
            tvDay.text = data.day
            tvDate.text = data.date
        }
    }

    interface OnDateSelectedListener {
        fun onDateSelected(data: DateItemModel)
    }

}