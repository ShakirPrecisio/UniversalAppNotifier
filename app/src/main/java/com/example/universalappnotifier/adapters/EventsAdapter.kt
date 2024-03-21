package com.example.universalappnotifier.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.universalappnotifier.R
import com.example.universalappnotifier.enums.EventSource
import com.example.universalappnotifier.models.GenericEventModel

class EventsAdapter(private val dataList: List<GenericEventModel>, private val context: Context) : RecyclerView.Adapter<EventsAdapter.ViewHolder>() {

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

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvCreatedBy: TextView = itemView.findViewById(R.id.tv_created_by)
        private val tvStartTime: TextView = itemView.findViewById(R.id.tv_start_time)
        private val tvEndTime: TextView = itemView.findViewById(R.id.tv_end_time)
        private val tvEventSource: TextView = itemView.findViewById(R.id.tv_event_source)
        private val imgEventSource: ImageView = itemView.findViewById(R.id.img_event_source)

        fun bind(data: GenericEventModel, context: Context) {
            tvTitle.text = data.title
            tvCreatedBy.text = data.created_by
            tvStartTime.text = data.start_time
            tvEndTime.text = data.end_time
            tvEventSource.text = data.event_source_email_id
            if (data.event_source == EventSource.GOOGLE) {
                imgEventSource.setImageResource(R.drawable.logo_google)
            }
        }
    }

}