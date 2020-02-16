package com.geekbrains.geek.cloud.common;

import java.io.Serializable;

public abstract class AbstractMsg implements Serializable {
    String type;

    public abstract String toString();

    public String getType() {
        return this.type;
    }

    public AbstractMsg(String type) {
        this.type = type;
    }
}
