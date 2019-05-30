package com.gutearbyte.meetandy

import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*


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
        object : CountDownTimer(4000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                tvCountdown.text = ((millisUntilFinished / 1000) + 1).toString()
            }

            override fun onFinish() {
                tvCountdown.text = "Cheese!"
                tvCountdown.postDelayed({
                    tvCountdown.visibility = View.INVISIBLE
                    fabTakePicture.show()
                    arFragment.arSceneView.planeRenderer.isEnabled = true
                }, 1000)
                arFragment.takePhoto()
            }
        }.start()
        // Hide all the chrome for a nice photo
        arFragment.arSceneView.planeRenderer.isEnabled = false
        tvCountdown.visibility = View.VISIBLE
        fabTakePicture.hide()
        selectedAndy?.transformationSystem?.selectNode(null)
    }
}