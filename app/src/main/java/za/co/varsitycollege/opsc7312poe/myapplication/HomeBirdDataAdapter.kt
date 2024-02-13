package za.co.varsitycollege.opsc7312poe.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HomeBirdDataAdapter(private val birdList: ArrayList<BirdData>) : RecyclerView.Adapter<HomeBirdDataAdapter.homeBirdViewHolder>(){
    class homeBirdViewHolder(itemview: View): RecyclerView.ViewHolder(itemview){
        val imageView : ImageView = itemView.findViewById(R.id.imagePlaceholder)
        val birdNameView: TextView = itemView.findViewById(R.id.textView)
        val locationView: TextView = itemView.findViewById(R.id.textView3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeBirdDataAdapter.homeBirdViewHolder {
        val itemview =LayoutInflater.from(parent.context).inflate(R.layout.bird_card_view,parent,false)
        return HomeBirdDataAdapter.homeBirdViewHolder(itemview)
    }

    override fun getItemCount(): Int {
        return birdList.size
    }

    override fun onBindViewHolder(holder: HomeBirdDataAdapter.homeBirdViewHolder, position: Int) {
        val currentBird = birdList[position]
        holder.birdNameView.text= currentBird.bname
        holder.locationView.text= currentBird.latitude.toString()
        Glide.with(holder.imageView.context)
            .load(currentBird.birdImage) // Assuming birdImage is a URL string
            .into(holder.imageView)

    }
}