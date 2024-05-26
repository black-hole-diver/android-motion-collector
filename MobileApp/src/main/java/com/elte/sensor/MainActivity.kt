package com.elte.sensor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.elte.sensor.databinding.ActivityMainBinding

/**
 * The main activity of the app.
 * @author Wittawin Panta
 * @version 1.0 2024-04-13
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /**
     * Called when the activity is starting.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
    }

}