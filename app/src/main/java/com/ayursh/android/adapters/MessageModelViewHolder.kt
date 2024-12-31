package com.ayursh.android.adapters

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.ayursh.android.R
import com.ayursh.android.activities.FullImageActivity
import com.ayursh.android.activities.agora.ChatActivity
import com.ayursh.android.models.MessageModel
import com.ayursh.android.utils.toDate
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


class MessageModelViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    var imageLeft: ImageView
    var imageRight: ImageView
    var failAlert: ImageView
    var leftFailAlert: ImageView
    var msgTextLeft: TextView
    var msgTextRight: TextView
    var timeTextRight: TextView
    var timeTextLeft: TextView
    var rightLay: LinearLayout
    var rootLay: LinearLayout
    var leftLay: LinearLayout
    var dateLay: RelativeLayout
    var date: TextView
    var progressBar: ProgressBar
    var leftProgressbar: ProgressBar
    var TAG="MessageModel"
    var handler: Handler = Handler()
    var messageModel: MessageModel?=null
    var fromUser: String?=null


    private val format: String = "dd,MMM hh:mm a"
    fun bindMessage(messageModel: MessageModel, context: Context?, fromUser: String) {
        this.messageModel=messageModel
        this.fromUser=fromUser

        rootLay.setOnClickListener {
            if (messageModel.type == "media-image") {
                context?.startActivity(Intent(context, FullImageActivity::class.java).putExtra("image_url", messageModel.message))
            }
        }
        if (messageModel.type == "text") {

            imageLeft.visibility = View.GONE
            imageRight.visibility = View.GONE
            msgTextRight.visibility = View.GONE
            msgTextLeft.visibility = View.GONE
            timeTextRight.visibility = View.GONE
            timeTextLeft.visibility = View.GONE
            if (messageModel.from_uid == fromUser) {
                msgTextRight.visibility = View.VISIBLE
                timeTextRight.visibility = View.VISIBLE
                msgTextRight.text = messageModel.message
                timeTextRight.text = messageModel.sendAt.toDate(format)
            } else {
                msgTextLeft.visibility = View.VISIBLE
                timeTextLeft.visibility = View.VISIBLE
                msgTextLeft.text = messageModel.message
                timeTextLeft.text = messageModel.sendAt.toDate(format)
            }
        } else if(messageModel.message!=null && messageModel.message.length>0) {
            if (messageModel.type == "media-image") {

                val imageUrl: String = messageModel.message
                if (imageUrl.startsWith("gs://")) {
                    val storageReference: StorageReference = FirebaseStorage.getInstance()
                        .getReferenceFromUrl(imageUrl)
                    storageReference.downloadUrl
                        .addOnSuccessListener(OnSuccessListener<Uri> { uri ->
                            val downloadUrl = uri.toString()
                            if (messageModel.from_uid == fromUser) {
                                rightLay.visibility = ImageView.VISIBLE
                                leftLay.visibility = ImageView.GONE
                                Glide.with(imageRight.context)
                                    .load(downloadUrl)
                                    .listener(object : RequestListener<Drawable> {
                                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                            if (!isFirstResource) {
                                                failAlert.visibility = View.VISIBLE
                                                progressBar.visibility = View.GONE
                                            }
                                            return false
                                        }

                                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                            progressBar.visibility = View.GONE
                                            return false
                                        }

                                    })
                                    .into(imageRight)
                                timeTextRight.visibility = View.VISIBLE
                                timeTextRight.text = messageModel.sendAt.toDate(format)
                            } else {
                                leftLay.visibility = ImageView.VISIBLE
                                rightLay.visibility = ImageView.GONE
                                Glide.with(imageLeft.context)
                                    .load(downloadUrl)
                                    .listener(object : RequestListener<Drawable> {
                                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                            leftFailAlert.visibility = View.VISIBLE
                                            leftProgressbar.visibility = View.GONE
                                            return false
                                        }

                                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                            leftProgressbar.visibility = View.GONE
                                            return false
                                        }

                                    })
                                    .into(imageLeft)

                                timeTextLeft.visibility = View.VISIBLE
                                timeTextLeft.text = messageModel.sendAt.toDate(format)
                            }
                        })
                        .addOnFailureListener { e -> Log.w(TAG, "Getting download url was not successful.", e) }
                } else {
                    if (messageModel.from_uid == fromUser) {
                        if (!messageModel.isShown) {
                            rightLay.visibility = ImageView.VISIBLE
                            leftLay.visibility = ImageView.GONE
                        }
                        Glide.with(imageRight.context)
                            .load(messageModel.message)
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                    if (!isFirstResource && !messageModel.isShown) {
                                        failAlert.visibility = View.VISIBLE
                                        progressBar.visibility = View.GONE
                                        messageModel.isShown = true
                                    }
                                    return false
                                }

                                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                    progressBar.visibility = View.GONE
                                    messageModel.isShown = true
                                    return false
                                }

                            })
                            .into(imageRight)
                        timeTextRight.visibility = View.VISIBLE
                        msgTextRight.text = messageModel.message
                        timeTextRight.text = messageModel.sendAt.toDate(format)
                    } else {
                        if (!messageModel.isShown) {
                            leftLay.visibility = ImageView.VISIBLE
                            rightLay.visibility = ImageView.GONE
                        }
                        Glide.with(imageLeft.context)
                            .load(messageModel.message)
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                    if (!messageModel.isShown) {
                                        leftFailAlert.visibility = View.VISIBLE
                                        leftProgressbar.visibility = View.GONE
                                        messageModel.isShown = true
                                    }
                                    return false
                                }

                                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                    leftProgressbar.visibility = View.GONE
                                    messageModel.isShown = true
                                    return false
                                }

                            })
                            .into(imageLeft)
                        timeTextLeft.visibility = View.VISIBLE
                        msgTextLeft.text = messageModel.message
                        timeTextLeft.text = messageModel.sendAt.toDate(format)
                    }
                }
            }
            msgTextRight.visibility = TextView.GONE
            msgTextLeft.visibility = TextView.GONE
        }
    }



    companion object {
        private const val TAG = "MessageModelViewHolder"
    }

    init {
        rightLay = itemView.findViewById<View>(R.id.rightLay) as LinearLayout
        leftLay = itemView.findViewById<View>(R.id.leftLay) as LinearLayout
        rootLay = itemView.findViewById<View>(R.id.rootLay) as LinearLayout
        leftFailAlert = itemView.findViewById<View>(R.id.leftFailImg) as ImageView
        failAlert = itemView.findViewById<View>(R.id.failImg) as ImageView
        imageLeft = itemView.findViewById<View>(R.id.imageLeft) as ImageView
        imageRight = itemView.findViewById<View>(R.id.imageRight) as ImageView
        msgTextLeft = itemView.findViewById<View>(R.id.messengerTextView) as TextView
        msgTextRight = itemView.findViewById<View>(R.id.messengerTextViewRight) as TextView
        timeTextRight = itemView.findViewById<View>(R.id.timeRight) as TextView
        timeTextLeft = itemView.findViewById<View>(R.id.timeLeft) as TextView
        date = itemView.findViewById<View>(R.id.date) as TextView
        dateLay = itemView.findViewById<View>(R.id.dateLay) as RelativeLayout
        progressBar = itemView.findViewById<View>(R.id.progress) as ProgressBar
        leftProgressbar = itemView.findViewById<View>(R.id.leftProgress) as ProgressBar
    }
}