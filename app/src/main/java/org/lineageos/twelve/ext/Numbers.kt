package org.lineageos.twelve.ext

import kotlin.math.pow
import kotlin.math.roundToInt

fun Float.round(decimalPrecision: Int): Double {
    val factor = 10.0.pow(decimalPrecision)
    return (this * factor).roundToInt() / factor
}