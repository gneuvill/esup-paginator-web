package epw.dao.utils;

import com.mysema.query.jpa.impl.JPAQuery;
import epw.domain.beans.Person;
import fj.P1;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class PaginatorFactory {

    @PersistenceContext
    private EntityManager ent;

    private final Paginator<JPAQuery, Person> userPag;

    private PaginatorFactory() {
        userPag = new Paginator<JPAQuery, Person>(new P1<EntityManager>() {
                     public EntityManager _1() {
                         return ent;
                     }}) {};
    }

    public static PaginatorFactory pagFact() {
        return new PaginatorFactory();
    }

    public Paginator<JPAQuery, Person> userPaginator() {
        return userPag;
    }


}


