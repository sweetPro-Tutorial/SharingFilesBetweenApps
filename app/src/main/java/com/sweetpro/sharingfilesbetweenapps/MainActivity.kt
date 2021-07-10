package com.sweetpro.sharingfilesbetweenapps

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import com.sweetpro.sharingfilesbetweenapps.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private final val TAG="MainActivity"

    // for camera picker
    var photoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        resultLauncherForPermission.launch(neededRuntimePermissions)

        binding.button.setOnClickListener {
            openCameraAppToPick(it)
        }
    }

    private fun openCameraAppToPick(view: View) {
        // prepare a file for saving the captured photo.
        photoFile = prepareEmptyPhotoFile("photo.jpg")
        if (photoFile == null) {
            Snackbar.make(
                view,
                "Unable to make a file for the captured photo.",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        // note:
        // Because the direct passing a file pointer to the other app is not recommended on Android 7,
        // it's needed to use Android FileProvider to get the content URI from a file.
        // Google calls it as the only secure way to offer a file from your app to another app.
        //
        // For example, intent.putExtra(MediaStore.EXTRA_OUTPUT, anyFile) is deprecated on API 24.
        // It's needed to use content uri.
        val photoUri = getUriFromFile(photoFile!!)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            // call camera app using registerForActivityResult by passing contract,
            // to handle the result value(OK or CANCELED) after camera app is finished.
            resultLauncherForCameraResult.launch(cameraIntent)
        } else {
            Snackbar.make(view, "Unable to open camera", Snackbar.LENGTH_SHORT).show()
        }
    }

    // region:- Activity Result Handler
    // to handle the result value(ok or cancel) after camera app is finished.
    private val resultLauncherForCameraResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d(TAG, ": RESULT_OK")
                binding.imageView.setImageURI(getUriFromFile(photoFile!!))
            } else {
                Log.d(TAG, ": RESULT_CANCELED")
                photoFile?.delete()
            }
        }
    // endregion----

    private fun getUriFromFile(photoFile: File): Uri? {
        return FileProvider.getUriForFile(this,
            "com.sweetpro.sharingfilesbetweenapps.fileprovider",
            photoFile)
    }

    private fun prepareEmptyPhotoFile(filename: String): File? {
        // to get app specific directory for saving photo which taken by camera app
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        var tempFile: File? = null
        try {
            tempFile = File.createTempFile(filename, ".jpg", storageDirectory)
        } catch (e: IOException) {
            Log.d(TAG, "prepareEmptyPhotoFile: ${e.message}")
        }
        return tempFile
    }

    // region:- Routine
    // view binding and requesting runtime permission(s)
    //
    // permission:
    // for requesting runtime permission(s) using new API
    private val neededRuntimePermissions = arrayOf(android.Manifest.permission.CAMERA)
    private val resultLauncherForPermission =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.d(TAG, ": registerForActivityResult: ${it.key}=${it.value}")

                    // if any permission is not granted...
                    if (! it.value) {
                        // do anything if needed: ex) display about limitation
                        Snackbar.make(binding.root, R.string.permissions_request, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }

    // view binding
    lateinit var binding: ActivityMainBinding
    // endregion----

}