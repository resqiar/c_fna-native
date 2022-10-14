package com.example.findnearbyapp

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.example.findnearbyapp.databinding.MainCardBinding
import com.example.findnearbyapp.others.Event
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat

class MainCardAdapter(private val context: Context, private val dto: ArrayList<Event>):
    RecyclerView.Adapter<MainCardAdapter.MainCardViewHolder>() {

    inner class MainCardViewHolder (val binding: MainCardBinding) : RecyclerView.ViewHolder(binding.root)

    var onItemClick : ((Event) -> Unit)? = null

    // called when the adapter need to create the view,
    // for example when item already recycled from th view port but user want to go back -
    // again, this method will be called again to recycle the recycled view (?).
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainCardViewHolder {
        // This code below is mainly just bind and inflate the layout into view holder
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = MainCardBinding.inflate(layoutInflater, parent, false)
        return MainCardViewHolder(binding)
    }

    // THIS METHOD IS THE MAIN GUY
    // In this method is exactly where the dynamic things of the item happened
    override fun onBindViewHolder(holder: MainCardViewHolder, position: Int) {
        // Get Author data from Firestore
        var username: String? = null

        FirebaseFirestore.getInstance().collection("users").document(dto[position].authorId).get()
            .addOnSuccessListener {
                username = it.get("email").toString()

                // Split the username and remove after '@gmail.com'
                username = username?.split('@')?.get(0)

                // note that "itemView" in this context is the single item of the list.
                // basically the same as "v" or "value" in javascript's map.
                holder.binding.apply {
                    // bind the position of the view holder to the data transfer object
                    mcTitle.text = dto[position].title
                    mcDescription.text = dto[position].description
                    mcAuthor.text = "Reported by " + username.toString()
                    mcTimestamp.text = SimpleDateFormat("dd/MM/yyyy HH:mm").format(dto[position].timestamp!!.toDate())
                }
            }

//        Glide.with(context)
//            .load(dto[position].image_url)
//            .centerCrop()
//            .placeholder(R.drawable.ic_baseline_image_24)
//            .into(holder.binding.mcImage)

        // Only apply to the last item of the list
        if(position == dto.size - 1){
            holder.binding.apply {
               mcAuthor.updatePadding(0, 0, 0, 200) // in pixels
            }
        }

       holder.binding.mainCardContainer.setOnClickListener {
           onItemClick?.invoke(dto[position])
       }
    }

    // This method is to define how many list that need to be shown by the adapter,
    // most of the time (probably) we only need to return the DTO size
    override fun getItemCount(): Int {
        return dto.size
    }
}