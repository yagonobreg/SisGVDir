/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author Yago
 */
public class Emf {
    public static final EntityManagerFactory factory = Persistence.createEntityManagerFactory("SisGvDirPU");
}
