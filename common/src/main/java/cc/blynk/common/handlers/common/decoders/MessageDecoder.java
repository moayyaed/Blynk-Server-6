package cc.blynk.common.handlers.common.decoders;

import cc.blynk.common.enums.Command;
import cc.blynk.common.handlers.DefaultExceptionHandler;
import cc.blynk.common.model.messages.MessageBase;
import cc.blynk.common.stats.GlobalStats;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static cc.blynk.common.model.messages.MessageFactory.*;

/**
 * Decodes input byte array into java message.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 */
public class MessageDecoder extends ByteToMessageDecoder implements DefaultExceptionHandler {

    protected static final Logger log = LogManager.getLogger(MessageDecoder.class);

    private final GlobalStats stats;

    public MessageDecoder(GlobalStats stats) {
        this.stats = stats;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 5) {
            return;
        }

        in.markReaderIndex();

        final short command = in.readUnsignedByte();
        final int messageId = in.readUnsignedShort();
        final int codeOrLength = in.readUnsignedShort();

        MessageBase message;
        if (command == Command.RESPONSE) {
            message = produce(messageId, codeOrLength);
        } else {
            if (in.readableBytes() < codeOrLength) {
                in.resetReaderIndex();
                return;
            }

            message = produce(messageId, command, in.readSlice(codeOrLength).toString(CharsetUtil.UTF_8));
        }

        log.trace("Incoming {}", message);

        stats.mark(command);

        out.add(message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handleGeneralException(ctx, cause);
    }
}
