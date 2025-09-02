package com.kuzin.TelegamBotSpring;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;

@Component
public class Bot implements SpringLongPollingBot {

    private final UpdateConsumer updateConsumer;

    public Bot(UpdateConsumer updateConsumer) {
        this.updateConsumer = updateConsumer;
    }

    @Override
    public String getBotToken() {
        return "8158865423:AAGT8K5p6hi9lBiZ5u9VPD9VwDGdMKQ-5mo";
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updateConsumer;
    }
}
