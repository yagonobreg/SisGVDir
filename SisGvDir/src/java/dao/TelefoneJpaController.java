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
import modelo.Telefone;
import modelo.Vendedor;

/**
 *
 * @author Yago
 */
public class TelefoneJpaController implements Serializable {

    public TelefoneJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Telefone telefone) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Vendedor vendedor = telefone.getVendedor();
            if (vendedor != null) {
                vendedor = em.getReference(vendedor.getClass(), vendedor.getId());
                telefone.setVendedor(vendedor);
            }
            em.persist(telefone);
            if (vendedor != null) {
                vendedor.getTelefones().add(telefone);
                vendedor = em.merge(vendedor);
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Telefone telefone) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Telefone persistentTelefone = em.find(Telefone.class, telefone.getId());
            Vendedor vendedorOld = persistentTelefone.getVendedor();
            Vendedor vendedorNew = telefone.getVendedor();
            if (vendedorNew != null) {
                vendedorNew = em.getReference(vendedorNew.getClass(), vendedorNew.getId());
                telefone.setVendedor(vendedorNew);
            }
            telefone = em.merge(telefone);
            if (vendedorOld != null && !vendedorOld.equals(vendedorNew)) {
                vendedorOld.getTelefones().remove(telefone);
                vendedorOld = em.merge(vendedorOld);
            }
            if (vendedorNew != null && !vendedorNew.equals(vendedorOld)) {
                vendedorNew.getTelefones().add(telefone);
                vendedorNew = em.merge(vendedorNew);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Long id = telefone.getId();
                if (findTelefone(id) == null) {
                    throw new NonexistentEntityException("The telefone with id " + id + " no longer exists.");
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
            Telefone telefone;
            try {
                telefone = em.getReference(Telefone.class, id);
                telefone.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The telefone with id " + id + " no longer exists.", enfe);
            }
            Vendedor vendedor = telefone.getVendedor();
            if (vendedor != null) {
                vendedor.getTelefones().remove(telefone);
                vendedor = em.merge(vendedor);
            }
            em.remove(telefone);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Telefone> findTelefoneEntities() {
        return findTelefoneEntities(true, -1, -1);
    }

    public List<Telefone> findTelefoneEntities(int maxResults, int firstResult) {
        return findTelefoneEntities(false, maxResults, firstResult);
    }

    private List<Telefone> findTelefoneEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Telefone.class));
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

    public Telefone findTelefone(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Telefone.class, id);
        } finally {
            em.close();
        }
    }

    public int getTelefoneCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Telefone> rt = cq.from(Telefone.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
