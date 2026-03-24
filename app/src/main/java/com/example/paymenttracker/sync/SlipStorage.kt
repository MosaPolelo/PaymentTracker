package com.example.paymenttracker.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SlipStorage {

    fun createTempSlipUri(context: Context): Uri {
        val slipsDir = File(context.filesDir, "payment_slips").apply {
            if (!exists()) mkdirs()
        }

        val fileName = "slip_${timestamp()}.jpg"
        val file = File(slipsDir, fileName)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun copyUriToAppStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val slipsDir = File(context.filesDir, "payment_slips").apply {
                if (!exists()) mkdirs()
            }

            val fileName = "slip_${timestamp()}.jpg"
            val destFile = File(slipsDir, fileName)

            val bitmap = decodeSampledBitmapFromUri(
                context = context,
                sourceUri = sourceUri,
                reqWidth = 1400,
                reqHeight = 1400
            ) ?: return null

            FileOutputStream(destFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, output)
            }

            bitmap.recycle()

            destFile.toURI().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeSampledBitmapFromUri(
        context: Context,
        sourceUri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
        }

        val sampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            return BitmapFactory.decodeStream(input, null, decodeOptions)
        }

        return null
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize.coerceAtLeast(1)
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
}