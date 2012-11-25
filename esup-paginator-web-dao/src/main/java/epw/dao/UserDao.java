package epw.dao;

import epw.domain.beans.User;
import fj.Effect;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public final class UserDao {

    @PersistenceContext
    private EntityManager entityManager;

    public final Effect<User> save = new Effect<User>() {
        public void e(User user) {
            entityManager.persist(user);
        }
    };

}
