package com.riz.hyperlocalreport.ui.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.riz.hyperlocalreport.core.util.toDisplayCategory
import com.riz.hyperlocalreport.core.util.toDisplayStatus
import com.riz.hyperlocalreport.databinding.ItemReportBinding
import com.riz.hyperlocalreport.domain.model.ReportUiModel
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportAdapter(
    private val onItemClick: (String) -> Unit,
    private val onUpvoteClick: (String) -> Unit
) : ListAdapter<ReportUiModel, ReportAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding, onItemClick, onUpvoteClick)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReportViewHolder(
        private val binding: ItemReportBinding,
        private val onItemClick: (String) -> Unit,
        private val onUpvoteClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(model: ReportUiModel) {
            val report = model.report
            binding.tvTitle.text = report.title
            binding.tvStatus.text = report.status.toDisplayStatus()
            binding.tvCategory.text = report.category.toDisplayCategory()
            
            // Format time correctly for relative time (e.g., 2 jam lalu) - fallback to date if hard
            val now = System.currentTimeMillis()
            val time = report.createdAt?.time ?: now
            val diff = now - time
            
            val relativeTime = when {
                diff < android.text.format.DateUtils.MINUTE_IN_MILLIS -> "Baru saja"
                diff < android.text.format.DateUtils.HOUR_IN_MILLIS -> "${diff / android.text.format.DateUtils.MINUTE_IN_MILLIS} menit lalu"
                diff < android.text.format.DateUtils.DAY_IN_MILLIS -> "${diff / android.text.format.DateUtils.HOUR_IN_MILLIS} jam lalu"
                diff < android.text.format.DateUtils.DAY_IN_MILLIS * 2 -> "Kemarin"
                else -> "${diff / android.text.format.DateUtils.DAY_IN_MILLIS} hari lalu"
            }
            binding.tvDate.text = relativeTime
            binding.tvLocation.text = report.areaId.replace("KEL_", "").replace("_", " ")

            // Set background color of status pill based on status
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            drawable.cornerRadius = 16f
            when (report.status) {
                com.riz.hyperlocalreport.core.common.Constants.ReportStatus.NEW -> drawable.setColor(android.graphics.Color.parseColor("#F44336"))
                com.riz.hyperlocalreport.core.common.Constants.ReportStatus.VERIFIED -> drawable.setColor(android.graphics.Color.parseColor("#FF9800"))
                com.riz.hyperlocalreport.core.common.Constants.ReportStatus.PROCESSING -> drawable.setColor(android.graphics.Color.parseColor("#2196F3"))
                com.riz.hyperlocalreport.core.common.Constants.ReportStatus.DONE -> drawable.setColor(android.graphics.Color.parseColor("#4CAF50"))
                else -> drawable.setColor(android.graphics.Color.GRAY)
            }
            binding.tvStatus.background = drawable

            if (report.photoUrls.isNotEmpty()) {
                val photoData = report.photoUrls.first()
                binding.ivReportPhoto.visibility = android.view.View.VISIBLE
                if (photoData.startsWith("base64:")) {
                    try {
                        val base64String = photoData.removePrefix("base64:")
                        val bytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                        Glide.with(binding.root.context)
                            .load(bytes)
                            .centerCrop()
                            .into(binding.ivReportPhoto)
                    } catch (e: Exception) {
                        binding.ivReportPhoto.visibility = android.view.View.GONE
                    }
                } else {
                    Glide.with(binding.root.context)
                        .load(photoData)
                        .centerCrop()
                        .into(binding.ivReportPhoto)
                }
            } else {
                binding.ivReportPhoto.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(report.reportId)
            }
        }
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<ReportUiModel>() {
        override fun areItemsTheSame(oldItem: ReportUiModel, newItem: ReportUiModel): Boolean {
            return oldItem.report.reportId == newItem.report.reportId
        }

        override fun areContentsTheSame(oldItem: ReportUiModel, newItem: ReportUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
