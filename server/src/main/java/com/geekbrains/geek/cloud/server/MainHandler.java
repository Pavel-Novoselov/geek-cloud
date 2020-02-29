package com.geekbrains.geek.cloud.server;

import org.apache.log4j.Logger;

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
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private static final Logger admin = Logger.getLogger("admin");
    String user;
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof CommandMsg) {
                CommandMsg commandMsg = (CommandMsg) msg;
                switch (commandMsg.getCommand()) {
                    case "auth":
                        user = auth(commandMsg.getFileName(), ctx);
                        listFiles(ctx, user);
                        break;
                    case "download":
                        sendFile(commandMsg.getFileName(), ctx, user);
                        break;
                    case "list":
                        listFiles(ctx, user);
                        break;
                    case "reg":
                        registration(commandMsg.getFileName(), ctx);
                    case "delete":
                        delete(commandMsg.getFileName(), ctx, user);
                    default:
                        break;
                }
            } else if (msg instanceof FileMsg) {
                FileMsg fileMsg = (FileMsg) msg;
                Files.write(Paths.get("server/server_storage/"+user+"/" + fileMsg.getFilename()), fileMsg.getBytes(), StandardOpenOption.CREATE);
                admin.info("User "+user+" upload a new file "+fileMsg.getFilename());
                listFiles(ctx, user);
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

    private void sendFile(String filename, ChannelHandlerContext ctx, String user) throws IOException {
        if (Files.exists(Paths.get("server/server_storage/"+user+"/" + filename))) {
            FileMsg fileMsg = new FileMsg(Paths.get("server/server_storage/"+user+"/" + filename));
            ctx.writeAndFlush(fileMsg);
        }
    }

    private void listFiles(ChannelHandlerContext ctx, String user) throws IOException {
        if (user!=null){
            StringBuilder sb = new StringBuilder();
            Files.list(Paths.get("server/server_storage/"+user+"/")).map(p -> p.getFileName().toString()+", ").collect(Collectors.toList()).forEach(sb::append);
            ctx.writeAndFlush(new CommandMsg("List: "+sb.toString(), null));
        }
    }

    private void delete(String fileNAme, ChannelHandlerContext ctx, String user) throws IOException {
        Path path = Paths.get("server/server_storage/"+user+"/"+fileNAme);
        try {
            Files.deleteIfExists(path);
            admin.info("Deleted file "+path.toString());
        } catch (IOException e) {
            e.printStackTrace();
            admin.warn("Попытка удалить несуществующий файл "+fileNAme);
        }
        listFiles(ctx, user);
    }

    private String auth(String authFields, ChannelHandlerContext ctx) throws SQLException {
        String[] tokens = authFields.split(" ");;
        String currentNick;
        if (tokens.length>0){
            currentNick = AuthService.getNickByLoginAndPass(tokens[0], tokens[1]);
        } else
            currentNick=null;
        if (currentNick == null) {
            ctx.writeAndFlush(new CommandMsg("Неверный логин/пароль", null));
            admin.warn("неудачная попытка залогиниться - неверный логин/пароль");
        } else {
            admin.info("Клиент " + currentNick + " успешно залогинился");
            ctx.writeAndFlush(new CommandMsg("AuthOK"));
        }
        return currentNick;
    }
    private void registration(String regFields, ChannelHandlerContext ctx) throws SQLException {
        String[] regTokens = regFields.split(" ");
        if (regTokens.length>0){
            AuthService.addNewUser(regTokens[0], regTokens[1], regTokens[2]);
            admin.info("Зарегистрирован новый пользователь: nick="+regTokens[0] +", login=" + regTokens[1]+ ", password(hash)="+ regTokens[2].hashCode());
            Path path= Paths.get("server/server_storage/"+regTokens[0]+"/");
            try {
                Files.createDirectories(path);
                admin.info("Создана папка "+path.toString());
            } catch (IOException e) {
                e.printStackTrace();
                admin.warn("Невозможно создать папку /" + regTokens[0]);
            }
            ctx.writeAndFlush(new CommandMsg("RegOK"));
        } else {
            ctx.writeAndFlush(new CommandMsg("Неверный nickname/логин/пароль", null));
            admin.warn("неудачная создать пользователя");
        }
    }
}
