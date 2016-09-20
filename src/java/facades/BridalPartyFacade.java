/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facades;

import entities.BridalParty;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author zkmatu
 */
@Stateless
public class BridalPartyFacade extends AbstractFacade<BridalParty> {

    @PersistenceContext(unitName = "WeddingBookPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public BridalPartyFacade() {
        super(BridalParty.class);
    }
    
}
