package com.drww.photopermission

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as MaterialR
import com.drww.photopermission.databinding.ItemGalleryImageBinding

class GalleryImageAdapter(
    private val onItemClick: (Uri) -> Unit
) : RecyclerView.Adapter<GalleryImageAdapter.VH>() {

    private var items: List<Uri> = emptyList()
    private var selectedOrdered: List<Uri> = emptyList()

    fun submitList(all: List<Uri>, selected: List<Uri>) {
        items = all
        selectedOrdered = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGalleryImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(
        private val binding: ItemGalleryImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri) {
            binding.image.setImageURI(uri)
            val index = selectedOrdered.indexOf(uri)
            if (index >= 0) {
                binding.badge.visibility = android.view.View.VISIBLE
                binding.badge.text = (index + 1).toString()
                val primary = MaterialColors.getColor(
                    binding.card,
                    MaterialR.attr.colorPrimary,
                    Color.parseColor("#6750A4")
                )
                binding.card.strokeWidth = (4 * binding.root.resources.displayMetrics.density).toInt()
                binding.card.strokeColor = primary
            } else {
                binding.badge.visibility = android.view.View.GONE
                binding.card.strokeWidth = (1 * binding.root.resources.displayMetrics.density).toInt()
                binding.card.strokeColor = MaterialColors.getColor(
                    binding.card,
                    MaterialR.attr.colorOutline,
                    Color.GRAY
                )
            }
            binding.root.setOnClickListener { onItemClick(uri) }
        }
    }
}
