package squad.support

import org.jetbrains.spek.api.Spek

class RandomTest : Spek({
    describe("Random") {
        val randomString = Random.randomString(1000000)

        it("should generate lower letters") {
            assert(randomString.any(Char::isLowerCase))
        }
        it("should generate upper letters") {
            assert(randomString.any(Char::isUpperCase))
        }
        it("should generate digits") {
            assert(randomString.any(Char::isDigit))
        }
    }
})