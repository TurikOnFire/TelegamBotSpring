package com.kuzin.TelegamBotSpring;

import com.kuzin.TelegamBotSpring.services.UserService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final UserService userService;

    private final CharacterEncodingFilter characterEncodingFilter;

    private final TelegramClient telegramClient;

    public UpdateConsumer(UserService userService,
                          CharacterEncodingFilter characterEncodingFilter) {
        this.userService = userService;
        this.telegramClient = new OkHttpTelegramClient(
                Dotenv.load().get("BOT_TOKEN")
        );
        this.characterEncodingFilter = characterEncodingFilter;
    }


    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                Long chatId = update.getMessage().getChatId();
                if (messageText.equals("/start")) {
                    sendMainMenu(chatId);
                } else {
                    sendMessage(chatId, "Я тебя не понимаю.");
                }
            } else if (update.getMessage().hasVoice() || update.getMessage().hasVideoNote()) {
                Long chatId = update.getMessage().getChatId();
                sendMessage(chatId, "У тебя очень красивый голос, но я не понимаю.");
                return;
            } else {
                Long chatId = update.getMessage().getChatId();
                sendMessage(chatId, "Скорее всего ты отправил мне медиа-файл или стикер." +
                        "Я тебя не понимаю.");
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        }

    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        var data = callbackQuery.getData();
        var user = callbackQuery.getFrom();
        var chatId = callbackQuery.getFrom().getId();
        switch (data) {
            case "my_name" -> sendMyName(chatId, user);
            case "random" -> sendRandom(chatId);
            case "create_shopping_list" -> createShoppingList(chatId, user);
            case "check_shopping_list" -> checkShoppingList(chatId, user);
            default -> sendMessage(chatId, "Неизвестная команда.");
        }
    }

    private void checkShoppingList(Long chatId, User tgUser) {
        System.out.println("checkShoppingList");
        ShoppingList shoppingList = new ShoppingList();
        var user = userService.getUserById(tgUser.getId());
        System.out.println(user);
        user.ifPresent(value -> sendMessage(chatId, "Ur user: " + value.getUsername()));
    }

    private void createShoppingList(Long chatId, User tgUser) {
        System.out.println("createShoppingList");
        ShoppingList shoppingList = new ShoppingList();
        com.kuzin.TelegamBotSpring.entities.User user = new com.kuzin.TelegamBotSpring.entities.User();
        user.setId(tgUser.getId());
        user.setUsername(tgUser.getUserName());
        user.setFirstName(tgUser.getFirstName());
        if (userService.getUserById(tgUser.getId()).isEmpty()) {
            userService.createUser(user);
        } else {
            sendMessage(chatId, "User already exists");
            return;
        }
        sendMessage(chatId, "User created");
    }


    @SneakyThrows
    private void sendMessage(Long chatId, String messageText
    ) {
        SendMessage message = SendMessage.builder()
                .text(messageText)
                .chatId(chatId)
                .build();

        telegramClient.execute(message);
    }

    private void sendMyName(Long chatId, User user) {
        System.out.println("sendMyName");
        var text = "Привет!\n\nВас зовут: %s\nВаш ник: @%s".formatted(
                user.getFirstName() + " " + (user.getLastName() == null ? "" : user.getLastName()),
                user.getUserName()
        );
        sendMessage(chatId, text);
    }

    private void sendRandom(Long chatId) {
        System.out.println("sendRandom");
        var randomInt = Math.abs(ThreadLocalRandom.current().nextInt());
        sendMessage(chatId, "Ваше рандомное число: " + randomInt);
    }

    @SneakyThrows
    private void sendMainMenu(Long chatId) {
        SendMessage message = SendMessage.builder()
                .text("Привет, выбери пункт меню: ")
                .chatId(chatId)
                .build();

        var button1 = InlineKeyboardButton.builder()
                .text("Как меня зовут?")
                .callbackData("my_name")
                .build();

        var button2 = InlineKeyboardButton.builder()
                .text("Случайное число")
                .callbackData("random")
                .build();

        var button3 = InlineKeyboardButton.builder()
                .text("Создать список покупок")
                .callbackData("create_shopping_list")
                .build();

        var button4 = InlineKeyboardButton.builder()
                .text("Проверить список покупок")
                .callbackData("check_shopping_list")
                .build();

        List<InlineKeyboardRow> keyboardRows = List.of(
                new InlineKeyboardRow(button1),
                new InlineKeyboardRow(button2),
                new InlineKeyboardRow(button3),
                new InlineKeyboardRow(button4)
        );



        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(keyboardRows);

        message.setReplyMarkup(markup);

        telegramClient.execute(message);
    }
}
