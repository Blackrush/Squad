package squad.support

import java.util.*

fun String.slugify(): String {
    throw UnsupportedOperationException("todo")
}

fun String.splat(delim: String, limit: Int): List<String> {
    val res = ArrayList<String>(limit)
    var start: Int = 0
    for (i in 0..limit) {
        val stop = this.indexOf(delim, start)
        if (stop < 0) {
            break
        }
        res += this.substring(start, stop)
        start = stop + delim.length
    }
    if (start > 0) {
        res += this.substring(start)
    }
    while (res.size != limit) {
        res += ""
    }
    return res
}