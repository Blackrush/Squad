package squad.support

private val ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

object Random {
    fun randomString(len: Int): String {
        val random = java.util.Random(System.currentTimeMillis())
        val result = StringBuilder(len)

        for (i in 0..len - 1) {
            val idx = random.nextInt(ALPHABET.length)
            result.append(ALPHABET[idx])
        }

        return result.toString()
    }
}
