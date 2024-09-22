package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SearchFragment : Fragment() {

    private lateinit var model: Model
    private lateinit var result: TextView
    private lateinit var confidence: TextView
    private lateinit var imageView: ImageView
    private lateinit var picture: Button
    private lateinit var galleryButton2: Button
    private val imageSize = 224
    private val GALLERY_REQUEST_CODE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = Model.newInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        result = view.findViewById(R.id.result)
        confidence = view.findViewById(R.id.confidence)
        imageView = view.findViewById(R.id.imageView)
        picture = view.findViewById(R.id.button)
        galleryButton2 = view.findViewById(R.id.galleryButton2)

        picture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, 1)
            } else {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), 100)
            }
        }

        galleryButton2.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data ?: run {
                Log.e("SearchFragment", "No image URI found")
                return
            }
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(imageUri)
            inputStream?.use {
                val bitmap = BitmapFactory.decodeStream(it)
                imageView.setImageBitmap(bitmap)
                val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width / 2, bitmap.height / 2)
                classifyImage(croppedBitmap)
            } ?: Log.e("SearchFragment", "Failed to decode image stream")
        } else {
            Log.e("SearchFragment", "Result code not OK or data is null")
        }
    }

    private fun classifyImage(image: Bitmap) {
        try {
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, imageSize, imageSize, 3), DataType.FLOAT32)
            val byteBuffer = prepareImageData(image)
            Log.d("SearchFragment", "Image data prepared")

            inputFeature0.loadBuffer(byteBuffer)
            Log.d("SearchFragment", "Model processing started")

            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
            Log.d("SearchFragment", "Model processing completed")

            val confidences = outputFeature0.floatArray
            val classes = arrayOf("Blight", "Common Rust", "Grey Leaf", "Healthy")
            var maxPos = 0
            var maxConfidence = 0f

            for (i in confidences.indices) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i]
                    maxPos = i
                }
            }

            result.text = classes[maxPos]
            confidence.text = confidences.joinToString(separator = "\n") { confidence ->
                String.format("%.1f%%", confidence * 100)
            }
        } catch (e: Exception) {
            Log.e("SearchFragment", "Error during image classification", e)
        }
    }

    private fun prepareImageData(image: Bitmap): ByteBuffer {
        val resizedImage = Bitmap.createScaledBitmap(image, imageSize, imageSize, true)
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(imageSize * imageSize)
        resizedImage.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        intValues.forEach { value ->
            byteBuffer.putFloat(((value shr 16) and 0xFF) * (1f / 255f))
            byteBuffer.putFloat(((value shr 8) and 0xFF) * (1f / 255f))
            byteBuffer.putFloat((value and 0xFF) * (1f / 255f))
        }

        return byteBuffer
    }

    override fun onDestroy() {
        model.close()
        super.onDestroy()
    }
}
