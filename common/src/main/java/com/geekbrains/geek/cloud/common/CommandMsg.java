package com.geekbrains.geek.cloud.common;

import java.io.Serializable;

public class CommandMsg extends AbstractMsg implements Serializable {

    private String command;

    private String fileName;

    public CommandMsg(String command, String fileName) {
        super("command");
        this.command = command;
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return command;
    }
}
