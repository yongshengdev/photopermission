package com.drww.photopermission

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast

object ImageUtils {

    /**
     * 将 Bitmap 保存到系统相册
     * @param context 上下文
     * @param bitmap 要保存的图片
     * @param folderName 文件夹名称 (Pictures/ 下的子目录)
     * @param fileName 文件名 (不传则根据时间戳生成)
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        folderName: String = "PhotoPermissionDemo",
        fileName: String? = null
    ): Uri? {
        val finalFileName = fileName ?: "img_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri == null) {
            Toast.makeText(context, "保存失败：无法创建媒体记录", Toast.LENGTH_SHORT).show()
            return null
        }

        return runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw Exception("Bitmap 压缩失败")
                }
            } ?: throw Exception("无法打开输出流")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val published = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, published, null, null)
            }

            Toast.makeText(context, "图片已保存到相册", Toast.LENGTH_SHORT).show()
            uri
        }.getOrElse {
            resolver.delete(uri, null, null)
            Toast.makeText(context, "保存失败: ${it.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
}