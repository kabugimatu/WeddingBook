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
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.io.OutputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.PhotoSize;

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
    private static final int DATEREG = 4;
    private static final int VENUEREG = 5;
    private static final int RECEPTIONREG = 6;
    private static final int PHONEREG = 7;
    private static final int COMPLETEREG = 8;

    private static final int OWNWEDDINGREQ = 9;
    private static final int OTHERWEDDINGREQ = 10;
    private static final int VIEWINGOWNWEDDINGREQ = 11;
    private static final int VIEWINGOWNWEDPHOTO = 12;
    private static final int SENDINGOWNWEDPHOTO = 13;

    private boolean fromBack;
    private String filePathUrl = "https://api.telegram.org/file/bot" + BotConfig.BOT_TOKEN + "/";
    private ChatFacade chatFacade;
    private WeddingFacade weddingFacade;
    private Properties sysProps;

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
            SendMessage sendMessageRequest = new SendMessage();
            //check if the message has text. it could also contain for example a location ( message.hasLocation() )
            try {
                //   if(message.)

                if (message.hasText()) {
                    //create an object that contains the information to send back the message
                    System.out.println("Command -- >" + message.getText());
                    if (message.getText().startsWith("?")) {
                        sendMessageRequest.setChatId(message.getChatId().toString());
                        sendMessageRequest.setText(Emoji.THUMBS_UP_SIGN + "");
                        sendMessage(sendMessageRequest);
                    } else {
                        //      sendMessageRequest.setChatId(message.getChatId().toString()); //who should get from the message the sender that sent it.
                        handleIncomingMessage(message);
                    }
                } else {
                    dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
                    if (dbChat.getChatState() == SENDINGOWNWEDPHOTO) {
                        onReceiveWeddingPhoto(message);
                    }
                    //  handleIncomingMessage(message);
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
            // SendMessage sendMessageRequest = new SendMessage();
            dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            if (dbChat == null) {
                chatFacade.create(new Chat(message.getChatId(), message.getFrom().getId(), MAINMENU));
                dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            }

            if (message.getText().equalsIgnoreCase(prop.getProperty("commandstart"))) {
                System.out.println("Showing menu..");

                sendMessage(showMainMenu(message));
            } else if (message.getText().equalsIgnoreCase(prop.getProperty("commandstop"))) {
                hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
            } else if (message.getText().equalsIgnoreCase(prop.getProperty("gettingMarried"))) {
                ongettingMarriedRequest(message);

            } else if (message.getText().equalsIgnoreCase(prop.getProperty("viewwedding"))) {
                Thread.sleep(500);
                sendMessage(showViewWeddingMenu(message));

            } else if (message.getText().equalsIgnoreCase(prop.getProperty("viewmywedding"))) {
                Thread.sleep(500);
                messageOnViewMyWeddingRequest(message);

            } else if (message.getText().equalsIgnoreCase(prop.getProperty("viewotherswedding"))) {
                Thread.sleep(500);
                //  messageOnViewMyWeddingRequest(message);

            } else if (message.getText().equalsIgnoreCase(prop.getProperty("backtoviewweddingmenu")) && dbChat.getChatState() == VIEWINGOWNWEDDINGREQ) {

                sendMessage(showViewWeddingMenu(message));

            } else if (message.getText().equalsIgnoreCase(prop.getProperty("couplephoto"))) {

                onViewWeddingPhoto(message);

            } else {

                int state = 0;
                if (dbChat != null) {
                    state = dbChat.getChatState();
                }

                switch (state) {
                    case MAINMENU:
                        showMainMenu(message);
                        break;

                    case COUPLENAMEREG:
                        oncoupleNameRegRequest(message);
                        break;
                    case DATEREG:
                        onweddingDateRegRequest(message);
                        break;
                    case VENUEREG:
                        onVenueRegRequest(message);
                        break;
                    case RECEPTIONREG:
                        onReceptionRegRequest(message);
                        break;
                    case PHONEREG:
                        onPhoneNumberRegRequest(message);
                        break;
                    case COMPLETEREG:
                        hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
                        break;
                    case OWNWEDDINGREQ:
                        processWeddingAdminCode(message);
                        break;
                    case SENDINGOWNWEDPHOTO:
                        onReceiveWeddingPhoto(message);
                        break;
                    default:
                        //messageOnViewWedding(message);
                        break;
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private java.io.File downloadFile(String fileUrl) {
        java.io.File file = null;
        try {

            sysProps = System.getProperties();
            URL url = new URL(fileUrl);
            InputStream in = url.openStream();
            String directoryPath = sysProps.getProperty("file.separator") + sysProps.getProperty("user.home") + sysProps.getProperty("file.separator") + "Documents" + sysProps.getProperty("file.separator") + "dev";
            java.io.File directory = new java.io.File(directoryPath);

            String pathToFile = directoryPath + sysProps.getProperty("file.separator") + new Random().nextInt(100) + fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

            if (!directory.exists()) {
                directory.mkdirs();
            }
            file = new java.io.File(pathToFile);
            file.createNewFile();

            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            
            byte[] bytes =  new byte[10000];
            while ((read = in.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);

            }
            outputStream.flush();
            outputStream.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return file;
    }

    private void onReceiveWeddingPhoto(Message message) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId() + "");
        String downloadFilePath = "";
        try {

            if (message.getPhoto() != null) {
                System.out.println("Receiving photo ");
                dbWedding = weddingFacade.fetchWeddingByCreator(message.getFrom().getId() + "");
                List<PhotoSize> photos = message.getPhoto();
                System.out.println("Photos --> " + photos.size());
                for (int i = 0; i < photos.size(); i++) {

                    GetFile getFileRequest = new GetFile();
                    getFileRequest.setFileId(photos.get(i).getFileId());
                    File file = getFile(getFileRequest);
                    //  System.out.println(file.getFilePath());
                    downloadFilePath = filePathUrl + file.getFilePath();
                    System.out.println("Photo --> " + downloadFilePath);
                    java.io.File fileFromSystem = downloadFile(downloadFilePath);

                    byte[] bytes = new byte[(int) fileFromSystem.length()];

                    System.out.println("Wedding photo Size --> " + bytes.length);
                    FileInputStream fileInputStream = new FileInputStream(fileFromSystem);
                    fileInputStream.read(bytes);
                    dbWedding.setCouplePhoto(bytes);
                    weddingFacade.edit(dbWedding);
                    dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
                    dbChat.setChatState(VIEWINGOWNWEDDINGREQ);
                    chatFacade.edit(dbChat);
                    sendMessageRequest.setText(prop.getProperty("photoreceived"));
                    sendMessage(sendMessageRequest);

                    sendMessageRequest = showOwnWeddingMenu(message);
                    sendMessageRequest.setChatId(message.getChatId() + "");
                    sendMessage(sendMessageRequest);
                    // fileFromSystem.delete();

                    break;
                }

            } else {
                hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
                sendMessageRequest.setText(prop.getProperty("photorequest2"));
                sendMessage(sendMessageRequest);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void onViewWeddingPhoto(Message message) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId() + "");
        sysProps = System.getProperties();
        try {
            //System.out.println("Wedding photo request--> " );
            dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            dbWedding = weddingFacade.fetchWeddingByCreator(message.getFrom().getId() + "");
            if (dbChat != null && dbChat.getChatState() == VIEWINGOWNWEDDINGREQ) {

                if (dbWedding.getCouplePhoto() == null) {
                    hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
                    sendMessageRequest.setText(prop.getProperty("photorequest"));
                    sendMessage(sendMessageRequest);
                    dbChat.setChatState(SENDINGOWNWEDPHOTO);
                    chatFacade.edit(dbChat);
                } else {

                    String strFilePath = sysProps.getProperty("user.home") + sysProps.getProperty("file.separator") + "Documents" + sysProps.getProperty("file.separator") + "dev" + sysProps.getProperty("file.separator") + new Random().nextInt(100) + ".jpeg";
                    FileOutputStream fos = new FileOutputStream(strFilePath);
                    fos.write(dbWedding.getCouplePhoto());
                    fos.close();

                    SendPhoto sendPhotoRequest = new SendPhoto();
                    sendPhotoRequest.setChatId(message.getChatId().toString());
                    java.io.File fileToSend = new java.io.File(strFilePath);
                    sendPhotoRequest.setNewPhoto(fileToSend);

                    //    System.out.println("Sending phtoto -->   " + strFilePath );
                    sendPhoto(sendPhotoRequest);
                    fileToSend.delete();

                }

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void processWeddingAdminCode(Message message) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId() + "");
        try {
            dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            if (dbChat != null && dbChat.getChatState() == OWNWEDDINGREQ) {
                dbWedding = weddingFacade.fetchByAdminCode(message.getText());
                if (dbWedding != null) {
                    sendMessageRequest.setText(prop.getProperty("ownweddingwelcoome") + " " + dbWedding.getCoupleName() + Emoji.WAVING_HAND_SIGN + " ? ");
                    sendMessage(sendMessageRequest);

                    sendMessageRequest = showOwnWeddingMenu(message);
                    sendMessageRequest.setChatId(message.getChatId() + "");
                    Thread.sleep(500);
                    sendMessage(sendMessageRequest);
                    dbChat.setChatState(VIEWINGOWNWEDDINGREQ);
                    chatFacade.edit(dbChat);

                } else {
                    sendMessageRequest.setText(prop.getProperty("invalidadmincode") + " " + Emoji.ASTONISHED_FACE);
                    sendMessage(sendMessageRequest);
                }
            } else {
                showMainMenu(message);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void messageOnViewMyWeddingRequest(Message message) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId() + "");
        // System.out.println("Requeting code");
        try {
            dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            if (dbChat != null && dbChat.getChatState() == VIEWWEDDING) {
                dbChat.setChatState(OWNWEDDINGREQ);
                chatFacade.edit(dbChat);
                hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
                // Thread.sleep(1000);
                sendMessageRequest.setText(prop.getProperty("weddingadmincodereq"));
                sendMessage(sendMessageRequest);

            } else {
                showMainMenu(message);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private SendMessage ongettingMarriedRequest(Message message) {
        SendMessage sendMessageRequest = new SendMessage();
        try {

            Chat chatStatus = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());

            if (chatStatus != null) {
                dbWedding = weddingFacade.fetchWeddingByCreator(message.getFrom().getId() + "");
                if (dbWedding != null) {
                    sendMessageRequest.setChatId(message.getChatId() + "");
                    sendMessageRequest.setText(Emoji.SMILING_FACE_WITH_SMILING_EYES + " " + prop.getProperty("detectregmsg") + " '" + prop.getProperty("viewwedding") + "' instead.");
                    sendMessage(sendMessageRequest);
                } else {
                    Thread.sleep(500);
                    chatStatus.setChatState(COUPLENAMEREG);
                    chatFacade.edit(chatStatus);
                    sendMessageRequest.setChatId(message.getChatId() + "");
                    sendMessageRequest.setText(prop.getProperty("congratsmessg") + Emoji.SMILING_FACE_WITH_SMILING_EYES);
                    sendMessage(sendMessageRequest);
                    sendMessageRequest.setChatId(message.getChatId() + "");
                    sendMessageRequest.setText(prop.getProperty("couplerequest"));
                    sendMessage(sendMessageRequest);
                    hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());

                }
            } else {

                chatFacade.create(new Chat(message.getChatId(), message.getFrom().getId(), COUPLENAMEREG));
                sendMessageRequest.setChatId(message.getChatId() + "");
                sendMessageRequest.setText(prop.getProperty("congratsmessg") + Emoji.SMILING_FACE_WITH_SMILING_EYES);
                sendMessage(sendMessageRequest);
                sendMessageRequest.setChatId(message.getChatId() + "");
                sendMessageRequest.setText(prop.getProperty("couplerequest"));
                sendMessage(sendMessageRequest);
            }
            // Thread.sleep(500);

        } catch (Exception ex) {
            Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sendMessageRequest;

    }

    private void onweddingDateRegRequest(Message message) {
        SendMessage sendMessage = new SendMessage();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY");

        try {
            Date weddingDate = dateFormat.parse(message.getText());
            //  System.out.println("Date --> "+ weddingDate);
            dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            if (dbChat != null && dbChat.getChatState() == DATEREG) {
                dbWedding = weddingFacade.fetchWeddingByChatId(dbChat.getId());
                if (dbWedding != null) {
                    dbChat.setChatState(VENUEREG);
                    dbWedding.setWeddingDate(message.getText());
                    dbWedding.setCurrentChatId(dbChat.getId());
                    chatFacade.edit(dbChat);
                    weddingFacade.edit(dbWedding);
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage.setText(prop.getProperty("venuemsg"));
                    sendMessage(sendMessage);
                }
            }
        } catch (ParseException ex) {
            sendMessage.setChatId(message.getChatId() + "");
            sendMessage.setText(prop.getProperty("parsedateerr"));
            try {
                sendMessage(sendMessage);
                dbChat.setChatState(DATEREG);
                chatFacade.edit(dbChat);
            } catch (TelegramApiException ex1) {
                dbChat.setChatState(DATEREG);
                chatFacade.edit(dbChat);
                Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } catch (TelegramApiException ex) {
            Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void oncoupleNameRegRequest(Message message) {
        SendMessage sendMessage = new SendMessage();
        try {
            //  int chatState = 0;
            dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            if (dbChat != null && dbChat.getChatState() == COUPLENAMEREG) {
                dbWedding = weddingFacade.fetchWeddingByChatId(dbChat.getId());
                if (dbWedding == null) {
                    dbChat.setChatState(DATEREG);

                    newWedding = new Wedding();
                    newWedding.setRegisteredBy(message.getFrom().getId() + "");
                    newWedding.setCoupleName(message.getText());
                    newWedding.setWeddingCode(new Random().nextInt(10000) + "");
                    newWedding.setClientCode(new Random().nextInt(10000) + "");
                    newWedding.setCurrentChatId(dbChat.getId());
                    weddingFacade.create(newWedding);
                    sendMessage.setText(prop.getProperty("congratsmsg") + newWedding.getCoupleName() + " " + Emoji.THUMBS_UP_SIGN + " ");
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                    Thread.sleep(1000);
                    sendMessage.setText(" Your admin wedding code is >> " + newWedding.getWeddingCode());
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                    sendMessage.setText(" Your public wedding code is >> " + newWedding.getClientCode() + ". " + prop.getProperty("publiccodemessg"));
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                    Thread.sleep(1000);
                    //Request Venue
                    chatFacade.edit(dbChat);
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage.setText(prop.getProperty("weddingdate"));

                    sendMessage(sendMessage);
                }
//                else {
//                    dbChat.setChatState(VENUEREG);
//                    sendMessage.setText(prop.getProperty("congratsmsg") + dbWedding.getCoupleName() + " " + Emoji.THUMBS_UP_SIGN + " ");
//                    sendMessage.setChatId(message.getChatId() + "");
//                    sendMessage(sendMessage);
//                    Thread.sleep(1000);
//                    sendMessage.setText(" Your admin wedding code is >> " + dbWedding.getWeddingCode());
//                    sendMessage.setChatId(message.getChatId() + "");
//                    sendMessage(sendMessage);
//                    sendMessage.setText(" Your public wedding code is >> " + dbWedding.getClientCode());
//                    sendMessage.setChatId(message.getChatId() + "");
//                    sendMessage(sendMessage);
//                    //Request Venue
//                    sendMessage.setText(prop.getProperty("venuemsg"));
//                    sendMessage(sendMessage);
//                }
            }

        } catch (Exception ex) {
            Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void onPhoneNumberRegRequest(Message message) {
        SendMessage sendMessage = new SendMessage();

        dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
        if (dbChat != null && dbChat.getChatState() == PHONEREG) {
            dbChat.setChatState(COMPLETEREG);
            dbWedding = weddingFacade.fetchWeddingByChatId(dbChat.getId());
            if (dbWedding != null) {
                try {
                    Wedding tWedding = weddingFacade.fetchWeddingByTel(message.getText());
                    if (tWedding != null) {
                        sendMessage.setChatId(message.getChatId() + "");
                        sendMessage.setText(prop.getProperty("phonenoreqerr"));
                        sendMessage(sendMessage);
                    } else {
                        dbWedding.setTelNumber(message.getText());
                        dbWedding.setCurrentChatId(dbChat.getId());
                        weddingFacade.edit(dbWedding);
                        chatFacade.edit(dbChat);

                        sendMessage.setChatId(message.getChatId() + "");
                        sendMessage.setText(prop.getProperty("phonenoreqconf") + " " + dbWedding.getCoupleName() + " " + Emoji.SMILING_FACE_WITH_SMILING_EYES);
                        sendMessage(sendMessage);
                        sendMessage.setChatId(message.getChatId() + "");
                        sendMessage.setText(prop.getProperty("completeregmsg"));
                        sendMessage(sendMessage);
                        sendMessage.setChatId(message.getChatId() + "");
                        sendMessage(showMainMenu(message));

                    }
                } catch (Exception ex) {

                }
            }
        }

    }

    private void onReceptionRegRequest(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(prop.getProperty("phonenoreq"));
        dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
        if (dbChat != null && dbChat.getChatState() == RECEPTIONREG) {
            dbChat.setChatState(PHONEREG);

            dbWedding = weddingFacade.fetchWeddingByChatId(dbChat.getId());
            if (dbWedding != null) {
                try {
                    dbWedding.setReception(message.getText());
                    dbWedding.setCurrentChatId(dbChat.getId());
                    weddingFacade.edit(dbWedding);
                    chatFacade.edit(dbChat);
                    Thread.sleep(1000);
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                } catch (Exception ex) {

                }
            }

        }

    }

    private void onVenueRegRequest(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(prop.getProperty("receptionmsg"));
        dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
        if (dbChat != null && dbChat.getChatState() == VENUEREG) {
            dbChat.setChatState(RECEPTIONREG);

            dbWedding = weddingFacade.fetchWeddingByChatId(dbChat.getId());
            if (dbWedding != null) {
                try {
                    chatFacade.edit(dbChat);
                    dbWedding.setVenue(message.getText());
                    dbWedding.setCurrentChatId(dbChat.getId());
                    weddingFacade.edit(dbWedding);
                    Thread.sleep(500);
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                } catch (Exception ex) {
                    Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

        }

    }

    private void hideKeyboard(Integer userId, Long chatId, Integer messageId) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setText("Ok");

        ReplyKeyboardHide replyKeyboardHide = new ReplyKeyboardHide();
        replyKeyboardHide.setSelective(true);
        sendMessage.setReplyMarkup(replyKeyboardHide);

        sendMessage(sendMessage);
        //  DatabaseManager.getInstance().insertWeatherState(userId, chatId, STARTSTATE);
    }

    private ReplyKeyboardMarkup getViewWeddingMenuKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(prop.getProperty("viewmywedding"));

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(prop.getProperty("viewotherswedding"));
//        keyboardSecondRow.add(getRateCommand(language));
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
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
        dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
        if (dbChat != null) {

            chatFacade.remove(dbChat);
            chatFacade.create(new Chat(message.getChatId(), message.getFrom().getId(), MAINMENU));
        } else {
            chatFacade.create(new Chat(message.getChatId(), message.getFrom().getId(), MAINMENU));
        }

        return sendMessageRequest;

    }

    private SendMessage showViewWeddingMenu(Message message) throws TelegramApiException {
        SendMessage sendMessageRequest = sendChooseViewWeddingOptionMessage(message.getChatId(), message.getMessageId(),
                getViewWeddingMenuKeyboard());
        dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
        if (dbChat != null) {
            chatFacade.remove(dbChat);
            chatFacade.create(new Chat(message.getChatId(), message.getFrom().getId(), VIEWWEDDING));
        } else {
            chatFacade.create(new Chat(message.getChatId(), message.getFrom().getId(), VIEWWEDDING));
        }

        return sendMessageRequest;

    }

    private SendMessage showOwnWeddingMenu(Message message) throws TelegramApiException {
        SendMessage sendMessageRequest = sendChooseViewOwnWeddingOptionMessage(message.getChatId(), message.getMessageId(),
                getViewOwnWeddingMenuKeyboard());;

        return sendMessageRequest;
    }

    private ReplyKeyboardMarkup getViewOwnWeddingMenuKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(prop.getProperty("couplephoto"));
        keyboardFirstRow.add(prop.getProperty("bridalparty"));

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(prop.getProperty("venuereception"));
        keyboardSecondRow.add(prop.getProperty("editwedding"));
//        keyboardSecondRow.add(getRateCommand(language));

        replyKeyboardMarkup.setKeyboard(keyboard);
        KeyboardRow keyboardThridRow = new KeyboardRow();
        keyboardThridRow.add(prop.getProperty("backtoviewweddingmenu"));
        keyboardThridRow.add(prop.getProperty("cancelwedding"));
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        keyboard.add(keyboardThridRow);

        return replyKeyboardMarkup;
    }

    private SendMessage sendChooseViewOwnWeddingOptionMessage(Long chatId, Integer messageId,
            ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(prop.getProperty("helpmsg"));

        return sendMessage;
    }

    private SendMessage sendChooseOptionMessage(Long chatId, Integer messageId,
            ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(prop.getProperty("helpmsg"));

        return sendMessage;
    }

    private SendMessage sendChooseViewWeddingOptionMessage(Long chatId, Integer messageId,
            ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(prop.getProperty("helpmsg"));

        return sendMessage;
    }

}
