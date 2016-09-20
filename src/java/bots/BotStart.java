/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bots;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.logging.BotLogger;

/**
 *
 * @author zkmatu
 */
@javax.ejb.Singleton 
@javax.ejb.Startup 
public class BotStart  {

    @PostConstruct  
    private void init() {
       System.out.println("Starting wedding book bot service ..");
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new ServiceBot());
        } catch (TelegramApiException e) {
            BotLogger.error("ERROR", e);
        }

    }
     @PreDestroy 
     public void cleanUp(){
         
     }

}
