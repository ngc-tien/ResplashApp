package com.ngc.tien.resplash.modules.user

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.ngc.tien.resplash.R
import com.ngc.tien.resplash.data.remote.mapper.user.User
import com.ngc.tien.resplash.databinding.ActivityUserDetailBinding
import com.ngc.tien.resplash.modules.collections.CollectionsFragment
import com.ngc.tien.resplash.modules.core.BaseViewPagerAdapter
import com.ngc.tien.resplash.modules.photo.PhotosFragment
import com.ngc.tien.resplash.util.Constants
import com.ngc.tien.resplash.util.IntentConstants
import com.ngc.tien.resplash.util.extentions.gone
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserDetailActivity : AppCompatActivity() {
    private lateinit var user: User
    private lateinit var viewPagerAdapter: BaseViewPagerAdapter

    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityUserDetailBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initViews()
        loadData()
    }

    private fun initViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadData() {
        val user = intent.getSerializableExtra(IntentConstants.KEY_USER, User::class.java)
        if (user == null) {
            finish()
        } else {
            this.user = user
            binding.toolbar.title = user.userName
            binding.name.text = user.name
            binding.bio.text = user.bio
            if (user.location.isNotEmpty()) {
                binding.location.text = user.location
            } else {
                binding.location.gone()
            }
            binding.totalLikes.text = user.totalLikes.toString()
            binding.totalPhotos.text = user.totalPhotos.toString()
            binding.totalCollections.text = user.totalCollections.toString()
            Glide.with(this).load(user.profileImageMedium).into(binding.profileImage)
            setupViewPager()
        }
    }

    @SuppressLint("WrongConstant")
    private fun setupViewPager() {
        viewPagerAdapter = BaseViewPagerAdapter(this)
        addUserPhotoFragment()
        if (user.totalLikes != 0) {
            addLikePhotosFragment()
        }
        if (user.totalCollections != 0) {
            addCollectionFragment()
        }
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.offscreenPageLimit = Constants.SEARCH_SCREEN_PAGE_LIMIT
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = viewPagerAdapter.getPageTitle(position)
        }.attach()
    }

    private fun addUserPhotoFragment() {
        val fragment = PhotosFragment()
        val bundle = Bundle().apply {
            putString(IntentConstants.KEY_USER_PHOTOS, user.userName)
        }
        fragment.arguments = bundle
        viewPagerAdapter.addFragment(fragment, getString(R.string.photos))
    }

    private fun addLikePhotosFragment() {
        val fragment = PhotosFragment()
        val bundle = Bundle().apply {
            putString(IntentConstants.KEY_USER_LIKES, user.userName)
        }
        fragment.arguments = bundle
        viewPagerAdapter.addFragment(fragment, getString(R.string.likes))
    }

    private fun addCollectionFragment() {
        val fragment = CollectionsFragment()
        val bundle = Bundle().apply {
            putString(IntentConstants.KEY_USER_COLLECTIONS, user.userName)
        }
        fragment.arguments = bundle
        viewPagerAdapter.addFragment(fragment, getString(R.string.collections))
    }
}