/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facades;

import entities.Rsvp;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author kabugi
 */
@Stateless
public class RsvpFacade extends AbstractFacade<Rsvp> {

    @PersistenceContext(unitName = "WeddingBookPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    public Rsvp fetchRSVPByPhone(String phone,Long Wid){
        Rsvp dbRsvp = null;
        Query query = getEntityManager().createNamedQuery("checkRSVPPhone");
        query.setParameter("phone", phone);
        query.setParameter("Wid", Wid);
        
        if(!query.getResultList().isEmpty()){
            dbRsvp = (Rsvp)query.getResultList().get(0);
        }
        else{
            dbRsvp = null;
        }
        return dbRsvp;
    }

    public RsvpFacade() {
        super(Rsvp.class);
    }
    
}
