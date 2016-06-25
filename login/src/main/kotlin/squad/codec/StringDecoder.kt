package squad.codec

import io.netty.buffer.ByteBuf

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import java.nio.charset.Charset

class StringDecoder(private val charset: Charset, private val suffix: String = "\n\u0000") : MessageToMessageDecoder<ByteBuf>() {

    private val buffer: StringBuilder = StringBuilder()

    override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        buffer.append(msg.toString(charset))

        while (true) {
            val idx = buffer.indexOf(suffix)
            if (idx < 0) {
                break
            }

            out.add(buffer.substring(0, idx))
            buffer.replace(0, idx + suffix.length, "")
        }
    }
}
