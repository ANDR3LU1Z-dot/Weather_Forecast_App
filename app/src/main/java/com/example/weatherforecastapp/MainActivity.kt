package com.example.weatherforecastapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.databinding.DataBindingUtil
import com.example.weatherforecastapp.databinding.ActivityMainBinding
import com.example.weatherforecastapp.model.ModelMainData
import com.example.weatherforecastapp.network.ApiUtilities
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.log

class MainActivity : AppCompatActivity() {

    private lateinit var activityMainBinding: ActivityMainBinding

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar?.hide()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        activityMainBinding.rlMainLayout.visibility = View.GONE
        getCurrentLocation()

        activityMainBinding.etGetCityName.setOnEditorActionListener ({ v, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                getCityWeather(activityMainBinding.etGetCityName.text.toString())
                val view = this.currentFocus
                if (view != null) {
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    activityMainBinding.etGetCityName.clearFocus()
                }
                true
            }
            else false
        })
    }

    private fun getCityWeather(cityName: String) {
        activityMainBinding.pbLoading.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCityWeatherData(cityName, API_KEY)
            ?.enqueue(object : Callback<ModelMainData> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<ModelMainData>,
                    response: Response<ModelMainData>
                ) {
                    setDataInViews(response.body())
                }

                override fun onFailure(call: Call<ModelMainData>, t: Throwable) {
                    Toast.makeText(applicationContext, "Not a Valid City Name", Toast.LENGTH_SHORT)
                        .show()

                }

            })
    }

    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                //final latitude and longitude code here....
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    requestPermission()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        Toast.makeText(applicationContext, "Null Recieved", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        fetchCurrentLocation(location.latitude.toString(), location.longitude.toString())
                        Toast.makeText(applicationContext, "Get Success", Toast.LENGTH_SHORT).show()
                        Log.i("latitude", "${location.latitude}")
                        Log.i("longitude", "${location.longitude}")
                    }
                }

            } else {
                //setting open here
                Toast.makeText(this, "Turn on Location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)

            }
        } else {
            //request permisiion here
            requestPermission()
        }
    }

    private fun fetchCurrentLocation(latitude: String, longitude: String) {

        activityMainBinding.pbLoading.visibility = View.VISIBLE
        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude, longitude, API_KEY)
            ?.enqueue(object : Callback<ModelMainData> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<ModelMainData>,
                    response: Response<ModelMainData>
                ) {
                    if (response.isSuccessful) {
                        setDataInViews(response.body())
                    }
                }

                override fun onFailure(call: Call<ModelMainData>, t: Throwable) {
                    Toast.makeText(applicationContext, "ERROR", Toast.LENGTH_SHORT).show()
                }


            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setDataInViews(body: ModelMainData?) {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm")
        val currentDate = sdf.format(Date())
        activityMainBinding.tvDateAndTime.text = currentDate

        activityMainBinding.tvDayMaxTemp.text = "Day " + kelvinToCelsius(body!!.main.tempMax) + "°"
        activityMainBinding.tvDayMinTemp.text =
            "Night " + kelvinToCelsius(body!!.main.tempMin) + "°"
        activityMainBinding.tvTemp.text = "" + kelvinToCelsius(body!!.main.temp) + "°"
        activityMainBinding.tvFeelsLike.text = "" + kelvinToCelsius(body!!.main.feelsLike) + "°"
        activityMainBinding.tvWeatherType.text = body!!.weather[0].main
        activityMainBinding.tvSunrise.text = timeStampToLocalDate(body!!.sys.sunrise.toLong())
        activityMainBinding.tvSunset.text = timeStampToLocalDate(body!!.sys.sunset.toLong())
        activityMainBinding.tvPressure.text = body.main.pressure.toString()
        activityMainBinding.tvHumidity.text = body.main.humidity.toString() + " %"
        activityMainBinding.tvWindSpeed.text = body.wind.speed.toString() + " m/s"

        activityMainBinding.tvTempFarenhite.text = "" + kelvinToCelsius(body.main.temp)
        activityMainBinding.etGetCityName.setText(body.name)

        updateUi(body.weather[0].id)
    }

    private fun updateUi(id: Int) {
        if (id in 200..232) {
            //thunderstorm
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.ic_thunderstorm)
        } else if (id in 300..321) {
            //drizzle
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.ic_drizzle)
        } else if (id in 500..531) {
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.ic_rainy)

        } else if (id in 600..620) {
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.ic_snow)
        } else if (id in 700..781) {
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.ic_clouds)

        } else if (id == 800) {
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.ic_sun)

        } else {
            activityMainBinding.ivWeatherIcon.setImageResource(R.drawable.ic_clouds)
        }

        activityMainBinding.pbLoading.visibility = View.GONE
        activityMainBinding.rlMainLayout.visibility = View.VISIBLE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun timeStampToLocalDate(timeStamp: Long): String {
        val localeTime = timeStamp.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }
        return localeTime.toString()
    }

    private fun kelvinToCelsius(temp: Double): Double {
        var intTemp = temp
        intTemp = intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()

    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_REQUEST_ACESS_LOCATION
        )
    }

    companion object {
        private const val PERMISSION_REQUEST_ACESS_LOCATION = 100
        const val API_KEY = "1766167aadf3d55c406c47e6ac278efe"
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_ACESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_SHORT).show()
            getCurrentLocation()
        } else {
            Toast.makeText(applicationContext, "Denied", Toast.LENGTH_SHORT).show()

        }
    }
}