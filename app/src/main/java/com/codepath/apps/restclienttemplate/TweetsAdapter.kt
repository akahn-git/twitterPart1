package com.codepath.apps.restclienttemplate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.codepath.apps.restclienttemplate.models.Tweet
import java.text.FieldPosition

class TweetsAdapter (val tweets: ArrayList<Tweet>) :RecyclerView.Adapter<TweetsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup,viewType: Int):TweetsAdapter.ViewHolder {
        val context = parent.context
        val inflator = LayoutInflater.from(context)

        val view = inflator.inflate(R.layout.item_tweet,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: TweetsAdapter.ViewHolder,position: Int) {
        //get data model based on position
        val tweet: Tweet =tweets.get(position)

        //set views based on model
        holder.tvName.text=tweet.user?.name
        holder.tvScreenName.text="@"+tweet.user?.screenName
        holder.tvTweetBody.text=tweet.body
        holder.tvTimeStamp.text=tweet.timeStamp

        Glide.with(holder.itemView).load(tweet.user?.publicImageUrl).into(holder.ivProfileImage)
    }

    override fun getItemCount() :Int {
        return tweets.size
    }

    fun clear(){
        tweets.clear()
        notifyDataSetChanged()
    }

    fun addAll(tweetList:List<Tweet>){
        tweets.addAll(tweetList)
        notifyDataSetChanged()
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val ivProfileImage = itemView.findViewById<ImageView>(R.id.ivProfileImage)
        val tvName = itemView.findViewById<TextView>(R.id.tvName)
        val tvTweetBody = itemView.findViewById<TextView>(R.id.tvTweetBody)
        val tvScreenName = itemView.findViewById<TextView>(R.id.tvScreenName)
        val tvTimeStamp = itemView.findViewById<TextView>(R.id.tvTimeStamp)
    }
}