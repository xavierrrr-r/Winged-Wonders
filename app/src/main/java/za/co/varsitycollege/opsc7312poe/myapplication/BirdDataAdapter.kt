package za.co.varsitycollege.opsc7312poe.myapplication
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide

class BirdDataAdapter(private val birdList: ArrayList<BirdData>) : RecyclerView.Adapter<BirdDataAdapter.birdViewHolder>(){
    class birdViewHolder(itemview: View):RecyclerView.ViewHolder(itemview){
 val imageView : ImageView= itemView.findViewById(R.id.imageView)
 val birdNameView: TextView= itemView.findViewById(R.id.textView)
 val DateView: TextView= itemView.findViewById(R.id.textView2)
 val locationView: TextView= itemView.findViewById(R.id.textView3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): birdViewHolder {
    val itemview =LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_row,parent,false)
    return birdViewHolder(itemview)
    }

    override fun getItemCount(): Int {
return birdList.size
    }

    override fun onBindViewHolder(holder: birdViewHolder, position: Int) {
     val currentBird = birdList[position]
        holder.birdNameView.text= currentBird.bname
        holder.DateView.text= currentBird.selectDate
        holder.locationView.text= "Latitude: "+ currentBird.latitude +"Longitude: " + currentBird.longitude
        Glide.with(holder.imageView.context)
            .load(currentBird.birdImage) // Assuming birdImage is a URL string
            .into(holder.imageView)

    }

}