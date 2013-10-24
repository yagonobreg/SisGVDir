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
import modelo.Item;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import modelo.Produto;

/**
 *
 * @author Yago
 */
public class ProdutoJpaController implements Serializable {

    public ProdutoJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Produto produto) {
        if (produto.getItems() == null) {
            produto.setItems(new ArrayList<Item>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            List<Item> attachedItems = new ArrayList<Item>();
            for (Item itemsItemToAttach : produto.getItems()) {
                itemsItemToAttach = em.getReference(itemsItemToAttach.getClass(), itemsItemToAttach.getId());
                attachedItems.add(itemsItemToAttach);
            }
            produto.setItems(attachedItems);
            em.persist(produto);
            for (Item itemsItem : produto.getItems()) {
                Produto oldProdutoOfItemsItem = itemsItem.getProduto();
                itemsItem.setProduto(produto);
                itemsItem = em.merge(itemsItem);
                if (oldProdutoOfItemsItem != null) {
                    oldProdutoOfItemsItem.getItems().remove(itemsItem);
                    oldProdutoOfItemsItem = em.merge(oldProdutoOfItemsItem);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Produto produto) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Produto persistentProduto = em.find(Produto.class, produto.getId());
            List<Item> itemsOld = persistentProduto.getItems();
            List<Item> itemsNew = produto.getItems();
            List<Item> attachedItemsNew = new ArrayList<Item>();
            for (Item itemsNewItemToAttach : itemsNew) {
                itemsNewItemToAttach = em.getReference(itemsNewItemToAttach.getClass(), itemsNewItemToAttach.getId());
                attachedItemsNew.add(itemsNewItemToAttach);
            }
            itemsNew = attachedItemsNew;
            produto.setItems(itemsNew);
            produto = em.merge(produto);
            for (Item itemsOldItem : itemsOld) {
                if (!itemsNew.contains(itemsOldItem)) {
                    itemsOldItem.setProduto(null);
                    itemsOldItem = em.merge(itemsOldItem);
                }
            }
            for (Item itemsNewItem : itemsNew) {
                if (!itemsOld.contains(itemsNewItem)) {
                    Produto oldProdutoOfItemsNewItem = itemsNewItem.getProduto();
                    itemsNewItem.setProduto(produto);
                    itemsNewItem = em.merge(itemsNewItem);
                    if (oldProdutoOfItemsNewItem != null && !oldProdutoOfItemsNewItem.equals(produto)) {
                        oldProdutoOfItemsNewItem.getItems().remove(itemsNewItem);
                        oldProdutoOfItemsNewItem = em.merge(oldProdutoOfItemsNewItem);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Long id = produto.getId();
                if (findProduto(id) == null) {
                    throw new NonexistentEntityException("The produto with id " + id + " no longer exists.");
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
            Produto produto;
            try {
                produto = em.getReference(Produto.class, id);
                produto.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The produto with id " + id + " no longer exists.", enfe);
            }
            List<Item> items = produto.getItems();
            for (Item itemsItem : items) {
                itemsItem.setProduto(null);
                itemsItem = em.merge(itemsItem);
            }
            em.remove(produto);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Produto> findProdutoEntities() {
        return findProdutoEntities(true, -1, -1);
    }

    public List<Produto> findProdutoEntities(int maxResults, int firstResult) {
        return findProdutoEntities(false, maxResults, firstResult);
    }

    private List<Produto> findProdutoEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Produto.class));
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

    public Produto findProduto(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Produto.class, id);
        } finally {
            em.close();
        }
    }

    public int getProdutoCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Produto> rt = cq.from(Produto.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
