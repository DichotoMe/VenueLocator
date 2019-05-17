package com.dichotome.locator.ui

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent.ACTION_UP
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.dichotome.locator.R
import com.dichotome.locator.api.APIModel
import com.dichotome.locator.api.APIService
import com.dichotome.locator.ui.MainActivity.Companion.TAG
import com.dichotome.locator.utils.constants.DISPLAY_HEIGHT
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_map_info.*
import java.lang.Math.abs
import java.lang.Math.pow

class MapInfoFragment : Fragment() {

    private var minTileHeight: Int = -1
    private var maxScrollHeight: Int = -1
    private var maxDelta: Int = -1

    private var speed = 0
    private var curScrollY = 0

    companion object {
        const val TITLE = "title"
        const val ADDRESS = "location"
        const val PIC_ID = "picId"
    }

    private lateinit var pictureService: APIService
    private lateinit var disposable: Disposable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_map_info, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        arguments?.apply {
            title.text = getString(TITLE, "")
            address.text = getString(ADDRESS, "")
            val id = getString(PIC_ID, "")
            Log.d(TAG, id)
            pictureService = APIService.create("$id/photos")
            disposable = pictureService.fetchPhotos()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn {
                    errorMessage.text = it.message
                    APIModel.PictureResult()
                }
                .subscribe { res ->
                    Log.d(TAG, res.toString())
                    context?.let { ctx ->
                        res.response?.let {
                            it.photos.items[0].apply {
                                Glide.with(ctx)
                                    .load("${prefix}300x500${suffix}")
                                    .into(pic)
                            }
                        }
                    }
                }
        }

        tile.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, DISPLAY_HEIGHT * 2 / 10)
        margin.layoutParams =
            LinearLayout.LayoutParams(MATCH_PARENT, DISPLAY_HEIGHT - tile.layoutParams.height)
        content.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, DISPLAY_HEIGHT * 6 / 10)
        bottom.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, DISPLAY_HEIGHT / 10)

        minTileHeight = tile.layoutParams.height
        maxScrollHeight = content.layoutParams.height + bottom.layoutParams.height
        maxDelta = maxScrollHeight - minTileHeight

        view?.findViewById<ScrollView>(R.id.scroller)?.apply {
            setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                curScrollY = scrollY
                speed = scrollY - oldScrollY

                (activity as? MainActivity)?.apply {
                    scrollMap(
                        (scrollY - oldScrollY).toFloat() * pow(
                            (scrollY / maxScrollHeight.toDouble()),
                            3.0
                        ).toFloat()
                    )
                    val progress = scrollY / maxScrollHeight.toFloat()
                    zoomMap(progress)
                }
            }
            setOnTouchListener { v, event ->
                var res = false
                if (event.action == ACTION_UP) {
                    if (curScrollY >= maxScrollAmount * 0.7)
                        if (speed >= 0 || abs(speed) < 70) {
                            animateToMaxSize()
                            res = true
                        }
                    if (curScrollY < maxScrollAmount * 0.7)
                        if (speed <= 0 || abs(speed) < 70) {
                            animateToMinSize()
                            res = true
                        }
                }
                res
            }
        }
    }

    private fun animateToMaxSize() {
        scroller?.apply {
            ObjectAnimator.ofInt(this, "scrollY", curScrollY, maxScrollHeight).start()
        }
    }

    private fun animateToMinSize() {
        scroller?.apply {
            ObjectAnimator.ofInt(this, "scrollY", curScrollY, 0).start()
        }
    }

    fun openFragment() = animateToMaxSize()

    fun finishFragment(): Boolean {
        val isFragmentOpen = curScrollY == maxScrollHeight
        animateToMinSize()
        return isFragmentOpen
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "TAG")
    }
}