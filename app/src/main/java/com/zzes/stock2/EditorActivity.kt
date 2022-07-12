package com.zzes.stock2

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.zzes.stock2.beans.SinaStockBean
import com.zzes.stock2.beans.WatchFaceData
import com.zzes.stock2.databinding.ActivityEditorBinding

class EditorActivity : ComponentActivity()  {
    companion object {
        var watchFaceData = WatchFaceData()
    }
    private lateinit var binding: ActivityEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        val refreshText: EditText = findViewById(R.id.refreshTimes)
        val stockCodeText: EditText = findViewById(R.id.stockCodes)
        refreshText.setText(watchFaceData.refreshSecond.toString())
        stockCodeText.setText(watchFaceData.stockCodes)
    }

    fun saveHandler(view: View) {
        val refreshText: EditText = findViewById(R.id.refreshTimes)
        val stockCodeText: EditText = findViewById(R.id.stockCodes)
        val refreshSecond = refreshText.editableText.toString().toInt()
        val stockCodes = stockCodeText.text.toString()

        watchFaceData.refreshSecond = refreshSecond
        watchFaceData.stockCodes = stockCodes.trim().lowercase()
        Toast.makeText(applicationContext, "已保存, 请返回等待数据刷新", Toast.LENGTH_SHORT)
            .show()

    }
}