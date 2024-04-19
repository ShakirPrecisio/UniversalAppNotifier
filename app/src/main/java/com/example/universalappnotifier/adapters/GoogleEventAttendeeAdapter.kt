package com.example.universalappnotifier.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.universalappnotifier.R
import com.google.api.services.calendar.model.EventAttendee

class GoogleEventAttendeeAdapter(private val dataList: ArrayList<EventAttendee>, private val emailRemovedListener: EmailRemovedListener) : RecyclerView.Adapter<GoogleEventAttendeeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.email_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataList[position]
        holder.bind(position, data, emailRemovedListener)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun clear() {
        dataList.clear()
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val viewSideBar: android.view.View = itemView.findViewById(R.id.view_side_bar)
        private val tvEmailId: TextView = itemView.findViewById(R.id.tv_email_id)
        private val imgEmailType: ImageView = itemView.findViewById(R.id.img_email_type)
        private val imgRemoveEmail: ImageView = itemView.findViewById(R.id.img_remove_email)

        fun bind(
            position: Int,
            data: EventAttendee,
            emailRemovedListener: EmailRemovedListener
        ) {

            tvEmailId.text = data.email

            imgRemoveEmail.setOnClickListener {
                emailRemovedListener.onEmailIdRemoved(position, data)
            }

        }

    }

    interface EmailRemovedListener {
        fun onEmailIdRemoved(position: Int, itemData: EventAttendee)
    }

}