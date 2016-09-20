/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facades;

import bots.ServiceBot;
import entities.Wedding;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.logging.BotLogger;

/**
 *
 * @author zkmatu
 */
@Stateless
public class WeddingFacade extends AbstractFacade<Wedding> {

    @PersistenceContext(unitName = "WeddingBookPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public Wedding fetchWeddingByChatId(String chatId){
        Wedding wedding = null;
        Query query = getEntityManager().createNamedQuery("fetchByChatId");
        query.setParameter("chatid", chatId);
        
        if(!query.getResultList().isEmpty()){
            wedding = (Wedding)query.getResultList().get(0);
        }
        return wedding;
    }
   

    public WeddingFacade() {
        super(Wedding.class);
    }

}
