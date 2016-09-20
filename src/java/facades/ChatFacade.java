/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facades;

import entities.Chat;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author zkmatu
 */
@Stateless
public class ChatFacade extends AbstractFacade<Chat> {

    @PersistenceContext(unitName = "WeddingBookPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ChatFacade() {
        super(Chat.class);
    }
    
    public Chat fetchChatByIdAndUserId(Long cid ,int userId){
        Chat dbChat = new Chat();
        
        try{
           Query query = getEntityManager().createNamedQuery("fetchChatState");
           query.setParameter("cid", cid);
            query.setParameter("uid", userId);
            
            if(query.getResultList().isEmpty()){
                dbChat = null;
            }
            else
            {
                dbChat =(Chat) query.getResultList().get(0);
            }
        }
        catch(Exception ex){
            dbChat =null;
        }
        
        return dbChat;
    }
    
}
