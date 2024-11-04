package com.email.imagesender

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.email.imagesender.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import android.Manifest

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var currentPhotoPath: String
    private val startForCameraResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri = FileProvider.getUriForFile(
                    this,
                    "com.email.image_sender.fileprovider",
                    File(currentPhotoPath)
                )
                binding.imagePreview.setImageURI(imageUri)
            }
        }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        controlButtons()
    }

    private fun controlButtons() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.scale_up)
        binding.btnSendEmail.setOnClickListener {
            binding.btnSendEmail.startAnimation(animation)
            sendEmail()
        }
        binding.btnTakePhoto.setOnClickListener {
            binding.btnTakePhoto.startAnimation(animation)
            takePhoto()
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            launchCameraIntent()
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun launchCameraIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }

                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.email.image_sender.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startForCameraResult.launch(takePictureIntent)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCameraIntent()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timestamp + "_"
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
        currentPhotoPath = image.absolutePath
        return image
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun sendEmail() {
        if (isInternetConnected()) {
            val email = "hodovychenko@op.edu.ua"
            val subject = "Andrushchenko Vladyslav"
            val message =
                "Дякую за курс! За Ваш час! За можливість розвиватися! Посилання на репозиторій: https://github.com/V-34-10/Image-Sender"

            try {
                val intent = Intent(Intent.ACTION_SEND).also { sendEmailIntent ->
                    sendEmailIntent.type = "text/plain"
                    sendEmailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                    sendEmailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
                    sendEmailIntent.putExtra(Intent.EXTRA_TEXT, message)
                    if (::currentPhotoPath.isInitialized) {
                        val imageUri = FileProvider.getUriForFile(
                            this,
                            "com.email.image_sender.fileprovider",
                            File(currentPhotoPath)
                        )
                        sendEmailIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
                    }
                }

                startActivity(
                    Intent.createChooser(
                        intent,
                        "Select an application for sending mail"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this, "No internet connection!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isInternetConnected(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}