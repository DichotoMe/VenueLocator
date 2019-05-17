package com.dichotome.locator.ui

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager.widget.ViewPager
import com.dichotome.locator.R
import com.dichotome.locator.api.APIModel
import com.dichotome.locator.api.APIService
import com.dichotome.locator.api.extractItems
import com.dichotome.locator.api.toLanLng
import com.dichotome.locator.utils.LocationsPagerAdapter
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.ui.IconGenerator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ANIMATION_SHORT = 250
        const val REQUEST_CODE_PERM = 0
        const val REQUEST_ERROR = 0
        const val TAG = "MainActivity"
    }


    private val maxZoom = 18f
    private var minZoom: Float = 0f
    private var curZoom = 0f

    fun scrollMap(value: Float) {
        map?.moveCamera(CameraUpdateFactory.scrollBy(0f, value))
    }

    fun zoomMap(value: Float) {
        curZoom = minZoom + value * (maxZoom - minZoom)
        map?.moveCamera(CameraUpdateFactory.zoomTo(curZoom))
    }

    private val locationPermissions = arrayOf(
        ACCESS_FINE_LOCATION,
        ACCESS_COARSE_LOCATION
    )

    val exploreService by lazy {
        APIService.create("explore")
    }

    var disposable: Disposable? = null

    val limit = 5
    val myLat = 37.8022038
    val myLng = -122.4279103
    val query = "coffee"

    private var pagerState = 0

    private var currentFocus = LatLng(myLat, myLng)

    private lateinit var client: GoogleApiClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var map: GoogleMap? = null
    private var markerCount = 0
    private val markerColors = arrayOf(
        R.color.colorBlue,
        R.color.colorRed
    )
    private var pagerItems: Array<APIModel.Item>? = null

    private fun addMarker(position: Int) {
        pagerItems?.let { items ->
            val iconGen = IconGenerator(this).apply {
                setTextAppearance(R.style.WhiteText)
                markerColors.run {
                    get(markerCount++ % size)
                }.let {
                    setColor(ResourcesCompat.getColor(resources, it, theme))
                }
            }

            val item = items[position]
            MarkerOptions()
                .position(item.toLanLng())
                .icon(BitmapDescriptorFactory.fromBitmap(iconGen.makeIcon(item.venue.name)))
                .also {
                    map?.addMarker(it)?.tag = position
                }
        }
    }

    private fun animateMapFocus(latLng: LatLng) {
        if (currentFocus != latLng)
            currentFocus = latLng
        map?.animateCamera(CameraUpdateFactory.newLatLng(latLng), ANIMATION_SHORT, null)
    }

    private fun setMapFocus(latLng: LatLng) {
        if (currentFocus != latLng)
            currentFocus = latLng
        map?.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun setupItems(items: Array<APIModel.Item>) {
        setupPages(items)
        val bounds = LatLngBounds.Builder().run {
            for (pos in 0 until items.size) {
                addMarker(pos)
                include(items[pos].toLanLng())
            }
            build()
        }
        val margin = resources.getDimensionPixelSize(R.dimen.map_inset_margin)
        map?.apply {
            moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, margin))
            moveCamera(CameraUpdateFactory.newLatLng(items[0].toLanLng()))
            minZoom = cameraPosition?.zoom ?: 0f
        }

    }

    private fun requestData() {
        disposable = exploreService.fetchVenues(limit, "$myLat,$myLng", query)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { res ->
                res.extractItems().also {
                    setupItems(it)
                    map?.setOnMarkerClickListener { m ->
                        pager.setCurrentItem(m.tag as Int, true)
                        true
                    }
                    map_view.visibility = View.VISIBLE
                    pager.visibility = View.VISIBLE
                }
            }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkLocationPermissions()

        client = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .build()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        map_view.apply {
            onCreate(savedInstanceState)
            getMapAsync {
                map = it
                requestData()
            }
        }

        pager.pageMargin = 50
        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                pagerState = state
            }

            override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {
                pagerItems?.let { items ->
                    if (offset > 0) {
                        currentFocus = items[position].toLanLng()
                        val neighborPos = position + 1
                        if (neighborPos !in 0 until items.size) return

                        val neighborFocus = items[neighborPos].toLanLng()

                        val latDelta = (neighborFocus.latitude - currentFocus.latitude) * offset
                        val lngDelta = (neighborFocus.longitude - currentFocus.longitude) * offset

                        setMapFocus(
                            LatLng(
                                currentFocus.latitude + latDelta,
                                currentFocus.longitude + lngDelta
                            )
                        )
                    }
                }
            }

            override fun onPageSelected(position: Int) {
                var itemBefore: MapInfoFragment? = null
                var itemAfter: MapInfoFragment? = null
                (pager.adapter as LocationsPagerAdapter).pageReferenceMap.apply {
                    try {
                        itemBefore = get(position - 1) as MapInfoFragment
                    } catch (e: Exception) {
                        Log.d(TAG, "No Fragment Before")
                    }

                    try {
                        itemAfter = get(position + 1) as MapInfoFragment
                    } catch (e: Exception) {
                        Log.d(TAG, "No Fragment After")
                    }

                    val wasItemBeforeOpen = itemBefore?.finishFragment() ?: false
                    val wasItemAfterOpen = itemAfter?.finishFragment() ?: false

                    val itemNow = get(position) as MapInfoFragment
                    if (wasItemBeforeOpen || wasItemAfterOpen)
                        itemNow.openFragment()

                    pagerItems?.let {
                        animateMapFocus(it[position].toLanLng())
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()

        GoogleApiAvailability.getInstance().apply {
            isGooglePlayServicesAvailable(this@MainActivity).let { errorCode ->
                if (errorCode != ConnectionResult.SUCCESS)
                    getErrorDialog(this@MainActivity, errorCode, REQUEST_ERROR) { finish() }
                        .show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
        disposable?.dispose()
    }

    override fun onStart() {
        super.onStart()
        map_view.onStart()
        client.connect()
    }

    override fun onStop() {
        super.onStop()
        map_view.onStop()
        client.disconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view.onDestroy()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(
                this, locationPermissions,
                REQUEST_CODE_PERM
            )
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, locationPermissions[0]) == PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_PERM -> if (!hasLocationPermission()) finish()
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun setupPages(items: Array<APIModel.Item>) {
        pagerItems = items.also {
            pager.adapter = LocationsPagerAdapter(it, supportFragmentManager)
        }
    }
}