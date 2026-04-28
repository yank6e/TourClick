package com.hfad.yultour

import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions

object ImageLoader {

    fun loadImage(imageResource: String, imageView: ImageView) {
        try {
            val resourceId = imageView.context.resources.getIdentifier(
                imageResource,
                "drawable",
                imageView.context.packageName
            )

            if (resourceId != 0) {
                Glide.with(imageView.context)
                    .load(resourceId)
                    .apply(RequestOptions()
                        .placeholder(R.drawable.tour_placeholder)
                        .error(R.drawable.tour_placeholder)
                        .transform(RoundedCorners(16))
                    )
                    .into(imageView)
            } else {
                // Если ресурс не найден, используем placeholder
                imageView.setImageResource(R.drawable.tour_placeholder)
                Log.e("ImageLoader", "Resource not found: $imageResource")
            }
        } catch (e: Exception) {
            imageView.setImageResource(R.drawable.tour_placeholder)
            Log.e("ImageLoader", "Error loading image: $imageResource", e)
        }
    }
}