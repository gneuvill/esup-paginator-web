package epw.dao;

import epw.domain.beans.Person;
import fj.Effect;
import fj.data.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public final class PersonDao {

    @PersistenceContext
    private EntityManager entityManager;

    public final Effect<Person> savePerson = new Effect<Person>() {
        public void e(Person person) {
            entityManager.persist(person);
        }
    };

    public final Effect<Stream<Person>> savePersons = new Effect<Stream<Person>>() {
        public void e(Stream<Person> persons) {
            persons.foreach(savePerson);
            entityManager.flush();
        }
    };

}
