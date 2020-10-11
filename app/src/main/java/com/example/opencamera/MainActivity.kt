package com.example.opencamera


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

val permissions = arrayOf(
    android.Manifest.permission.CAMERA,
    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
    android.Manifest.permission.READ_EXTERNAL_STORAGE
)

class MainActivity : AppCompatActivity() {

    private var lensFacing = CameraX.LensFacing.FRONT
    private var imageCapture: ImageCapture? = null
    private var imagePreView: Preview? = null
    private lateinit var imageAnalysis: ImageAnalysis
    private var textureView: TextureView? = null

    companion object{
        val initialized = OpenCVLoader.initDebug()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.textureView = findViewById(R.id.view_finder)

        CameraX.unbindAll()

        imagePreView = previewCreation()
        imageCapture = imageCaptureCreation()
        imageAnalysis = imageAnalyzer()

        CameraX.bindToLifecycle(this as LifecycleOwner, imageCapture, imagePreView, imageAnalysis)

    }

    private fun hasNoPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 0)
    }

    private fun imageCaptureCreation(): ImageCapture {

        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .setTargetRotation(windowManager.defaultDisplay.rotation)
            .setLensFacing(lensFacing)
            .setFlashMode(FlashMode.ON)
            .build()

        return ImageCapture(imageCaptureConfig)

    }

    private fun imageAnalyzer(): ImageAnalysis {

        val analyzerThread = HandlerThread("OpenCVAnalysis")
        analyzerThread.start()

        val imageConfigAnalysis = ImageAnalysisConfig.Builder()
            .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            .setLensFacing(lensFacing)
            .setCallbackHandler(Handler(analyzerThread.looper))
            .setImageQueueDepth(1).build()

        val imageAnalzer = ImageAnalysis(imageConfigAnalysis)

        imageAnalzer.setAnalyzer { image, rotationDegrees ->

            val bitmap = this.textureView?.bitmap
            val matOpenCV = Mat()
            Utils.bitmapToMat(bitmap, matOpenCV);
            Imgproc.cvtColor(matOpenCV, matOpenCV, Imgproc.COLOR_BGR2GRAY);
            Utils.matToBitmap(matOpenCV, bitmap);
            runOnUiThread { ivBitmap.setImageBitmap(bitmap) }

        }
        return imageAnalzer
    }

    private fun previewCreation(): Preview {
        val previewConfig = PreviewConfig.Builder()
            .setLensFacing(lensFacing)
            .build()
        val preview = Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener { previewOutput ->

            this.textureView?.surfaceTexture = previewOutput.surfaceTexture
        }
        return preview

    }

    override fun onStart() {
        super.onStart()

        if (hasNoPermissions()) {
            requestPermission()
        }
    }
}