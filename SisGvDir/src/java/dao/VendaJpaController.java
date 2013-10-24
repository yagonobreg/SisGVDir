/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import dao.exceptions.NonexistentEntityException;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import modelo.Vendedor;
import modelo.Item;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import modelo.Venda;

/**
 *
 * @author Yago
 */
public class VendaJpaController implements Serializable {

    public VendaJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Venda venda) {
        if (venda.getItems() == null) {
            venda.setItems(new ArrayList<Item>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Vendedor vendedor = venda.getVendedor();
            if (vendedor != null) {
                vendedor = em.getReference(vendedor.getClass(), vendedor.getId());
                venda.setVendedor(vendedor);
            }
            List<Item> attachedItems = new ArrayList<Item>();
            for (Item itemsItemToAttach : venda.getItems()) {
                itemsItemToAttach = em.getReference(itemsItemToAttach.getClass(), itemsItemToAttach.getId());
                attachedItems.add(itemsItemToAttach);
            }
            venda.setItems(attachedItems);
            em.persist(venda);
            if (vendedor != null) {
                vendedor.getVendas().add(venda);
                vendedor = em.merge(vendedor);
            }
            for (Item itemsItem : venda.getItems()) {
                Venda oldVendaOfItemsItem = itemsItem.getVenda();
                itemsItem.setVenda(venda);
                itemsItem = em.merge(itemsItem);
                if (oldVendaOfItemsItem != null) {
                    oldVendaOfItemsItem.getItems().remove(itemsItem);
                    oldVendaOfItemsItem = em.merge(oldVendaOfItemsItem);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Venda venda) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Venda persistentVenda = em.find(Venda.class, venda.getId());
            Vendedor vendedorOld = persistentVenda.getVendedor();
            Vendedor vendedorNew = venda.getVendedor();
            List<Item> itemsOld = persistentVenda.getItems();
            List<Item> itemsNew = venda.getItems();
            if (vendedorNew != null) {
                vendedorNew = em.getReference(vendedorNew.getClass(), vendedorNew.getId());
                venda.setVendedor(vendedorNew);
            }
            List<Item> attachedItemsNew = new ArrayList<Item>();
            for (Item itemsNewItemToAttach : itemsNew) {
                itemsNewItemToAttach = em.getReference(itemsNewItemToAttach.getClass(), itemsNewItemToAttach.getId());
                attachedItemsNew.add(itemsNewItemToAttach);
            }
            itemsNew = attachedItemsNew;
            venda.setItems(itemsNew);
            venda = em.merge(venda);
            if (vendedorOld != null && !vendedorOld.equals(vendedorNew)) {
                vendedorOld.getVendas().remove(venda);
                vendedorOld = em.merge(vendedorOld);
            }
            if (vendedorNew != null && !vendedorNew.equals(vendedorOld)) {
                vendedorNew.getVendas().add(venda);
                vendedorNew = em.merge(vendedorNew);
            }
            for (Item itemsOldItem : itemsOld) {
                if (!itemsNew.contains(itemsOldItem)) {
                    itemsOldItem.setVenda(null);
                    itemsOldItem = em.merge(itemsOldItem);
                }
            }
            for (Item itemsNewItem : itemsNew) {
                if (!itemsOld.contains(itemsNewItem)) {
                    Venda oldVendaOfItemsNewItem = itemsNewItem.getVenda();
                    itemsNewItem.setVenda(venda);
                    itemsNewItem = em.merge(itemsNewItem);
                    if (oldVendaOfItemsNewItem != null && !oldVendaOfItemsNewItem.equals(venda)) {
                        oldVendaOfItemsNewItem.getItems().remove(itemsNewItem);
                        oldVendaOfItemsNewItem = em.merge(oldVendaOfItemsNewItem);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Long id = venda.getId();
                if (findVenda(id) == null) {
                    throw new NonexistentEntityException("The venda with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Long id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Venda venda;
            try {
                venda = em.getReference(Venda.class, id);
                venda.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The venda with id " + id + " no longer exists.", enfe);
            }
            Vendedor vendedor = venda.getVendedor();
            if (vendedor != null) {
                vendedor.getVendas().remove(venda);
                vendedor = em.merge(vendedor);
            }
            List<Item> items = venda.getItems();
            for (Item itemsItem : items) {
                itemsItem.setVenda(null);
                itemsItem = em.merge(itemsItem);
            }
            em.remove(venda);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Venda> findVendaEntities() {
        return findVendaEntities(true, -1, -1);
    }

    public List<Venda> findVendaEntities(int maxResults, int firstResult) {
        return findVendaEntities(false, maxResults, firstResult);
    }

    private List<Venda> findVendaEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Venda.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Venda findVenda(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Venda.class, id);
        } finally {
            em.close();
        }
    }

    public int getVendaCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Venda> rt = cq.from(Venda.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
