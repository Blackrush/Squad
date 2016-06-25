package squad.login

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.ServerChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import squad.codec.StringDecoder
import squad.codec.StringEncoder
import squad.support.logger
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

inline fun ServerBootstrap.pipelineBuilder(crossinline fn: ChannelPipeline.() -> Unit): ServerBootstrap {
    return childHandler(object : ChannelInitializer<Channel>() {
        override fun initChannel(ch: Channel?) {
            ch!!.pipeline().fn()
        }
    })
}

inline fun <reified T : ServerChannel> ServerBootstrap.channel(): ServerBootstrap {
    return channel(T::class.java)
}

inline fun Runtime.shutdown(crossinline fn: () -> Unit) {
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            fn()
        }
    })
}

object App {
    private val log = logger<App>()

    @JvmStatic fun main(args: Array<String>) {
        log.trace("a new world is born")

        val port = 5555

        val parentGroup = NioEventLoopGroup()
        val childGroup = NioEventLoopGroup()

        val channel = ServerBootstrap()
                .group(parentGroup, childGroup)
                .channel<NioServerSocketChannel>()
                .pipelineBuilder {
                    addLast(StringEncoder(Charset.forName("utf-8")))
                    addLast(StringDecoder(Charset.forName("utf-8")))
                    addLast(LoggingHandler(App::class.qualifiedName, LogLevel.DEBUG))
                    addLast(ServerHandler())
                }
                .bind(port)
                .syncUninterruptibly()
                .channel()

        log.info("listening on {}", port)

        Runtime.getRuntime().shutdown {
            log.info("shutdown requested")

            childGroup.shutdownGracefully()
            parentGroup.shutdownGracefully()
            channel.close().awaitUninterruptibly(12, TimeUnit.SECONDS)

            log.trace("shutdown completed")
        }
    }
}
