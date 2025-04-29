package com.android.code.ui.binding

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager

object ViewBinding {

    @JvmStatic
    @BindingAdapter("loadImage")
    fun bindLoadImage(imageView: ImageView, url: String?) {
        url ?: return

        Glide.with(imageView.context).load(url).into(imageView)
    }

    @JvmStatic
    @BindingAdapter("loadImage", "requestManager")
    fun bindLoadImage(imageView: ImageView, url: String?, requestManager: RequestManager?) {
        url ?: return
        requestManager ?: return

        requestManager.load(url).into(imageView)
    }
}
