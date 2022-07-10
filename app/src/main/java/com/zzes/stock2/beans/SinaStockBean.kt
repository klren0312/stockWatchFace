package com.zzes.stock2.beans
//服务器返回的数据为：var hq_str_s_sh000001="上证指数,3094.668,-128.073,-3.97,436653,5458126";
//数据含义分别为：指数名称，当前点数，当前价格，涨跌率，成交量（手），成交额（万元）；
class SinaStockBean: ArrayList<SinaStockBeanItem>()

data class SinaStockBeanItem(
    val name: String,
    val current: String,
    val currentChange: String,
    val percent: String
)
