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
import com.example.universalappnotifier.enums.EventSource
import com.example.universalappnotifier.models.GenericEventModel
import com.example.universalappnotifier.utils.Utils
import com.example.universalappnotifier.utils.Utils.createFadedVersionOfColor


class GenericEventsAdapter(private val dataList: ArrayList<GenericEventModel>, private val context: Context) :
    RecyclerView.Adapter<GenericEventsAdapter.ViewHolder>() {

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
        private val tvCreatedBy: TextView = itemView.findViewById(R.id.tv_created_by)
        private val tvStartTime: TextView = itemView.findViewById(R.id.tv_start_time)
        private val tvEndTime: TextView = itemView.findViewById(R.id.tv_end_time)
        private val llEventSourceLayout: LinearLayout = itemView.findViewById(R.id.ll_event_source_layout)
        private val tvEventSource: TextView = itemView.findViewById(R.id.tv_event_source)
        private val imgEventSource: ImageView = itemView.findViewById(R.id.img_event_source)

        fun bind(data: GenericEventModel, context: Context) {
            viewSideBar.setBackgroundColor(data.color!!)
            llEventSourceLayout.background = createRoundedDrawable(data.color!!)
            tvTitle.text = data.title
            tvCreatedBy.text = context.getString(R.string.created_by, data.created_by)
            if (data.event_source==EventSource.GOOGLE) {
                tvStartTime.text = context.getString(R.string.start, Utils.formatTimeFromTimestamp(data.start_time.toString()))
                tvEndTime.text = context.getString(R.string.end, Utils.formatTimeFromTimestamp(data.end_time.toString()))
            } else if (data.event_source == EventSource.OUTLOOK) {
                tvStartTime.text = context.getString(R.string.start, Utils.convertUTCToIndianTime(data.start_time.toString()))
                tvEndTime.text = context.getString(R.string.end, Utils.convertUTCToIndianTime(data.end_time.toString()))
            }

            tvEventSource.text = data.event_source_email_id
            tvEventSource.text = context.getString(R.string.coming_from, data.event_source_email_id)
            if (data.event_source == EventSource.GOOGLE) {
                imgEventSource.setImageResource(R.drawable.logo_google)
            } else if (data.event_source == EventSource.OUTLOOK) {
                imgEventSource.setImageResource(R.drawable.ic_outlook_calendar_logo)
            }
        }
        private fun createRoundedDrawable(color: Int): Drawable {
            val fadedColorVersion = createFadedVersionOfColor(color, 0.2f)
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.RECTANGLE
            drawable.cornerRadius = 100.0f
            drawable.setColor(fadedColorVersion)
            return drawable
        }
    }

}