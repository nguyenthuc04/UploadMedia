package com.honest.uploadmedia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.honest.uploadmedia.databinding.ActivityVideoUploadBinding

class VideoUploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoUploadBinding
    private lateinit var cloudinaryUploader: CloudinaryUploader
    private var selectedVideoUri: Uri? = null
    private val PICK_VIDEO_REQUEST = 1
    private val PERMISSION_REQUEST_CODE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVideoUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v ,insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left ,systemBars.top ,systemBars.right ,systemBars.bottom)
            insets
        }
        cloudinaryUploader = CloudinaryUploader(this)

        binding.buttonChooseVideo.setOnClickListener {
            checkPermissionAndPickVideo()
        }

        binding.buttonUpload.setOnClickListener {
            uploadVideo()
        }
    }
    private fun checkPermissionAndPickVideo() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                checkAndRequestPermission(Manifest.permission.READ_MEDIA_VIDEO)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                pickVideo()
            }
        }
    }

    private fun checkAndRequestPermission(permission: String) {
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                pickVideo()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                showPermissionRationale(permission)
            }
            else -> {
                requestPermission(permission)
            }
        }
    }

    private fun showPermissionRationale(permission: String) {
        Toast.makeText(this, "Ứng dụng cần quyền truy cập video để chọn và tải lên", Toast.LENGTH_LONG).show()
        requestPermission(permission)
    }

    private fun requestPermission(permission: String) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
    }

    private fun pickVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_VIDEO_REQUEST)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickVideo()
                } else {
                    Toast.makeText(this, "Quyền truy cập bị từ chối. Không thể chọn video.", Toast.LENGTH_LONG).show()
                    showManualPermissionInstructions()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedVideoUri = data.data
            showSelectedVideo()
        }
    }

    private fun showSelectedVideo() {
        selectedVideoUri?.let { uri ->
            binding.videoViewSelected.setVideoURI(uri)
            binding.videoViewSelected.visibility = View.VISIBLE
            binding.videoViewSelected.start()
        }
    }

    private fun uploadVideo() {
        selectedVideoUri?.let { uri ->
            binding.progressBar.visibility = View.VISIBLE
            binding.textViewStatus.text = "Đang tải lên..."
            cloudinaryUploader.uploadMedia(uri, true, object : CloudinaryUploader.UploadCallback {
                override fun onProgress(progress: Int) {
                    binding.progressBar.progress = progress
                }

                override fun onSuccess(url: String) {
                    binding.progressBar.visibility = View.GONE
                    binding.textViewStatus.text = "Tải lên thành công"
                    showUploadedVideo(url)
                }

                override fun onError(errorMessage: String) {
                    binding.progressBar.visibility = View.GONE
                    binding.textViewStatus.text = "Lỗi: $errorMessage"
                }
            })
        } ?: run {
            Toast.makeText(this, "Vui lòng chọn một video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUploadedVideo(url: String) {
        binding.videoViewUploaded.setVideoPath(url)
        binding.videoViewUploaded.visibility = View.VISIBLE
        val mediaController = MediaController(this)
        binding.videoViewUploaded.setMediaController(mediaController)
        mediaController.setAnchorView(binding.videoViewUploaded)
        binding.videoViewUploaded.start()
    }

    private fun showManualPermissionInstructions() {
        val message = "Để cấp quyền thủ công, vui lòng:\n" +
                "1. Mở Cài đặt điện thoại\n" +
                "2. Tìm và chọn ứng dụng này\n" +
                "3. Chọn Quyền\n" +
                "4. Cấp quyền truy cập Video"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}