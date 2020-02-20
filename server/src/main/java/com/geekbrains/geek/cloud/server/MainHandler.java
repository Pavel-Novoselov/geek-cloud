package com.geekbrains.geek.cloud.server;

import com.geekbrains.geek.cloud.common.CommandMsg;
import com.geekbrains.geek.cloud.common.FileMsg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MainHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof CommandMsg) {
                CommandMsg commandMsg = (CommandMsg) msg;
                switch (commandMsg.getCommand()) {
                    case "download":
                        sendFile(commandMsg.getFileName(), ctx);
                        break;
                    case "list":
                        listFiles(ctx);
                        break;
                    default:
                        break;
                }
            } else if (msg instanceof FileMsg) {
                FileMsg fileMsg = (FileMsg) msg;
                Files.write(Paths.get("server/server_storage/" + fileMsg.getFilename()), fileMsg.getBytes(), StandardOpenOption.CREATE);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private void sendFile(String filename, ChannelHandlerContext ctx) throws IOException {
        if (Files.exists(Paths.get("server/server_storage/" + filename))) {
            FileMsg fileMsg = new FileMsg(Paths.get("server/server_storage/" + filename));
            ctx.writeAndFlush(fileMsg);
        }
    }

    private void listFiles(ChannelHandlerContext ctx) throws IOException {
        List<Path> response = Files.list(Paths.get("server/server_storage/")).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        Files.list(Paths.get("server/server_storage/")).map(p -> p.getFileName().toString()).map(p -> p + " ").collect(Collectors.toList()).forEach(sb::append);
        ctx.writeAndFlush(new CommandMsg(sb.toString(), null));
    }

}
