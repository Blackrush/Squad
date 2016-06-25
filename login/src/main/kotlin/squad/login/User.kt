package squad.login

data class User(
        val id: Long,
        val login: String,
        val password: String,
        val nickname: String,
        val communityId: Int,
        val secretQuestion: String,
        val secretAnswer: String)
