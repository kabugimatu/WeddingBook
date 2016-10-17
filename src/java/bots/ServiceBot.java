/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bots;

import botconfig.BotConfig;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import entities.Chat;
import entities.Rsvp;
import entities.Wedding;
import facades.ChatFacade;
import facades.RsvpFacade;
import facades.WeddingFacade;
import facades.WeddingSupportFacade;
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
import org.telegram.telegrambots.api.methods.send.SendContact;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendLocation;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.objects.Contact;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;

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
    private static final int VIEWINGOTHERDING = 14;
    private static final int SENDINGOTHERPHOTO = 15;
    private static final int SENDINGCONTACTRSVP = 16;
    private static final int EDITMODE = 17;
    private static final int PHOTOEDITMODE = 18;
    private static final int VENUEEDITMODE = 19;
    private static final int RECEPTIONEDITMODE = 20;

    private boolean fromBack;
    private String filePathUrl = "https://api.telegram.org/file/bot" + BotConfig.BOT_TOKEN + "/";
    private ChatFacade chatFacade;
    private WeddingFacade weddingFacade;
    private RsvpFacade rsvpFacade;
    private WeddingSupportFacade weddingSupportFacade;
    private Properties sysProps;
    Location receivedLocation = null;
    GeocodingApiRequest apiReverseGeoRequest;
    GeoApiContext geoCodeMapContext = new GeoApiContext().setApiKey("AIzaSyBhRMBYFtF8JuiWk53dLlcs4qECpfaoktA");
    private Wedding newWedding, dbWedding;
    private Rsvp newRsvp;
    Chat dbChat;
    SendContact sendContact = null;

    public ServiceBot() {
        input = getClass().getResourceAsStream("/properties/properties.properties");

        try {
            // load  properties file
            this.chatFacade = (ChatFacade) new InitialContext().lookup("java:global/WeddingBook/ChatFacade");
            this.weddingFacade = (WeddingFacade) new InitialContext().lookup("java:global/WeddingBook/WeddingFacade");
            this.rsvpFacade = (RsvpFacade) new InitialContext().lookup("java:global/WeddingBook/RsvpFacade");
            this.weddingSupportFacade = (WeddingSupportFacade) new InitialContext().lookup("java:global/WeddingBook/WeddingSupportFacade");
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
                dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
                // System.out.println("Message has document --> " + message.hasDocument());
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
                } else if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
                    System.out.println("DOc received");
                    if (dbChat.getChatState() == SENDINGOWNWEDPHOTO) {
                        onReceiveWeddingPhoto(message);
                    }
                     if (dbChat.getChatState() == PHOTOEDITMODE) {
                        onReceiveWeddingEditPhoto(message);
                    }
                    //  handleIncomingMessage(message);
                } else if (message.hasLocation() && dbChat.getChatState() == VENUEREG) {
                    // System.out.println("Location received");
                    onVenueRegRequest(message);
                } else if (message.hasLocation() && dbChat.getChatState() == RECEPTIONREG) {
                    // System.out.println("Loc received");
                    onReceptionRegRequest(message);
                } else if (message.hasLocation() && dbChat.getChatState() == VENUEEDITMODE) {
                    System.out.println("Location received");
                    onVenueRegEditRequest(message);
                } else if (message.hasLocation() && dbChat.getChatState() == RECEPTIONEDITMODE) {
                    // System.out.println("Loc received");
                    onReceptionRegEditRequest(message);
                } else if (dbChat.getChatState() == SENDINGCONTACTRSVP) {
                    onreceiveRSVP(message);
                } else if (dbChat.getChatState() == PHONEREG) {
                    onPhoneNumberRegRequest(message);
                }
                //  else if(message.is)

            } catch (Exception e) {
                e.printStackTrace();
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
            } 
            else if (message.getText().equalsIgnoreCase(prop.getProperty("commandstop"))) {
                hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
            } 
            
            else if (message.getText().equalsIgnoreCase(prop.getProperty("commandback"))) {
                hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
                sendMessage(showViewWeddingMenu(message));
            }
            else if (message.getText().equalsIgnoreCase(prop.getProperty("gettingMarried"))) {
                ongettingMarriedRequest(message);

            } else if (message.getText().equalsIgnoreCase(prop.getProperty("viewwedding"))) {
                //  Thread.sleep(500);
                sendMessage(showViewWeddingMenu(message));

            } else if (message.getText().equalsIgnoreCase(prop.getProperty("viewmywedding"))) {
                //     Thread.sleep(500);
                messageOnViewMyWeddingRequest(message);

            } else if (message.getText().equalsIgnoreCase(prop.getProperty("viewotherswedding"))) {
                //   Thread.sleep(500);
                messageOnViewMyOtherRequest(message);

            } else if (message.getText().equalsIgnoreCase(prop.getProperty("backtoviewweddingmenu"))) {
                sendMessage(showMainMenu(message));

            } else if (message.getText().equalsIgnoreCase(prop.getProperty("couplephoto"))) {
                onViewWeddingPhoto(message);

            } else if (message.getText().equalsIgnoreCase(prop.getProperty("venuereception"))) {

                onrequestVenue(message);
            } else if (message.getText().equalsIgnoreCase(prop.getProperty("rsvp"))) {

                onRsvp(message);
            } else if (message.getText().equalsIgnoreCase(prop.getProperty("editwedding"))) {
                sendMessage(showOwnEditMenu(message));
            } else if (message.getText().equalsIgnoreCase(prop.getProperty("editcouplephoto")) && dbChat.getChatState() == EDITMODE) {
                hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
                requestEditPhoto(message);
            } else if (message.getText().equalsIgnoreCase(prop.getProperty("editvenuereception")) && dbChat.getChatState() == EDITMODE) {
                hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
                requestEditVenue(message);
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
                    case OTHERWEDDINGREQ:
                        processWeddingPublicCode(message);
                        break;
                    case SENDINGOWNWEDPHOTO:
                        onReceiveWeddingPhoto(message);
                        break;
                    case SENDINGCONTACTRSVP:
                        onreceiveRSVP(message);
                        break;
                    case PHOTOEDITMODE:
                        onReceiveWeddingEditPhoto(message);
                        break;

                    case VENUEEDITMODE:
                        onVenueRegEditRequest(message);
                        break;

                    case RECEPTIONEDITMODE:
                        onReceptionRegEditRequest(message);
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

    private void onReceptionRegEditRequest(Message message) {
        SendMessage sendMessage = new SendMessage();

        sendMessage.setText(prop.getProperty("thankyou"));
        dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
        if (dbChat != null && dbChat.getChatState() == RECEPTIONEDITMODE) {
            dbChat.setChatState(EDITMODE);

            dbWedding = weddingFacade.fetchWeddingByChatId(dbChat.getId());
            if (dbWedding != null) {
                try {

                    dbWedding.setCurrentChatId(dbChat.getId());
                    if (message.hasLocation()) {
                        receivedLocation = message.getLocation();
                        dbWedding.setReceptionLatitude(receivedLocation.getLatitude());
                        dbWedding.setReceptionLongitude(receivedLocation.getLongitude());
//                        GeocodingResult[] results = GeocodingApi.newRequest(geoCodeMapContext).latlng(new LatLng(receivedLocation.getLatitude(), receivedLocation.getLongitude())).await();
//                        dbWedding.setReception(results[0].formattedAddress);
                    } else {
                        dbWedding.setReception(message.getText());
                    }
                    weddingFacade.edit(dbWedding);
                    chatFacade.edit(dbChat);
                    // Thread.sleep(1000);
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);

                } catch (Exception ex) {

                }
            }

        }

    }

    private void onVenueRegEditRequest(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(prop.getProperty("receptionmsg"));
          sendMessage.setChatId(message.getChatId() + "");
        dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
   try {
        if (dbChat != null && dbChat.getChatState() == VENUEEDITMODE) {
            dbChat.setChatState(RECEPTIONEDITMODE);

            dbWedding = weddingFacade.fetchWeddingByCreator(message.getFrom().getId()+"");
            if (dbWedding != null) {
             System.out.println("Found wedding..");

                    chatFacade.edit(dbChat);
                    if (message.hasLocation()) {
                         
                        receivedLocation = message.getLocation();
                        dbWedding.setVenueLatitude(receivedLocation.getLatitude());
                        dbWedding.setVenueLongitude(receivedLocation.getLongitude());
//                        GeocodingResult[] results = GeocodingApi.newRequest(geoCodeMapContext).latlng(new LatLng(receivedLocation.getLatitude(), receivedLocation.getLongitude())).await();
//                        dbWedding.setVenue(results[0].formattedAddress);
                    } else {
                        dbWedding.setVenue(message.getText());
                    }
                    dbWedding.setCurrentChatId(dbChat.getId());

                    weddingFacade.edit(dbWedding);
                    Thread.sleep(500);
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                

            }

        }
        } catch (Exception ex) {
                    Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex);
                }

    }

    private void requestEditVenue(Message message) {
        try {
            dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setChatId(message.getChatId() + "");
            sendMessageRequest.setText(prop.getProperty("venuemsg"));
            dbChat.setChatState(VENUEEDITMODE);
            chatFacade.edit(dbChat);
            sendMessage(sendMessageRequest);

        } catch (Exception ex) {
            Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void requestEditPhoto(Message message) {
        try {
            dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            SendMessage sendMessageRequest = new SendMessage();
            sendMessageRequest.setChatId(message.getChatId() + "");
            sendMessageRequest.setText(prop.getProperty("photorequest3"));
            dbChat.setChatState(PHOTOEDITMODE);
            chatFacade.edit(dbChat);
            sendMessage(sendMessageRequest);

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

            byte[] bytes = new byte[10000];
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

    private void onreceiveRSVP(Message message) {
        // System.out.println("Receiving contact..");
        Contact rsvpContact = message.getContact();
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId() + "");
        try {
            newRsvp = new Rsvp();
            newRsvp.setFirstName(rsvpContact.getFirstName());
            newRsvp.setFirstName(rsvpContact.getLastName());
            newRsvp.setTelNumber(rsvpContact.getPhoneNumber());
            dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            dbWedding = weddingFacade.fetchByPublicCode(dbChat.getChatCode());
            if (rsvpFacade.fetchRSVPByPhone(rsvpContact.getPhoneNumber(), dbWedding.getId()) != null) {
                sendMessageRequest.setText(prop.getProperty("rsvpexists"));
                sendMessage(sendMessageRequest);
                sendMessage(showPublicWeddingMenu(message));
            } else if (dbWedding != null) {
                if (dbWedding.getTelNumber().equalsIgnoreCase(rsvpContact.getPhoneNumber())) {
                    sendMessageRequest.setText(prop.getProperty("rsvpownerror"));
                    sendMessage(sendMessageRequest);
                } else {
                    newRsvp.setWeddingID(dbWedding.getId());
                    rsvpFacade.create(newRsvp);
                    dbWedding.getWeddingRSVPs().add(newRsvp);
                    weddingFacade.edit(dbWedding);
                    dbChat.setChatState(VIEWINGOTHERDING);
                    chatFacade.edit(dbChat);
                    sendMessageRequest.setText(prop.getProperty("rsvpsuccess") + " " + Emoji.PERSON_RAISING_BOTH_HANDS_IN_CELEBRATION + " " + Emoji.PERSON_RAISING_BOTH_HANDS_IN_CELEBRATION + " " + prop.getProperty("keepposted"));
                    sendMessage(sendMessageRequest);

                    sendMessage(showPublicWeddingMenu(message));
                }
            } else {
                sendMessage(showMainMenu(message));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void onRsvp(Message message) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId() + "");
        dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
        dbWedding = weddingFacade.fetchByPublicCode(dbChat.getChatCode());
        // sendContact = new  SendContact();
        try {
            System.out.println("Sending contact keyboard");
            dbChat.setChatState(SENDINGCONTACTRSVP);
            chatFacade.edit(dbChat);
            sendMessage(requestContact(message, prop.getProperty("sendmycontact") + " '" + dbWedding.getCoupleName() + "'"));

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void onrequestVenue(Message message) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId() + "");
        try {
            dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            if (dbChat.getChatState() == VIEWINGOWNWEDDINGREQ) {
                dbWedding = weddingFacade.fetchWeddingByCreator(message.getFrom().getId() + "");

            } else {
                dbWedding = weddingFacade.fetchByPublicCode(dbChat.getChatCode());
            }

            if (dbWedding == null) {

                sendMessage(showMainMenu(message));

            } else {

                if (dbWedding.getVenue() != null && dbWedding.getReception() != null) {
                    sendMessageRequest.setReplyToMessageId(message.getMessageId());

                    sendMessageRequest.setText(dbWedding.getCoupleName() + "'s" + " wedding will be held at '" + dbWedding.getVenue() + "' and the reception at '" + dbWedding.getReception() + "'");
                    sendMessage(sendMessageRequest);
                }

                if (dbWedding.getVenueLatitude() != 0) {
                    sendMessageRequest.setText("The venue >> ");
                    sendMessage(sendMessageRequest);

                    SendLocation venueLocation = new SendLocation();
                    venueLocation.setLatitude(new Float(dbWedding.getVenueLatitude()));
                    venueLocation.setLongitude(new Float(dbWedding.getVenueLongitude()));
                    venueLocation.setChatId(message.getChatId() + "");
                    /// venueLocation.setReplyToMessageId(message.getMessageId());

                    sendLocation(venueLocation);
                }
                if (dbWedding.getReceptionLatitude() != 0) {
                    sendMessageRequest.setText("The reception >> ");
                    sendMessageRequest.setChatId(message.getChatId() + "");
                    sendMessage(sendMessageRequest);
                    SendLocation receptionLocation = new SendLocation();
                    receptionLocation.setLatitude(new Float(dbWedding.getReceptionLatitude()));
                    receptionLocation.setLongitude(new Float(dbWedding.getReceptionLongitude()));
                    receptionLocation.setChatId(message.getChatId() + "");
                    receptionLocation.setReplyToMessageId(message.getMessageId());
                    sendLocation(receptionLocation);
                }

            }

        } catch (Exception ex) {
            Logger.getLogger(ServiceBot.class.getName()).log(Level.SEVERE, null, ex);

        }

    }

    private void onReceiveWeddingEditPhoto(Message message) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId() + "");
        String downloadFilePath = "";
        try {

            if (message.getPhoto() != null) {
                System.out.println("Receiving photo ");
                dbWedding = weddingFacade.fetchWeddingByCreator(message.getFrom().getId() + "");
                List<PhotoSize> photos = message.getPhoto();
                System.out.println("Photos --> " + photos.size());

                GetFile getFileRequest = new GetFile();
                // if(photos.get(i).)
                getFileRequest.setFileId(photos.get(photos.size() - 1).getFileId());
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

                sendMessageRequest.setText(prop.getProperty("photoreceived"));
                sendMessage(sendMessageRequest);

                sendMessageRequest = showOwnEditMenu(message);
                sendMessageRequest.setChatId(message.getChatId() + "");
                sendMessage(sendMessageRequest);
                // fileFromSystem.delete();

            } else {
                hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
                sendMessageRequest.setText(prop.getProperty("photorequest2"));
                sendMessage(sendMessageRequest);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

                GetFile getFileRequest = new GetFile();
                // if(photos.get(i).)
                getFileRequest.setFileId(photos.get(photos.size() - 1).getFileId());
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
            if (dbChat != null && dbChat.getChatState() == VIEWINGOWNWEDDINGREQ) {

                dbWedding = weddingFacade.fetchWeddingByCreator(message.getFrom().getId() + "");
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

                    SendDocument sendDocRequest = new SendDocument();

                    sendDocRequest.setChatId(message.getChatId().toString());
                    java.io.File fileToSend = new java.io.File(strFilePath);
                    sendDocRequest.setNewDocument(fileToSend);

                    //    System.out.println("Sending phtoto -->   " + strFilePath );
                    sendDocument(sendDocRequest);
                    fileToSend.delete();

                }

            } else {

                dbWedding = weddingFacade.fetchByPublicCode(dbChat.getChatCode());

                if (dbWedding.getCouplePhoto() == null) {

                    sendMessageRequest.setText(prop.getProperty("nophotopublic"));
                    sendMessage(sendMessageRequest);

                } else {

                    String strFilePath = sysProps.getProperty("user.home") + sysProps.getProperty("file.separator") + "Documents" + sysProps.getProperty("file.separator") + "dev" + sysProps.getProperty("file.separator") + new Random().nextInt(100) + ".jpeg";
                    FileOutputStream fos = new FileOutputStream(strFilePath);
                    fos.write(dbWedding.getCouplePhoto());
                    fos.close();

                    SendDocument sendDocRequest = new SendDocument();

                    sendDocRequest.setChatId(message.getChatId().toString());
                    java.io.File fileToSend = new java.io.File(strFilePath);
                    sendDocRequest.setNewDocument(fileToSend);

                    //    System.out.println("Sending phtoto -->   " + strFilePath );
                    sendDocument(sendDocRequest);
                    fileToSend.delete();

                }

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void processWeddingPublicCode(Message message) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId() + "");
        try {
            dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            if (dbChat != null && dbChat.getChatState() == OTHERWEDDINGREQ) {
                dbWedding = weddingFacade.fetchByPublicCode(message.getText());
                if (dbWedding != null) {
                    sendMessageRequest.setText(prop.getProperty("ownweddingwelcoome") + Emoji.WAVING_HAND_SIGN + " ?" + "\n" + "You are now viewing '" + dbWedding.getCoupleName() + "'s ' wedding  ");
                    sendMessage(sendMessageRequest);

                    sendMessageRequest = showPublicWeddingMenu(message);
                    sendMessageRequest.setChatId(message.getChatId() + "");
                    Thread.sleep(500);
                    sendMessage(sendMessageRequest);
                    dbChat.setChatState(VIEWINGOTHERDING);
                    dbChat.setChatCode(message.getText());
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

    private void messageOnViewMyOtherRequest(Message message) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.setChatId(message.getChatId() + "");
        // System.out.println("Requeting code");
        try {
            dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
            if (dbChat != null && dbChat.getChatState() == VIEWWEDDING) {
                dbChat.setChatState(OTHERWEDDINGREQ);
                chatFacade.edit(dbChat);
                hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
                // Thread.sleep(1000);
                sendMessageRequest.setText(prop.getProperty("weddingpubliccodereq"));
                sendMessage(sendMessageRequest);

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
                    //   Thread.sleep(500);
                    chatStatus.setChatState(COUPLENAMEREG);
                    chatFacade.edit(chatStatus);
                    sendMessageRequest.setChatId(message.getChatId() + "");
                    hideKeyboard(message.getFrom().getId(), message.getChatId(), message.getMessageId());
                    sendMessageRequest.setText(prop.getProperty("congratsmessg") + Emoji.SMILING_FACE_WITH_SMILING_EYES);
                    sendMessage(sendMessageRequest);
                    sendMessageRequest.setChatId(message.getChatId() + "");
                    sendMessageRequest.setText(prop.getProperty("couplerequest"));
                    sendMessage(sendMessageRequest);

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
                    //  Thread.sleep(1000);
                    sendMessage.setText(" Your admin wedding code is >> " + newWedding.getWeddingCode());
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                    sendMessage.setText(" Your public wedding code is >> " + newWedding.getClientCode() + ". " + prop.getProperty("publiccodemessg"));
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);
                    //  Thread.sleep(1000);
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
        Contact receivedContact = message.getContact();
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
                        dbWedding.setTelNumber(receivedContact.getPhoneNumber());
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

                    dbWedding.setCurrentChatId(dbChat.getId());
                    if (message.hasLocation()) {
                        receivedLocation = message.getLocation();
                        dbWedding.setReceptionLatitude(receivedLocation.getLatitude());
                        dbWedding.setReceptionLongitude(receivedLocation.getLongitude());
//                        GeocodingResult[] results = GeocodingApi.newRequest(geoCodeMapContext).latlng(new LatLng(receivedLocation.getLatitude(), receivedLocation.getLongitude())).await();
//                        dbWedding.setReception(results[0].formattedAddress);
                    } else {
                        dbWedding.setReception(message.getText());
                    }
                    weddingFacade.edit(dbWedding);
                    chatFacade.edit(dbChat);
                    Thread.sleep(1000);
                    sendMessage.setChatId(message.getChatId() + "");
                    sendMessage(sendMessage);

                    sendMessage(requestContact(message, prop.getProperty("sendmycontactreg")));
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
                    if (message.hasLocation()) {
                        receivedLocation = message.getLocation();
                        dbWedding.setVenueLatitude(receivedLocation.getLatitude());
                        dbWedding.setVenueLongitude(receivedLocation.getLongitude());
//                        GeocodingResult[] results = GeocodingApi.newRequest(geoCodeMapContext).latlng(new LatLng(receivedLocation.getLatitude(), receivedLocation.getLongitude())).await();
//                        dbWedding.setVenue(results[0].formattedAddress);
                    } else {
                        dbWedding.setVenue(message.getText());
                    }
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

        KeyboardRow keyboardThirdRow = new KeyboardRow();
        keyboardThirdRow.add(prop.getProperty("backtoviewweddingmenu"));

        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        keyboard.add(keyboardThirdRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private ReplyKeyboardMarkup getMainContactRequestKeyBoard(String text) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();

        KeyboardButton contactKeyboardBTN = new KeyboardButton();
        contactKeyboardBTN.setRequestContact(true);
        contactKeyboardBTN.setText(text);

        keyboardFirstRow.add(contactKeyboardBTN);
        keyboard.add(keyboardFirstRow);
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
        KeyboardButton kbtn = new KeyboardButton();

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(prop.getProperty("viewwedding"));
//        keyboardSecondRow.add(getRateCommand(language));
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private SendMessage requestContact(Message message, String text) throws TelegramApiException {
        SendMessage sendMessageRequest;
        sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                getMainContactRequestKeyBoard(text), Emoji.SMILING_FACE_WITH_SMILING_EYES + "");
        //  dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());

        return sendMessageRequest;
    }

    private SendMessage showMainMenu(Message message) throws TelegramApiException {
        SendMessage sendMessageRequest;
        sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                getMainMenuKeyboard(), prop.getProperty("helpmsg"));
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

    private SendMessage showOwnEditMenu(Message message) throws TelegramApiException {
        SendMessage sendMessageRequest = sendChooseViewOwnWeddingOptionEditMessage(message.getChatId(), message.getMessageId(),
                getViewOwnWeddingEditMenuKeyboard());

        dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
        if (dbChat != null) {
            dbChat.setChatState(EDITMODE);
            chatFacade.edit(dbChat);
        } else {
            sendMessageRequest = showMainMenu(message);
        }

        return sendMessageRequest;
    }

    private SendMessage showOwnWeddingMenu(Message message) throws TelegramApiException {
        SendMessage sendMessageRequest = sendChooseViewOwnWeddingOptionMessage(message.getChatId(), message.getMessageId(),
                getViewOwnWeddingMenuKeyboard());;

        return sendMessageRequest;
    }

    private SendMessage showPublicWeddingMenu(Message message) throws TelegramApiException {
        SendMessage sendMessageRequest = sendChooseViewPublicWeddingOptionMessage(message.getChatId(), message.getMessageId(),
                getViewPublicWeddingMenuKeyboard());;

        dbChat = chatFacade.fetchChatByIdAndState(message.getChatId(), message.getFrom().getId());
        if (dbChat != null) {
            dbChat.setChatState(VIEWINGOTHERDING);
            chatFacade.edit(dbChat);
        } else {
            chatFacade.create(new Chat(message.getChatId(), message.getFrom().getId(), MAINMENU));
        }

        return sendMessageRequest;
    }

    private ReplyKeyboardMarkup getViewPublicWeddingMenuKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(prop.getProperty("couplephoto"));

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(prop.getProperty("venuereception"));
        keyboardSecondRow.add(prop.getProperty("rsvp"));

//        keyboardSecondRow.add(getRateCommand(language));
//        KeyboardRow keyboardThridRow = new KeyboardRow();
//        keyboardThridRow.add(prop.getProperty("supportus"));
        KeyboardRow keyboardFourthRow = new KeyboardRow();
        keyboardFourthRow.add(prop.getProperty("backtoviewweddingmenu"));

        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        //  keyboard.add(keyboardThridRow);
        keyboard.add(keyboardFourthRow);

        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private ReplyKeyboardMarkup getViewOwnWeddingEditMenuKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(prop.getProperty("editcouplephoto"));

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(prop.getProperty("editvenuereception"));

        KeyboardRow keyboardFourthRow = new KeyboardRow();
        keyboardFourthRow.add(prop.getProperty("backtoviewweddingmenu"));

        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        // keyboard.add(keyboardThirdRow);
        keyboard.add(keyboardFourthRow);

        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private ReplyKeyboardMarkup getViewOwnWeddingMenuKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboad(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(prop.getProperty("couplephoto"));

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add(prop.getProperty("venuereception"));

        keyboardSecondRow.add(prop.getProperty("editwedding"));

        KeyboardRow keyboardFourthRow = new KeyboardRow();
        keyboardFourthRow.add(prop.getProperty("backtoviewweddingmenu"));

        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        // keyboard.add(keyboardThirdRow);
        keyboard.add(keyboardFourthRow);

        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private SendMessage sendChooseViewPublicWeddingOptionMessage(Long chatId, Integer messageId,
            ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(prop.getProperty("helpmsg"));

        return sendMessage;
    }

    private SendMessage sendChooseViewOwnWeddingOptionEditMessage(Long chatId, Integer messageId,
            ReplyKeyboard replyKeyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(prop.getProperty("editques"));

        return sendMessage;
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
            ReplyKeyboard replyKeyboard, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        //   sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(text);

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
