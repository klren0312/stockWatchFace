package com.zzes.stock2.beans

private const val REFRESH_SECOND_DEFAULT = 10
private const val STOCK_CODE_DEFAULT = "sh000001,sz399001,sz399006,sh000688,sz002230,sz000630,int_dji,int_nasdaq,int_sp500,gb_bili"

data class WatchFaceData(
    var refreshSecond: Int = REFRESH_SECOND_DEFAULT,
    var stockCodes: String = STOCK_CODE_DEFAULT
)
