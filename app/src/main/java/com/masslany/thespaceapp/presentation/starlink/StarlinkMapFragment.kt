package com.masslany.thespaceapp.presentation.starlink

import android.content.res.Resources
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.slider.Slider
import com.masslany.thespaceapp.R
import com.masslany.thespaceapp.databinding.FragmentMapStarlinkBinding
import com.masslany.thespaceapp.di.DefaultDispatcher
import com.masslany.thespaceapp.di.MainDispatcher
import com.masslany.thespaceapp.domain.model.CirclePreferencesModel
import com.masslany.thespaceapp.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
@ExperimentalCoroutinesApi
class StarlinkMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapStarlinkBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private val viewModel: StarlinkViewModel by viewModels()

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    private lateinit var currentIcon: BitmapDescriptor
    private lateinit var iconSmall: BitmapDescriptor
    private lateinit var iconMedium: BitmapDescriptor

    private val markers = mutableMapOf<String, Marker?>()
    private val circles = mutableMapOf<String, Circle>()

    private var isIdle = true
    private var isSettingsOpen = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapStarlinkBinding.inflate(inflater, container, false)
        MapsInitializer.initialize(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        setMapStyle()
        setupIcons()
        setupListeners()
        setupObservers()
    }

    private fun setMapStyle() {
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(), R.raw.maps_style
                )
            )
            if (!success) {
                Log.e("TAG", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("TAG", "Can't find style. Error: ", e)
        }
    }

    private fun setupIcons() {

        iconSmall = BitmapDescriptorFactory.fromResource(R.drawable.sat_icon_20)
        iconMedium = BitmapDescriptorFactory.fromResource(R.drawable.sat_icon_50)

        currentIcon = iconSmall
    }

    private fun setupObservers() {
        viewModel.markersMap.observe(viewLifecycleOwner) { data ->
            data.forEach { (id, marker) ->

                marker?.let {
                    createMarker(id = id, marker = it)
                    createOrUpdateCircle(
                        id = id,
                        marker = it,
                        preferences = viewModel.settings.value!!
                    )
                }
            }

            viewModel.predictPosition()
        }

        viewModel.starlinks.observe(viewLifecycleOwner) { state ->

            when (state) {
                is Resource.Error -> {
                }

                Resource.Loading -> {
                }

                is Resource.Success -> {

                    if (!isIdle) {
                        return@observe
                    }

                    currentIcon = if (googleMap.cameraPosition.zoom < 5.0) {
                        iconSmall
                    } else {
                        iconMedium
                    }

                    state.data.forEach { starlink ->
                        updateUi(starlink)
                    }
                }
            }
        }

        viewModel.settings.observe(viewLifecycleOwner) { preferences ->

            binding.slCoverage.value = preferences.degrees.toFloat()
            binding.cbCoverage.isChecked = preferences.showCoverage

            circles.forEach { (id, _) ->
                val satellite = viewModel.markersMap.value?.get(id) ?: return@forEach
                createOrUpdateCircle(id, satellite, preferences)
            }
        }
    }

    private fun createMarker(id: String, marker: StarlinkMarker) {
        if (markers.containsKey(id)) return

        val m = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(marker.latitude, marker.longitude))
                .title(marker.objectName)
                .icon(currentIcon)
                .visible(false)
        )
        markers[id] = m
    }

    private fun createOrUpdateCircle(
        id: String,
        marker: StarlinkMarker,
        preferences: CirclePreferencesModel
    ) {
        val radius = viewModel.calculateRadius(preferences.degrees, marker.altitude)

        if (circles[id] != null) {
            circles[id]?.isVisible = preferences.showCoverage
            circles[id]?.radius = radius
        } else {
            circles[id] = googleMap.addCircle(
                CircleOptions()
                    .center(LatLng(marker.latitude, marker.longitude))
                    .radius(radius)
                    .strokeColor(0xEE29434E.toInt())
                    .strokeWidth(0.5f)
                    .fillColor(0x6629434E)
                    .visible(preferences.showCoverage)
            )
        }
    }

    private fun setupListeners() {
        googleMap.setOnCameraIdleListener {
            isIdle = true
            viewModel.calculatePosition()
        }

        googleMap.setOnCameraMoveStartedListener {
            isIdle = false
        }

        googleMap.setOnCameraMoveListener {
            isIdle = false
        }

        binding.btnSettings.setOnClickListener {
            isSettingsOpen = !isSettingsOpen
            isIdle = !isSettingsOpen

            binding.mcvSettings.visibility = if (isSettingsOpen) View.VISIBLE else View.GONE
        }

        binding.slCoverage.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                viewModel.onSliderChanged(slider.value)
            }
        })

        binding.cbCoverage.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onShowCoverageClicked(isChecked)
        }
    }

    private fun updateUi(starlink: StarlinkMarker) {
        val curScreen = googleMap.projection.visibleRegion.latLngBounds
        val scaleFactor = 1.5
        val scaledBounds = scaleBounds(curScreen, scaleFactor, googleMap.projection)

        val position = LatLng(starlink.latitude, starlink.longitude)

        markers[starlink.id]?.position = position
        markers[starlink.id]?.setIcon(currentIcon)
        circles[starlink.id]?.center = position

        if (scaledBounds.contains(position)) {
            markers[starlink.id]?.isVisible = true
            circles[starlink.id]?.isVisible = viewModel.settings.value?.showCoverage!!
        } else {
            markers[starlink.id]?.isVisible = false
            circles[starlink.id]?.isVisible = false
        }
    }

    private fun scaleBounds(
        bounds: LatLngBounds,
        scale: Double,
        projection: Projection
    ): LatLngBounds {
        val center = bounds.center
        val centerPoint: Point = projection.toScreenLocation(center)
        val screenPositionNortheast: Point = projection.toScreenLocation(bounds.northeast)
        screenPositionNortheast.x =
            (scale * (screenPositionNortheast.x - centerPoint.x) + centerPoint.x).toInt()
        screenPositionNortheast.y =
            (scale * (screenPositionNortheast.y - centerPoint.y) + centerPoint.y).toInt()
        val scaledNortheast = projection.fromScreenLocation(screenPositionNortheast)
        val screenPositionSouthwest: Point = projection.toScreenLocation(bounds.southwest)
        screenPositionSouthwest.x =
            (scale * (screenPositionSouthwest.x - centerPoint.x) + centerPoint.x).toInt()
        screenPositionSouthwest.y =
            (scale * (screenPositionSouthwest.y - centerPoint.y) + centerPoint.y).toInt()
        val scaledSouthwest = projection.fromScreenLocation(screenPositionSouthwest)
        return LatLngBounds(scaledSouthwest, scaledNortheast)
    }
}
