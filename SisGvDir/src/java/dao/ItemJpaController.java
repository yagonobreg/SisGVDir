/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import dao.exceptions.NonexistentEntityException;
import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import modelo.Item;
import modelo.Produto;
import modelo.Venda;

/**
 *
 * @author Yago
 */
public class ItemJpaController implements Serializable {

    public ItemJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Item item) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Produto produto = item.getProduto();
            if (produto != null) {
                produto = em.getReference(produto.getClass(), produto.getId());
                item.setProduto(produto);
            }
            Venda venda = item.getVenda();
            if (venda != null) {
                venda = em.getReference(venda.getClass(), venda.getId());
                item.setVenda(venda);
            }
            em.persist(item);
            if (produto != null) {
                produto.getItems().add(item);
                produto = em.merge(produto);
            }
            if (venda != null) {
                venda.getItems().add(item);
                venda = em.merge(venda);
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Item item) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Item persistentItem = em.find(Item.class, item.getId());
            Produto produtoOld = persistentItem.getProduto();
            Produto produtoNew = item.getProduto();
            Venda vendaOld = persistentItem.getVenda();
            Venda vendaNew = item.getVenda();
            if (produtoNew != null) {
                produtoNew = em.getReference(produtoNew.getClass(), produtoNew.getId());
                item.setProduto(produtoNew);
            }
            if (vendaNew != null) {
                vendaNew = em.getReference(vendaNew.getClass(), vendaNew.getId());
                item.setVenda(vendaNew);
            }
            item = em.merge(item);
            if (produtoOld != null && !produtoOld.equals(produtoNew)) {
                produtoOld.getItems().remove(item);
                produtoOld = em.merge(produtoOld);
            }
            if (produtoNew != null && !produtoNew.equals(produtoOld)) {
                produtoNew.getItems().add(item);
                produtoNew = em.merge(produtoNew);
            }
            if (vendaOld != null && !vendaOld.equals(vendaNew)) {
                vendaOld.getItems().remove(item);
                vendaOld = em.merge(vendaOld);
            }
            if (vendaNew != null && !vendaNew.equals(vendaOld)) {
                vendaNew.getItems().add(item);
                vendaNew = em.merge(vendaNew);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Long id = item.getId();
                if (findItem(id) == null) {
                    throw new NonexistentEntityException("The item with id " + id + " no longer exists.");
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
            Item item;
            try {
                item = em.getReference(Item.class, id);
                item.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The item with id " + id + " no longer exists.", enfe);
            }
            Produto produto = item.getProduto();
            if (produto != null) {
                produto.getItems().remove(item);
                produto = em.merge(produto);
            }
            Venda venda = item.getVenda();
            if (venda != null) {
                venda.getItems().remove(item);
                venda = em.merge(venda);
            }
            em.remove(item);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Item> findItemEntities() {
        return findItemEntities(true, -1, -1);
    }

    public List<Item> findItemEntities(int maxResults, int firstResult) {
        return findItemEntities(false, maxResults, firstResult);
    }

    private List<Item> findItemEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Item.class));
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

    public Item findItem(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Item.class, id);
        } finally {
            em.close();
        }
    }

    public int getItemCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Item> rt = cq.from(Item.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
