package com.kuzin.TelegamBotSpring;

import com.kuzin.TelegamBotSpring.entities.ShoppingList;
import com.kuzin.TelegamBotSpring.repositories.UserRepository;
import com.kuzin.TelegamBotSpring.services.ShoppingListService;
import com.kuzin.TelegamBotSpring.services.UserService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final UserService userService;
    private final ShoppingListService listService;

    private final UserRepository userRepository;

    private final Map<Long, Boolean> waitingForList = new HashMap<>();
    private final TelegramClient telegramClient;

    public UpdateConsumer(UserService userService, ShoppingListService listService,
                          UserRepository userRepository) {
        this.userService = userService;
        this.listService = listService;
        this.telegramClient = new OkHttpTelegramClient(
                Dotenv.load().get("BOT_TOKEN")
        );
        this.userRepository = userRepository;
    }

    @SneakyThrows
    public void handleStartCommand(Update update) {

        Long chatId = update.getMessage().getChatId();
        String firstName = update.getMessage().getChat().getFirstName();

        String welcomeText = """
        👋 Привет, %s! Добро пожаловать в наш бот!

        Для начала работы воспользуйтесь командой:
        /start - показать это сообщение

        Или используйте кнопки меню для навигации.
        """.formatted(firstName);

        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(welcomeText)
                .build();

        telegramClient.execute(message);

        sendReplyKeyboard(chatId);
    }



    @Override
    public void consume(Update update) {
        if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                Long chatId = update.getMessage().getChatId();
                if (messageText.equals("Меню")) {
                    sendMainMenu(chatId);
                } else if (messageText.equals("/start")) {
                    handleStartCommand(update);
                    return;
                } else if (messageText.equals("Картинка")) {
                    sendImage(chatId);
                } else if (messageText.contains("Список: ")) {
                    createShopList(update.getMessage());
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

    private void createShopList(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getChat().getId();
        String text = message.getText();

        text = text.substring(8);

        // Проверяем, ожидаем ли мы список от этого пользователя
        if (waitingForList.containsKey(chatId) && waitingForList.get(chatId)) {
            waitingForList.put(chatId, false);

            try {
                // Создаем и сохраняем список покупок
                ShoppingList shoppingList = new ShoppingList();
                shoppingList.setShoppingList(text);
                shoppingList.setId(chatId);
                com.kuzin.TelegamBotSpring.entities.User user = new com.kuzin.TelegamBotSpring.entities.User();
                user.setId(userId);
                user.setUsername(message.getChat().getUserName());
                shoppingList.setUser(user);

                listService.createList(shoppingList);

                sendMessage(chatId, "Список покупок успешно сохранен!");
            }  catch (Exception e) {
                sendMessage(chatId, "Ошибка при сохранении списка. Попробуйте еще раз.");
            }
        }
    }

    private void sendImage(Long chatId) {
        sendMessage(chatId, "Запустили загрузку картинки");
        new Thread(() -> {
            var imageUrl = "https://picsum.photos/200";
            try {
                URL url = new URL(imageUrl);
                var inputStream = url.openStream();

                SendPhoto sendPhoto = SendPhoto.builder()
                        .chatId(chatId)
                        .photo(new InputFile(inputStream, "random.jpg"))
                        .caption("Ваша случайная картинка:")
                        .build();

                telegramClient.execute(sendPhoto);

            } catch (TelegramApiException | IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @SneakyThrows
    private void sendReplyKeyboard(Long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text("Здравствуйте!")
                .build();

        List<KeyboardRow> keyboardRows = List.of(
                new KeyboardRow("Меню", "Картинка")
        );

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(keyboardRows);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);
        message.setReplyMarkup(markup);

        telegramClient.execute(message);
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        var data = callbackQuery.getData();
        var user = callbackQuery.getFrom();
        var chatId = callbackQuery.getFrom().getId();
        switch (data) {
            case "my_name" -> sendMyName(chatId, user);
            case "random" -> sendRandom(chatId);
            case "create_shopping_list" -> handleCreateShoppingList(chatId, user, callbackQuery);
            case "check_shopping_list" -> checkShoppingList(chatId, user);
            default -> sendMessage(chatId, "Неизвестная команда.");
        }
    }

    private void checkShoppingList(Long chatId, User tgUser) {
        var user = userService.getUserById(tgUser.getId());
        var userId = user.get().getId();
        ShoppingList shoppingList = listService.getListByUserId(userId).isPresent() ?
                listService.getListByUserId(userId).get() :
                null;
        if (shoppingList == null) {
            sendMessage(chatId, "Список покупок отсутствует.");
            return;
        }
        List<String> shoppingListText = !shoppingList.getShoppingList().isEmpty() ?
                Arrays.stream(shoppingList.getShoppingList().split(",")).toList() :
                Collections.emptyList();
        StringBuilder result = new StringBuilder("Ваш список покупок: \n");
        for (String s : shoppingListText) {
            result.append("!!! ").append(s).append("\n");
        }
        sendMessage(chatId, "Ваш список покупок: \n" + result);
    }

    private void handleCreateShoppingList(Long chatId, User tgUser, CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();

        // Проверяем, что callback связан с созданием списка покупок
        if ("create_shopping_list".equals(callbackData)) {
            waitingForList.put(chatId, true);

            sendMessage(chatId, "Напиши список покупок в формате: \"Список: продукт,продукт,продукт\".\nБот чувствителен к регистру!");
        }
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
        var text = "Привет!\n\nВас зовут: %s\nВаш ник: @%s".formatted(
                user.getFirstName() + " " + (user.getLastName() == null ? "" : user.getLastName()),
                user.getUserName()
        );
        sendMessage(chatId, text);
    }

    private void sendRandom(Long chatId) {
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
