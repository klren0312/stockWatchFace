package com.zzes.stock2

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.zzes.stock2.databinding.ActivityEditorBinding

class EditorActivity : ComponentActivity() {

    private lateinit var binding: ActivityEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}