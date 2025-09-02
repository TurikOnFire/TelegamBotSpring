package com.kuzin.TelegamBotSpring;

import io.github.cdimascio.dotenv.Dotenv;
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
        return Dotenv.load().get("BOT_TOKEN");
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updateConsumer;
    }
}
