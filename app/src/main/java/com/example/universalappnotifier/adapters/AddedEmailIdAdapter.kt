package com.example.universalappnotifier.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.universalappnotifier.R
import com.example.universalappnotifier.models.CalendarEmailData

class AddedEmailIdAdapter(private val dataList: ArrayList<CalendarEmailData>, private val context: Context, private val emailRemovedListener: EmailRemovedListener) : RecyclerView.Adapter<AddedEmailIdAdapter.ViewHolder>() {

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

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewSideBar: View = itemView.findViewById(R.id.view_side_bar)
        private val tvEmailId: TextView = itemView.findViewById(R.id.tv_email_id)
        private val imgEmailType: ImageView = itemView.findViewById(R.id.img_email_type)
        private val imgRemoveEmail: ImageView = itemView.findViewById(R.id.img_remove_email)

        fun bind(
            position: Int,
            data: CalendarEmailData,
            emailRemovedListener: EmailRemovedListener
        ) {
            val hexColor = String.format("#%06X", 0xFFFFFF and data.color)
            viewSideBar.backgroundTintList = ColorStateList.valueOf(Color.parseColor(hexColor))

            tvEmailId.text = data.email_id

            if (data.email_type == "google") {
                imgEmailType.setImageResource(R.drawable.ic_google_calendar_logo)
            } else if (data.email_type == "outlook") {
                imgEmailType.setImageResource(R.drawable.ic_outlook_calendar_logo)
            }

            imgRemoveEmail.setOnClickListener {
                emailRemovedListener.onEmailIdRemoved(position, data)
            }

        }

    }

    interface EmailRemovedListener {
        fun onEmailIdRemoved(position: Int, itemData: CalendarEmailData)
    }

}