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
      
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
             System.out.println("Starting WeddingBook bot service ..");
            telegramBotsApi.registerBot(new ServiceBot());
             System.out.println("Wedding Book Bot Service started successfully ..");
            
        } catch (TelegramApiException e) {
            BotLogger.error("Error Starting WeddingBook Bot Service", e);
        }

    }
     @PreDestroy 
     public void cleanUp(){
         
     }

}
