package com.example.myapplication
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

class MapFragment : Fragment() {

    private lateinit var webView: WebView
    private lateinit var textCurrentLocation: TextView
    private lateinit var layoutTappedCoords: ViewGroup
    private val tappedCoords: MutableList<String> = mutableListOf()
    private val locationPermissionCode = 1000
    private lateinit var locationManager: LocationManager

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateLocation(location)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // Initialize views
        webView = view.findViewById(R.id.webView)
        textCurrentLocation = view.findViewById(R.id.text_current_location)
        layoutTappedCoords = view.findViewById(R.id.layout_tapped_coords)

        // Set up WebView
        webView.settings.apply {
            javaScriptEnabled = true
            builtInZoomControls = true  // Enables zoom controls (buttons)
            displayZoomControls = false // Hides default zoom buttons if needed
            setSupportZoom(true)        // Enables zoom gestures (pinch-to-zoom)
        }

        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/leaflet_map2.html")

        // Initialize LocationManager
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Set up buttons
        view.findViewById<Button>(R.id.button_show_coords).setOnClickListener {
            showTappedCoords()
        }
        view.findViewById<Button>(R.id.button_clear_map).setOnClickListener {
            clearMap()
        }

        // Request location permission if needed
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        } else {
            // Permission already granted, fetch location
            fetchCurrentLocation()
        }

        return view
    }

    private fun fetchCurrentLocation() {
        // Check if permission is granted
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, locationListener)

            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            location?.let { updateLocation(it) } ?: Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show()

        } else {
            // Handle case where permission is not granted
            textCurrentLocation.text = "Location permission not granted"
        }
    }

    private fun updateLocation(location: Location) {
        val lat = location.latitude
        val lng = location.longitude

        // Update the WebView with the new location
        webView.post {
            webView.evaluateJavascript(
                "javascript:map.setView([$lat, $lng], 13);",
                null
            )
        }

        // Update the TextView with the current location
        textCurrentLocation.text = "Current Location - Lat: $lat, Lng: $lng"
    }

    private fun showTappedCoords() {
        layoutTappedCoords.removeAllViews()
        tappedCoords.forEachIndexed { index, coord ->
            val textView = TextView(context).apply {
                text = "${index + 1}. $coord"
            }
            layoutTappedCoords.addView(textView)
        }
    }

    private fun clearMap() {
        tappedCoords.clear()
        layoutTappedCoords.removeAllViews()
        // Clear map markers and overlays if necessary
        webView.loadUrl("javascript:clearMap()")
    }
}
