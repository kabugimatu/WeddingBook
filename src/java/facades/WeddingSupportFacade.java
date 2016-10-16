/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facades;

import entities.WeddingSupport;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author kabugi
 */
@Stateless
public class WeddingSupportFacade extends AbstractFacade<WeddingSupport> {

    @PersistenceContext(unitName = "WeddingBookPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public WeddingSupportFacade() {
        super(WeddingSupport.class);
    }
    
}
