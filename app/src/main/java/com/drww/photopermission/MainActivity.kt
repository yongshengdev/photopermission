package com.drww.photopermission

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.drww.photopermission.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val selectedUris = mutableListOf<Uri>()
    private lateinit var previews: Array<ImageView>

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val strings = result.data?.getStringArrayListExtra(GalleryPickerActivity.EXTRA_RESULT_URIS)
            ?: return@registerForActivityResult
        selectedUris.clear()
        strings.mapTo(selectedUris) { Uri.parse(it) }
        updatePreview()
    }

    private val actionPickSingleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@registerForActivityResult
        selectedUris.clear()
        selectedUris.add(uri)
        updatePreview()
    }

    private val actionPickMultiLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@registerForActivityResult
        val uris = mutableListOf<Uri>()
        data.data?.let { uris.add(it) }
        val clip = data.clipData
        if (clip != null) {
            for (i in 0 until clip.itemCount.coerceAtMost(3)) {
                clip.getItemAt(i).uri?.let(uris::add)
            }
        }
        selectedUris.clear()
        selectedUris.addAll(uris.distinct().take(3))
        updatePreview()
    }

    private val actionGetContentSingleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@registerForActivityResult
        selectedUris.clear()
        selectedUris.add(uri)
        updatePreview()
    }

    private val actionGetContentMultiLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@registerForActivityResult
        val uris = mutableListOf<Uri>()
        data.data?.let { uris.add(it) }
        val clip = data.clipData
        if (clip != null) {
            for (i in 0 until clip.itemCount.coerceAtMost(3)) {
                clip.getItemAt(i).uri?.let(uris::add)
            }
        }
        selectedUris.clear()
        selectedUris.addAll(uris.distinct().take(3))
        updatePreview()
    }

    private val photoPickerSingleLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@registerForActivityResult
        selectedUris.clear()
        selectedUris.add(uri)
        updatePreview()
    }

    private val photoPickerMultiLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        selectedUris.clear()
        selectedUris.addAll(uris.take(3))
        updatePreview()
    }

    private val saveImagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "保存失败：未授予写入权限", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        saveDemoBitmap()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        previews = arrayOf(binding.preview0, binding.preview1, binding.preview2)
        // 你的 App 并不是在读取存储，而是在向系统申请协助。
        // 直接指向图库 App todo 如果那个第三方图库 App 实现得不标准，极少数情况下会报安全错误，但总体来说也是免权限的。
        binding.btnActionPickSingle.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).setType("image/*")
            actionPickSingleLauncher.launch(intent)
        }
        binding.btnActionPickMulti.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            actionPickMultiLauncher.launch(Intent.createChooser(intent, "选择最多3张"))
        }
        // 你的 App 并不是在读取存储，而是在向系统申请协助。
        // 会打开文档选择器，标准的文件访问框架（SAF）的一部分，它打开的是系统级的文件选择器，始终不需要权限。
        binding.btnActionGetContentSingle.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
            actionGetContentSingleLauncher.launch(intent)
        }
        binding.btnActionGetContentMulti.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            actionGetContentMultiLauncher.launch(Intent.createChooser(intent, "选择最多3张"))
        }
        binding.btnPhotoPickerSingle.setOnClickListener {
            photoPickerSingleLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.btnPhotoPickerMulti.setOnClickListener {
            photoPickerMultiLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.btnOpenGalleryPicker.setOnClickListener {
            galleryLauncher.launch(Intent(this, GalleryPickerActivity::class.java))
        }
        binding.btnSaveDemo.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveDemoBitmap()
            } else {
                saveImagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        updatePreview()
    }

    private fun updatePreview() {
        previews.forEachIndexed { index, imageView ->
            if (index < selectedUris.size) {
                imageView.visibility = View.VISIBLE
                imageView.setImageURI(selectedUris[index])
            } else {
                imageView.visibility = View.GONE
                imageView.setImageDrawable(null)
            }
        }
        binding.textPreviewEmpty.visibility =
            if (selectedUris.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun saveDemoBitmap() {
        val bitmap = drawDemoBitmap()
        ImageUtils.saveBitmapToGallery(this, bitmap)
    }

    private fun drawDemoBitmap(): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint().apply { color = Color.parseColor("#FFF8E1") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#37474F")
            textSize = 72f
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#546E7A")
            textSize = 46f
        }
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80CBC4")
        }
        canvas.drawCircle(width * 0.5f, 420f, 220f, circlePaint)
        canvas.drawText("PhotoPermission Demo", 120f, 860f, titlePaint)
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis())
        canvas.drawText("Saved at: $time", 120f, 960f, bodyPaint)
        return bitmap
    }
}
