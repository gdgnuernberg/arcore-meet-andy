package com.gutearbyte.meetandy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {
    private lateinit var andyRenderable: Renderable
    private lateinit var arFragment: PhotoArFragment
    private var selectedAndy: TransformableNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = ux_fragment as PhotoArFragment

        ModelRenderable.builder()
            .setSource(this, Uri.parse("Andy.sfb"))
            .build()
            .thenAccept { renderable: Renderable -> andyRenderable = renderable }
            .exceptionally {
                val toast = Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                null
            }

        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            // Create the Anchor.
            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)

            // Create the transformable andy and add it to the anchor.
            val andy = TransformableNode(arFragment.transformationSystem)
            andy.setParent(anchorNode)
            andy.renderable = andyRenderable
            selectedAndy = andy
            andy.select()
        }

        fabTakePicture.setOnClickListener { takePhotoWithCountdown() }
    }

    private fun takePhotoWithCountdown() {
        object : CountDownTimer(6000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                if (millisUntilFinished < 5000) {
                    tvCountdown.text = ((millisUntilFinished / 1000) + 1).toString()
                    tvCountdown.visibility = View.VISIBLE

                } else {
                    arFragment.arSceneView.planeRenderer.isVisible = false
                    fabTakePicture.hide()
                    selectedAndy?.transformationSystem?.selectNode(null)
                }
            }

            override fun onFinish() {
                tvCountdown.text = "Cheese!"
                tvCountdown.postDelayed({
                    tvCountdown.visibility = View.GONE
                    takePhoto()
                }, 2000)
            }
        }.start()
    }

    private fun takePhoto() {
        arFragment.arSceneView.planeRenderer.isVisible = true
        progressBar.visibility = View.VISIBLE
        GlobalScope.launch {
            try {
                val photoUri = arFragment.savePhoto()
                val snackbar = Snackbar.make(
                    arFragment.view!!,
                    "Photo saved", Snackbar.LENGTH_LONG
                )
                snackbar.setAction("Open in Photos") {
                    val intent = Intent(Intent.ACTION_VIEW, photoUri)
                    intent.setDataAndType(photoUri, "image/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
                snackbar.show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to save the photo", Toast.LENGTH_LONG).show()
            }
            withContext(Dispatchers.Main) { fabTakePicture.show(); progressBar.visibility = View.GONE }
        }
    }
}