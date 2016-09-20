/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author zkmatu
 */
@Entity
public class Wedding implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    String coupleName;
    private String weddingCode;
    private String clientCode;
    @Temporal(TemporalType.DATE)
    private Date weddingDate;
    @Temporal(TemporalType.TIME)
    private Date churchTime;
    private String currentChatId;
    private byte[] couplePhoto;
     
    private String venue;
    
    private String reception;
    
    private double venueLatLong;
    
    private double receptionLatLong;
    
     private String telNumber;
     
     private String locale;
     
    @OneToMany
    private List<BridalParty> bridalParty = new ArrayList<>();

    public String getTelNumber() {
        return telNumber;
    }

    public void setTelNumber(String telNumber) {
        this.telNumber = telNumber;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
    
    
    
    

    public String getCurrentChatId() {
        return currentChatId;
    }

    public void setCurrentChatId(String currentChatId) {
        this.currentChatId = currentChatId;
    }
    
    

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }
    
    

    public String getWeddingCode() {
        return weddingCode;
    }

    public void setWeddingCode(String weddingCode) {
        this.weddingCode = weddingCode;
    }
    
    

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getReception() {
        return reception;
    }

    public void setReception(String reception) {
        this.reception = reception;
    }

    public double getVenueLatLong() {
        return venueLatLong;
    }

    public void setVenueLatLong(double venueLatLong) {
        this.venueLatLong = venueLatLong;
    }

    public double getReceptionLatLong() {
        return receptionLatLong;
    }

    public void setReceptionLatLong(double receptionLatLong) {
        this.receptionLatLong = receptionLatLong;
    }

    
    
    
    public Date getWeddingDate() {
        return weddingDate;
    }

    public void setWeddingDate(Date weddingDate) {
        this.weddingDate = weddingDate;
    }

    public Date getChurchTime() {
        return churchTime;
    }

    public void setChurchTime(Date churchTime) {
        this.churchTime = churchTime;
    }
    
    
    

    public String getCoupleName() {
        return coupleName;
    }

    public void setCoupleName(String coupleName) {
        this.coupleName = coupleName;
    }

    public byte[] getCouplePhoto() {
        return couplePhoto;
    }

    public void setCouplePhoto(byte[] couplePhoto) {
        this.couplePhoto = couplePhoto;
    }

    public List<BridalParty> getBridalParty() {
        return bridalParty;
    }

    public void setBridalParty(List<BridalParty> bridalParty) {
        this.bridalParty = bridalParty;
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
        if (!(object instanceof Wedding)) {
            return false;
        }
        Wedding other = (Wedding) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "entities.Wedding[ id=" + id + " ]";
    }
    
}
