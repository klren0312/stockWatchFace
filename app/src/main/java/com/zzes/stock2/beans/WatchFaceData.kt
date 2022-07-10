package com.zzes.stock2.beans

private const val REFRESH_SECOND_DEFAULT = 10
private const val STOCK_CODE_DEFAULT = "sh000001,sz399001,sz399006,sh000688"

data class WatchFaceData(
    val refreshSecond: Number = REFRESH_SECOND_DEFAULT,
    val stockCodes: String = STOCK_CODE_DEFAULT
)
