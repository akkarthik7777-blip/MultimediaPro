
package com.multimediapro.editor

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.MediaItem
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File

class MainActivity : ComponentActivity() {

    private var selectedVideo: Uri? = null
    private lateinit var preview: VideoView
    private lateinit var status: TextView

    private val chooseVideo =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedVideo = uri
                preview.setVideoURI(uri)
                preview.setOnPreparedListener { it.start() }
                status.text = "Video selected"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 32)
        }

        val title = TextView(this).apply {
            text = "Multimedia Pro"
            textSize = 28f
            gravity = Gravity.CENTER
        }

        preview = VideoView(this)
        status = TextView(this).apply {
            text = "Choose a video to begin"
            textSize = 16f
        }

        val pickButton = Button(this).apply {
            text = "Import Video"
            setOnClickListener { chooseVideo.launch("video/*") }
        }

        val exportButton = Button(this).apply {
            text = "Export First 10 Seconds"
            setOnClickListener { exportTrim() }
        }

        layout.addView(title)
        layout.addView(
            preview,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 420
            )
        )
        layout.addView(pickButton)
        layout.addView(exportButton)
        layout.addView(status)

        setContentView(layout)
    }

    private fun exportTrim() {
        val uri = selectedVideo
        if (uri == null) {
            Toast.makeText(
                this, "Choose a video first", Toast.LENGTH_SHORT
            ).show()
            return
        }

        val outputDir = getExternalFilesDir("exports")
        if (outputDir == null) {
            status.text = "Export folder unavailable"
            return
        }

        val output = File(outputDir, "multimedia_pro_trim.mp4")
        output.delete()
        status.text = "Exporting video..."

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(0)
                    .setEndPositionMs(10_000)
                    .build()
            )
            .build()

        val editedItem = EditedMediaItem.Builder(mediaItem).build()

        val transformer = Transformer.Builder(this)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(
                    composition: androidx.media3.transformer.Composition,
                    exportResult: ExportResult
                ) {
                    runOnUiThread {
                        status.text = "Export complete: ${output.absolutePath}"
                    }
                }

                override fun onError(
                    composition: androidx.media3.transformer.Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    runOnUiThread {
                        status.text = "Export failed: ${exportException.message}"
                    }
                }
            })
            .build()

        try {
            transformer.start(editedItem, output.absolutePath)
        } catch (e: Exception) {
            status.text = "Cannot start export: ${e.message}"
        }
    }
}
