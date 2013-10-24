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
import modelo.Venda;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import modelo.Vendedor;

/**
 *
 * @author Yago
 */
public class VendedorJpaController implements Serializable {

    public VendedorJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Vendedor vendedor) {
        if (vendedor.getVendas() == null) {
            vendedor.setVendas(new ArrayList<Venda>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            List<Venda> attachedVendas = new ArrayList<Venda>();
            for (Venda vendasVendaToAttach : vendedor.getVendas()) {
                vendasVendaToAttach = em.getReference(vendasVendaToAttach.getClass(), vendasVendaToAttach.getId());
                attachedVendas.add(vendasVendaToAttach);
            }
            vendedor.setVendas(attachedVendas);
            em.persist(vendedor);
            for (Venda vendasVenda : vendedor.getVendas()) {
                Vendedor oldVendedorOfVendasVenda = vendasVenda.getVendedor();
                vendasVenda.setVendedor(vendedor);
                vendasVenda = em.merge(vendasVenda);
                if (oldVendedorOfVendasVenda != null) {
                    oldVendedorOfVendasVenda.getVendas().remove(vendasVenda);
                    oldVendedorOfVendasVenda = em.merge(oldVendedorOfVendasVenda);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Vendedor vendedor) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Vendedor persistentVendedor = em.find(Vendedor.class, vendedor.getId());
            List<Venda> vendasOld = persistentVendedor.getVendas();
            List<Venda> vendasNew = vendedor.getVendas();
            List<Venda> attachedVendasNew = new ArrayList<Venda>();
            for (Venda vendasNewVendaToAttach : vendasNew) {
                vendasNewVendaToAttach = em.getReference(vendasNewVendaToAttach.getClass(), vendasNewVendaToAttach.getId());
                attachedVendasNew.add(vendasNewVendaToAttach);
            }
            vendasNew = attachedVendasNew;
            vendedor.setVendas(vendasNew);
            vendedor = em.merge(vendedor);
            for (Venda vendasOldVenda : vendasOld) {
                if (!vendasNew.contains(vendasOldVenda)) {
                    vendasOldVenda.setVendedor(null);
                    vendasOldVenda = em.merge(vendasOldVenda);
                }
            }
            for (Venda vendasNewVenda : vendasNew) {
                if (!vendasOld.contains(vendasNewVenda)) {
                    Vendedor oldVendedorOfVendasNewVenda = vendasNewVenda.getVendedor();
                    vendasNewVenda.setVendedor(vendedor);
                    vendasNewVenda = em.merge(vendasNewVenda);
                    if (oldVendedorOfVendasNewVenda != null && !oldVendedorOfVendasNewVenda.equals(vendedor)) {
                        oldVendedorOfVendasNewVenda.getVendas().remove(vendasNewVenda);
                        oldVendedorOfVendasNewVenda = em.merge(oldVendedorOfVendasNewVenda);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Long id = vendedor.getId();
                if (findVendedor(id) == null) {
                    throw new NonexistentEntityException("The vendedor with id " + id + " no longer exists.");
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
            Vendedor vendedor;
            try {
                vendedor = em.getReference(Vendedor.class, id);
                vendedor.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The vendedor with id " + id + " no longer exists.", enfe);
            }
            List<Venda> vendas = vendedor.getVendas();
            for (Venda vendasVenda : vendas) {
                vendasVenda.setVendedor(null);
                vendasVenda = em.merge(vendasVenda);
            }
            em.remove(vendedor);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Vendedor> findVendedorEntities() {
        return findVendedorEntities(true, -1, -1);
    }

    public List<Vendedor> findVendedorEntities(int maxResults, int firstResult) {
        return findVendedorEntities(false, maxResults, firstResult);
    }

    private List<Vendedor> findVendedorEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Vendedor.class));
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

    public Vendedor findVendedor(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Vendedor.class, id);
        } finally {
            em.close();
        }
    }

    public int getVendedorCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Vendedor> rt = cq.from(Vendedor.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
