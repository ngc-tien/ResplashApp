package com.ngc.tien.resplash.modules.photo.detail

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionListenerAdapter
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.viewmodel.viewModelFactory
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.google.android.material.chip.Chip
import com.ngc.tien.resplash.R
import com.ngc.tien.resplash.data.remote.ResplashServiceLocator
import com.ngc.tien.resplash.databinding.ActivityPhotoDetailBinding
import com.ngc.tien.resplash.modules.photo.zoom.PhotoZoomActivity
import com.ngc.tien.resplash.util.Constants
import com.ngc.tien.resplash.util.IntentConstants.KEY_PHOTO_COLOR
import com.ngc.tien.resplash.util.IntentConstants.KEY_PHOTO_HEIGHT
import com.ngc.tien.resplash.util.IntentConstants.KEY_PHOTO_ID
import com.ngc.tien.resplash.util.IntentConstants.KEY_PHOTO_URL
import com.ngc.tien.resplash.util.IntentConstants.KEY_PHOTO_WIDTH
import com.ngc.tien.resplash.util.ViewUtils
import com.ngc.tien.resplash.util.extentions.formatNumber
import com.ngc.tien.resplash.util.extentions.gone
import com.ngc.tien.resplash.util.extentions.pauseAndGone
import com.ngc.tien.resplash.util.extentions.playAndShow
import com.ngc.tien.resplash.util.extentions.transparent
import com.ngc.tien.resplash.util.extentions.visible

class PhotoDetailActivity : AppCompatActivity() {
    private lateinit var sharedEnterTransitionListener: Transition.TransitionListener
    private lateinit var sharedExitTransitionListener: Transition.TransitionListener
    private var photoUrl = ""
    private var photoWidth = 0
    private var photoHeight = 0
    private var onBackPressed: Boolean = false
    private var photoBitmap: Bitmap? = null

    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPhotoDetailBinding.inflate(layoutInflater)
    }

    private val viewModel by viewModels<PhotoDetailViewModel>(
        factoryProducer = {
            viewModelFactory {
                addInitializer(PhotoDetailViewModel::class) {
                    PhotoDetailViewModel(
                        resplashApiService = ResplashServiceLocator.resplashApiService
                    )
                }
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        loadIntentData()
        initViews()
        addListener()
        bindingData()
    }

    private fun loadIntentData() {
        intent?.let {
            binding.photoImage.setBackgroundColor(Color.parseColor(it.getStringExtra(KEY_PHOTO_COLOR)))
            binding.userImage.setBackgroundColor(Color.parseColor(it.getStringExtra(KEY_PHOTO_COLOR)))
            Glide.with(this)
                .asBitmap()
                .load(it.getStringExtra(KEY_PHOTO_URL))
                .into(object : BitmapImageViewTarget(binding.photoImage) {
                    override fun setResource(resource: Bitmap?) {
                        photoBitmap = resource
                        super.setResource(resource)
                    }
                })
            photoUrl = it.getStringExtra(KEY_PHOTO_URL) ?: ""
            photoWidth = it.getIntExtra(KEY_PHOTO_WIDTH, 0)
            photoHeight = it.getIntExtra(KEY_PHOTO_HEIGHT, 0)
            if (viewModel.uiState.value !is PhotoDetailUIState.Content) {
                viewModel.getPhoto(it.getStringExtra(KEY_PHOTO_ID) ?: "")
            }
        }
    }

    private fun addListener() {
        binding.toolBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        onBackPressedDispatcher.addCallback {
            binding.locationWrapper.gone()
            binding.appBarLayout.gone()
            binding.photoDetail.gone()
            binding.photoImage.foreground = null
            onBackPressed = true
            window.sharedElementEnterTransition.removeListener(sharedEnterTransitionListener)
            finishAfterTransition()
            setPhotoImageFitToScreen()
            binding.photoImage.post(::finishAfterTransition)
        }
        binding.photoImage.setOnClickListener {
            setPhotoImageFitToScreen()
            binding.photoImage.post {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    it, Constants.SHARED_PHOTO_TRANSITION_NAME
                )
                Intent(this, PhotoZoomActivity::class.java).apply {
                    putExtra(KEY_PHOTO_URL, photoUrl)
                    startActivity(this, options.toBundle())
                }
            }
        }
        addSharedWindowTransitionAnimation()
    }

    private fun setPhotoImageFitToScreen() {
        val params = binding.photoImage.layoutParams
        var photoWrapperWidth = ViewUtils.getScreenWidth()
        params.width = photoWrapperWidth
        params.height = ((photoWrapperWidth.toFloat() / photoWidth) * photoHeight).toInt()
        binding.photoImage.layoutParams = params
        if (photoBitmap != null) {
            binding.photoImage.setImageBitmap(photoBitmap!!)
        } else {
            binding.photoImage.setImageDrawable(binding.photoImage.drawable)
        }
        binding.photoImage.scaleType = ImageView.ScaleType.CENTER
    }

    private fun resetPhotoImageState() {
        val params = binding.photoImage.layoutParams
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = resources.getDimensionPixelSize(R.dimen.photo_detail_min_height)
        binding.photoImage.layoutParams = params
        if (photoBitmap != null) {
            binding.photoImage.setImageBitmap(photoBitmap!!)
        } else {
            binding.photoImage.setImageDrawable(binding.photoImage.drawable)
        }
        binding.photoImage.scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private fun addSharedWindowTransitionAnimation() {
        // shared enter/return is used to handle transition between PhotoDetail and HomeFragment
        // shared exit transition is used to handle transition between PhotoDetail and PhotoZoom
        sharedEnterTransitionListener = object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition?) {
                viewModel.isTransitionFinished = true
                renderUiState(viewModel.uiState.value)
            }
        }
        sharedExitTransitionListener = object : TransitionListenerAdapter() {
            override fun onTransitionStart(transition: Transition?) {
                binding.locationWrapper.transparent(true)
                binding.appBarLayout.transparent(true)
                binding.photoDetail.transparent(true)
            }
        }
        window.sharedElementEnterTransition.addListener(sharedEnterTransitionListener)
        window.sharedElementExitTransition.addListener(sharedExitTransitionListener)
    }

    private fun initViews() {
        binding.lottieLoading.apply {
            setAnimation(R.raw.lottie_loading)
            repeatCount = LottieDrawable.INFINITE;
        }
        binding.toolBar.menu.forEach { menu ->
            menu.icon?.setTint(resources.getColor(R.color.white, null))
        }
    }

    private fun bindingData() {
        viewModel.uiState.observe(
            this,
            ::renderUiState
        )
    }

    private fun renderUiState(uiState: PhotoDetailUIState?) {
        when (uiState) {
            is PhotoDetailUIState.Content -> renderPhotoDetail(uiState)
            is PhotoDetailUIState.Error -> binding.lottieLoading.pauseAndGone()
            is PhotoDetailUIState.Loading -> binding.lottieLoading.playAndShow()
            else -> {}
        }
    }

    private fun renderPhotoDetail(uiState: PhotoDetailUIState.Content) {
        if (!viewModel.isTransitionFinished) {
            binding.photoDetail.gone()
            return
        }

        val item = uiState.item
        binding.lottieLoading.pauseAndGone()
        binding.photoDetail.visible()
        binding.appBarLayout.visible()
        if (item.location.isNotEmpty()) {
            binding.locationWrapper.visible()
            binding.locationName.text = item.location
        } else {
            binding.locationWrapper.gone()
        }
        Glide.with(this)
            .load(item.userImage)
            .into(binding.userImage)
        item.run {
            binding.userName.text = userName
            binding.txtCamera.text = camInfo
            binding.txtAperture.text = aperture
            binding.txtFocalLength.text = focalLength
            binding.txtShutterSpeed.text = shutterSpeed
            binding.txtISO.text = item.iso
            binding.txtDimentions.text = "${item.width} x ${item.height}"
            binding.txtTotalViews.text = totalViews.formatNumber()
            binding.txtTotalDownloads.text = totalDownloads.formatNumber()
            binding.txtTotalLikes.text = totalLikes.formatNumber()
            for (tag in tags) {
                val layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                layoutParams.marginEnd =
                    this@PhotoDetailActivity.resources.getDimensionPixelSize(R.dimen.dimen_8dp)
                val chip = Chip(this@PhotoDetailActivity).apply {
                    text = tag
                }
                binding.tags.addView(chip, layoutParams)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        resetPhotoImageState()
    }

    override fun onResume() {
        super.onResume()
        binding.locationWrapper.transparent(false)
        binding.appBarLayout.transparent(false)
        binding.photoDetail.transparent(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        window.sharedElementEnterTransition.removeListener(sharedEnterTransitionListener)
        window.sharedElementExitTransition.removeListener(sharedExitTransitionListener)
    }
}
