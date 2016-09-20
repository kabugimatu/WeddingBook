/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bots;

import botconfig.BotConfig;
import entities.Chat;
import entities.Wedding;
import facades.ChatFacade;
import facades.WeddingFacade;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardHide;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import support.Emoji;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;

/**
 *
 * @author zkmatu
 */
public class ServiceBot extends TelegramLongPollingBot {

    InputStream input = null;
    Properties prop = new Properties();
    private static final int STARTSTATE = 0;
    private static final int MAINMENU = 1;
    private static final int VIEWWEDDING = 2;
    private static final int COUPLENAMEREG = 3;
    private static final int VENUEREG = 4;
    private static final int RECEPTIONREG = 5;
    private ChatFacade chatFacade;
    private WeddingFacade weddingFacade;

    private Wedding newWedding, dbWedding;
    Chat dbChat;

    public ServiceBot() {
        input = getClass().getResourceAsStream("/properties/properties.properties");

        try {
            // load  properties file
            this.chatFacade = (ChatFacade) new InitialContext().lookup("java:global/WeddingBook/ChatFacade");
            this.weddingFacade = (WeddingFacade) new InitialContext().lookup("java:global/WeddingBook/WeddingFacade");
            prop.load(input);
        } catch (Exception ex) {
            Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String getBotToken() {
        return BotConfig.BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            //check if the message has text. it could also contain for example a location ( message.hasLocation() )
            try {
                if (message.hasText()) {
                    //create an object that contains the information to send back the message
                    System.out.println("Command -- >" + message.getText());
                    SendMessage sendMessageRequest = new SendMessage();
                    sendMessageRequest.setChatId(message.getChatId().toString()); //who should get from the message the sender that sent it.
                    handleIncomingMessage(message);
                }
            } catch (Exception e) {
                //do some error handling
            }
        }
    }

    @Override
    public String getBotUsername() {
        return BotConfig.BOT_USERNAME;
    }

    private void handleIncomingMessage(Message message) {
        try {
            SendMessage sendMessageRequest = new SendMessage();
            if (message.getText().equalsIgnoreCase(prop.getProperty("commandstart"))) {
                System.out.println("Showing menu..");

                sendMessage(showMainMenu(message));
            } else if (message.getText().equalsIgnoreCase(prop.getProperty("commandstop"))) {
                hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
            } else if (message.getText().equalsIgnoreCase(prop.getProperty("gettingMarried"))) {
                ongettingMarriedRequest(message);

                //        sendMessage(hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId()));
            } else {

                int state = chatFacade.fetchChatByIdAndUserId(message.getChatId(), message.getFrom().getId()).getChatState();

                switch (state) {
                    case MAINMENU:
                        sendMessageRequest = showMainMenu(message);
                        break;

                    case COUPLENAMEREG:
                        sendMessageRequest = oncoupleNameRegRequest(message);
                        break;
                    case VENUEREG:
                        sendMessageRequest = onVenueRegRequest(message);
                        break;
                    case RECEPTIONREG:
                        break;
                    default:
                        sendMessageRequest = messageOnViewWedding(message);
                        break;
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private SendMessage ongettingMarriedRequest(Message message) {
        SendMessage sendMessageRequest = new SendMessage();
        try {
            sendMessageRequest.setChatId(message.getChatId() + "");
            sendMessageRequest.setText(prop.getProperty("congratsmessg") + Emoji.SMILING_FACE_WITH_SMILING_EYES);
            sendMessage(sendMessageRequest);

            Chat chatStatus = chatFacade.fetchChatByIdAndUserId(message.getChatId(), message.getFrom().getId());
            if (chatStatus != null) {
                chatStatus.setChatState(COUPLENAMEREG);
                chatFacade.edit(chatStatus);
            } else {
                chatFacade.create(new Chat(message.getChatId(), message.getFrom().getId(), COUPLENAMEREG));
            }
            Thread.sleep(500);
            sendMessageRequest.setText(prop.getProperty("couplerequest"));
            sendMessage(sendMessageRequest);

        } catch (Exception ex) {
            Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sendMessageRequest;

    }

    private SendMessage oncoupleNameRegRequest(Message message) {
        SendMessage sendMessage = new SendMessage();
        try {
            //  int chatState = 0;
            dbChat = chatFacade.fetchChatByIdAndUserId(message.getChatId(), message.getFrom().getId());
            if (dbChat != null && dbChat.getChatState() == COUPLENAMEREG) {
                dbWedding = weddingFacade.fetchWeddingByChatId(message.getChatId() + "");
                if (dbWedding == null) {
                    dbChat.setChatState(VENUEREG);

                    newWedding = new Wedding();
                    newWedding.setCoupleName(message.getText());
                    newWedding.setWeddingCode(new Random().nextInt(10000) + "");
                    newWedding.setClientCode(new Random().nextInt(10000) + "");
                    newWedding.setCurrentChatId(message.getChatId() + "");
                    weddingFacade.create(newWedding);
                    sendMessage.setText(prop.getProperty("congratsmsg") + newWedding.getCoupleName() + " " + Emoji.THUMBS_UP_SIGN + " ");
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                    Thread.sleep(1000);
                    sendMessage.setText(" Your admin wedding code is >> " + newWedding.getWeddingCode());
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                    sendMessage.setText(" Your public wedding code is >> " + newWedding.getClientCode() +". "+prop.getProperty("publiccodemessg"));
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                     Thread.sleep(1000);
                    //Request Venue
                   
                    sendMessage.setText(prop.getProperty("venuemsg"));
                    sendMessage(sendMessage);
                } else {
                    dbChat.setChatState(VENUEREG);
                    sendMessage.setText(prop.getProperty("congratsmsg") + dbWedding.getCoupleName() + " " + Emoji.THUMBS_UP_SIGN + " ");
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                    Thread.sleep(1000);
                    sendMessage.setText(" Your admin wedding code is >> " + dbWedding.getWeddingCode());
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                    sendMessage.setText(" Your public wedding code is >> " + dbWedding.getClientCode());
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                    //Request Venue
                    sendMessage.setText(prop.getProperty("venuemsg"));
                    sendMessage(sendMessage);
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex);
        }

        return sendMessage;
    }

    private SendMessage onVenueRegRequest(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(prop.getProperty("receptionmsg"));
        dbChat = chatFacade.fetchChatByIdAndUserId(message.getChatId(), message.getFrom().getId());
        if (dbChat != null && dbChat.getChatState() == VENUEREG) {
            dbChat.setChatState(RECEPTIONREG);
            chatFacade.edit(dbChat);
        }
        return sendMessage;
    }

    private SendMessage messageOnViewWedding(Message message) {
        return new SendMessage();
    }

    private void hideKeyboard(Integer userId, Long chatId, Integer messageId) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setText(Emoji.WAVING_HAND_SIGN.toString());

        ReplyKeyboardHide replyKeyboardHide = new ReplyKeyboardHide();
        replyKeyboardHide.setSelective(true);
        sendMessage.setReplyMarkup(replyKeyboardHide);

        sendMessage(sendMessage);
        //  DatabaseManager.getInstance().insertWeatherState(userId, chatId, STARTSTATE);
    }

    private ReplyKeyboardMarkup getMainMenuKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(prop.getProperty("gettingMarried"));

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(prop.getProperty("viewwedding"));
//        keyboardSecondRow.add(getRateCommand(language));
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private SendMessage showMainMenu(Message message) throws TelegramApiException {
        SendMessage sendMessageRequest;
        sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                getMainMenuKeyboard());
        chatFacade.create(new Chat(message.getChatId(), message.getFrom().getId(), MAINMENU));

        return sendMessageRequest;

    }

    private static SendMessage sendChooseOptionMessage(Long chatId, Integer messageId,
            ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText("How can i help you ? ");

        return sendMessage;
    }

}
