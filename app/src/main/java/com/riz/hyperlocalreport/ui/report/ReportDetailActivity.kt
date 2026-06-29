package com.riz.hyperlocalreport.ui.report

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.riz.hyperlocalreport.AppContainer
import com.riz.hyperlocalreport.HyperLocalReportApp
import com.riz.hyperlocalreport.core.util.requireAuthenticatedUser
import com.riz.hyperlocalreport.core.util.toDisplayCategory
import com.riz.hyperlocalreport.core.util.toDisplayStatus
import com.riz.hyperlocalreport.databinding.ActivityReportDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportDetailBinding
    private lateinit var viewModel: ReportDetailViewModel
    private lateinit var commentAdapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reportId = intent.getStringExtra("reportId") ?: return finish()

        // Get dependencies
        val appContainer = (application as HyperLocalReportApp).container
        
        val factory = ReportDetailViewModelFactory(
            appContainer.reportRepository,
            appContainer.authRepository,
            appContainer.commentRepository,
            appContainer.upvoteRepository,
            reportId
        )
        viewModel = ViewModelProvider(this, factory)[ReportDetailViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener { finish() }

        commentAdapter = CommentAdapter()
        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(this@ReportDetailActivity)
            adapter = commentAdapter
        }

        binding.btnUpvote.setOnClickListener {
            viewModel.toggleUpvote()
        }

        binding.btnSendComment.setOnClickListener {
            val content = binding.etComment.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.addComment(content)
                binding.etComment.text?.clear()
            }
        }
        
        binding.btnUpdateStatus.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(this, view)
            popup.menu.add(com.riz.hyperlocalreport.core.common.Constants.ReportStatus.NEW)
            popup.menu.add(com.riz.hyperlocalreport.core.common.Constants.ReportStatus.VERIFIED)
            popup.menu.add(com.riz.hyperlocalreport.core.common.Constants.ReportStatus.PROCESSING)
            popup.menu.add(com.riz.hyperlocalreport.core.common.Constants.ReportStatus.DONE)
            
            popup.setOnMenuItemClickListener { menuItem ->
                viewModel.updateStatus(menuItem.title.toString())
                true
            }
            popup.show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.report.collect { report ->
                report?.let {
                    binding.tvTitle.text = it.title
                    binding.tvDescription.text = it.description
                    binding.tvCategory.text = it.category.toDisplayCategory()
                    binding.tvReporter.text = "Dilaporkan oleh: ${it.reporterName}"
                    binding.tvUpvoteCount.text = "${it.upvoteCount} Dukungan"
                    binding.chipStatus.text = it.status.toDisplayStatus()
                    binding.tvLocation.text = it.areaId.replace("KEL_", "").replace("_", " ")
                    
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    binding.tvDate.text = it.createdAt?.let { date -> dateFormat.format(date) } ?: ""

                    binding.ivShare.setOnClickListener { _ ->
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "HyperLocal Report")
                            putExtra(android.content.Intent.EXTRA_TEXT, "Lihat laporan ini di HyperLocal: ${it.title} di ${binding.tvLocation.text}")
                        }
                        startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan via"))
                    }

                    if (it.photoUrls.isNotEmpty()) {
                        binding.ivReportPhoto.visibility = View.VISIBLE
                        val photoData = it.photoUrls.first()
                        if (photoData.startsWith("base64:")) {
                            try {
                                val base64String = photoData.removePrefix("base64:")
                                val bytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                                Glide.with(this@ReportDetailActivity).load(bytes).centerCrop().into(binding.ivReportPhoto)
                            } catch (e: Exception) {
                                binding.ivReportPhoto.visibility = View.GONE
                            }
                        } else {
                            Glide.with(this@ReportDetailActivity).load(photoData).centerCrop().into(binding.ivReportPhoto)
                        }
                    } else {
                        binding.ivReportPhoto.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.comments.collect { comments ->
                commentAdapter.submitList(comments)
                binding.tvNoComments.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.isUpvotedByMe.collect { isUpvoted ->
                if (isUpvoted) {
                    binding.btnUpvote.setIconResource(android.R.drawable.arrow_up_float)
                    binding.btnUpvote.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800"))
                    binding.btnUpvote.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                } else {
                    binding.btnUpvote.setIconResource(android.R.drawable.arrow_up_float)
                    binding.btnUpvote.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#757575"))
                    binding.btnUpvote.setTextColor(android.graphics.Color.parseColor("#757575"))
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isUpvoting.collect { isUpvoting ->
                binding.btnUpvote.isEnabled = !isUpvoting
                binding.btnUpvote.alpha = if (isUpvoting) 0.5f else 1.0f
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    Toast.makeText(this@ReportDetailActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isAdmin.collect { isAdmin ->
                binding.btnUpdateStatus.visibility = if (isAdmin) View.VISIBLE else View.GONE
            }
        }
    }
}
