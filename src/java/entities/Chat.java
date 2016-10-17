/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package entities;

import java.io.Serializable;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 *
 * @author zkmatu
 */
@Entity
@Cacheable(false)
@NamedQueries({
    @NamedQuery(name="fetchChatState" ,query="SELECT c FROM Chat c WHERE c.chatId =:cid AND C.userId =:uid")
})
public class Chat implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long chatId;
    private int userId;
    private int chatState;
    private String chatPreviousStep;
     
     private String chatCode;

    public Chat(Long chatId, int userId, int chatState) {
        this.chatId = chatId;
        this.userId = userId;
        this.chatState = chatState;
    }

    public String getChatPreviousStep() {
        return chatPreviousStep;
    }

    public void setChatPreviousStep(String chatPreviousStep) {
        this.chatPreviousStep = chatPreviousStep;
    }
    
    

    public Chat() {
    }

    public String getChatCode() {
        return chatCode;
    }

    public void setChatCode(String chatCode) {
        this.chatCode = chatCode;
    }
    
    
    
    

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    
   

    public int getChatState() {
        return chatState;
    }

    public void setChatState(int chatState) {
        this.chatState = chatState;
    }

   
    
    

    
    
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Chat)) {
            return false;
        }
        Chat other = (Chat) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.ChatMessage[ id=" + id + " ]";
    }
    
}
