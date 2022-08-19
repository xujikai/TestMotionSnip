package com.app.testmotionsnip

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.app.testmotionsnip.activity.GltfActivity
import com.app.testmotionsnip.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGltf.setOnClickListener {
            GltfActivity.start(this)
        }
    }
}