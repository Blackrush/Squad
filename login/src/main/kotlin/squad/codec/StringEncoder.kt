package squad.codec

import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

import java.nio.CharBuffer
import java.nio.charset.Charset

class StringEncoder(private val charset: Charset, private val suffix: String = "\u0000") : MessageToMessageEncoder<CharSequence>() {
    override fun encode(ctx: ChannelHandlerContext, msg: CharSequence, out: MutableList<Any>) {
        if (msg.isEmpty()) {
            return
        }

        out.add(ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(StringBuilder().append(msg).append(suffix)), charset))
    }
}
