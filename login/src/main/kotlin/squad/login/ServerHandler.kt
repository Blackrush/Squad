package squad.login

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.AttributeKey
import squad.support.Random
import squad.support.logger
import squad.support.splat


private val log = logger<ServerHandler>()


enum class State { HANDSHAKE, AUTHENTICATION, REALM }

val TICKET_ATTR = AttributeKey.valueOf<String>("squad.login.TICKET_ATTR")
val STATE_ATTR = AttributeKey.valueOf<State>("squad.login.STATE_ATTR")

class ServerHandler : ChannelInboundHandlerAdapter() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        val ticket = Random.randomString(32)
        ctx.channel().attr(TICKET_ATTR).set(ticket)
        ctx.channel().attr(STATE_ATTR).set(State.HANDSHAKE)

        ctx.writeAndFlush("HC$ticket")
    }

    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    override fun channelRead(ctx: ChannelHandlerContext, o: Any) {
        val message = o as String
        val stateAttr = ctx.channel().attr(STATE_ATTR)

        val newState = when (stateAttr.get()) {
            State.HANDSHAKE -> handleHandshake(ctx, message)
            State.AUTHENTICATION -> handleAuthentication(ctx, message)
            State.REALM -> handleRealm(ctx, message)
        }

        when (newState) {
            null -> ctx.close()
            else -> stateAttr.set(newState)
        }
    }

    private fun handleHandshake(ctx: ChannelHandlerContext, message: String): State? {
        if (message != "1.29.1") {
            ctx.writeAndFlush("AlEv1.29.1")
            return null
        }

        return State.AUTHENTICATION
    }

    private fun handleAuthentication(ctx: ChannelHandlerContext, message: String): State? {
        val (login, password) = message.splat("\n", limit = 2)

        log.debug("login=$login,password=$password")
        ctx.writeAndFlush("AlEf")
        return null
    }

    private fun handleRealm(ctx: ChannelHandlerContext, message: String): State? {
        return State.REALM
    }
}