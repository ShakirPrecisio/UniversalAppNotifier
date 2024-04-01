package com.example.universalappnotifier.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.universalappnotifier.R
import com.example.universalappnotifier.models.CalendarEmailData

class AddedEmailIdAdapter(private val dataList: ArrayList<CalendarEmailData>, private val context: Context) : RecyclerView.Adapter<AddedEmailIdAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.event_list_item, parent, false)
        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataList[position]
        holder.bind(data, context)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun clear() {
        dataList.clear()
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewSideBar: View = itemView.findViewById(R.id.view_side_bar)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)

        fun bind(data: CalendarEmailData, context: Context) {
            viewSideBar.setBackgroundColor(data.color)
            tvTitle.text = data.email_id
        }

    }

}