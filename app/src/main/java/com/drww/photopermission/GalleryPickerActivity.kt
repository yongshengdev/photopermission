package com.drww.photopermission

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.drww.photopermission.MediaGalleryUtils.PermissionState
import com.drww.photopermission.databinding.ActivityGalleryPickerBinding
import kotlinx.coroutines.launch

class GalleryPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryPickerBinding
    private val selectedUris = mutableListOf<Uri>()
    private val allUris = mutableListOf<Uri>()
    private lateinit var adapter: GalleryImageAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissionUi()
        loadImagesIfPossible()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = GalleryImageAdapter { uri ->
            val idx = selectedUris.indexOf(uri)
            if (idx >= 0) {
                selectedUris.removeAt(idx)
            } else if (selectedUris.size >= 3) {
                Toast.makeText(this, "最多选择3张", Toast.LENGTH_SHORT).show()
            } else {
                selectedUris.add(uri)
            }
            updateSelectionUi()
            adapter.submitList(allUris, selectedUris.toList())
        }

        binding.recyclerGallery.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerGallery.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnConfirm.setOnClickListener {
            val result = Intent().apply {
                putStringArrayListExtra(
                    EXTRA_RESULT_URIS,
                    ArrayList(selectedUris.map { it.toString() })
                )
            }
            setResult(RESULT_OK, result)
            finish()
        }
        binding.btnUpdatePermission.setOnClickListener {
            permissionLauncher.launch(MediaGalleryUtils.readMediaPermissions())
        }

        refreshPermissionUi()
        updateSelectionUi()

        if (MediaGalleryUtils.getMediaPermissionState(this) == PermissionState.None) {
            permissionLauncher.launch(MediaGalleryUtils.readMediaPermissions())
        } else {
            loadImagesIfPossible()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionUi()
    }

    private fun refreshPermissionUi() {
        val state = MediaGalleryUtils.getMediaPermissionState(this)
        binding.textPermissionStatus.text = when (state) {
            PermissionState.Full -> "完整权限：可浏览全部图片"
            PermissionState.Partial ->
                "部分权限：仅用户选中的图片 (READ_MEDIA_VISUAL_USER_SELECTED)"
            PermissionState.None -> "未授权：请申请权限"
        }
    }

    private fun updateSelectionUi() {
        binding.textSelectedCount.text = "已选 ${selectedUris.size}/3"
    }

    private fun loadImagesIfPossible() {
        if (MediaGalleryUtils.getMediaPermissionState(this) == PermissionState.None) {
            allUris.clear()
            adapter.submitList(emptyList(), selectedUris.toList())
            return
        }
        lifecycleScope.launch {
            val list = MediaGalleryUtils.loadGalleryUris(contentResolver)
            allUris.clear()
            allUris.addAll(list)
            adapter.submitList(allUris, selectedUris.toList())
        }
    }

    companion object {
        const val EXTRA_RESULT_URIS = "extra_result_uris"
    }
}
