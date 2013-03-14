package epw.dao;

import epw.domain.beans.Person;
import fj.F;
import fj.data.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public final class PersonDao {

    @PersistenceContext
    private EntityManager entityManager;

    public final F<Person, Person> savePerson = new F<Person, Person>() {
        public Person f(Person person) {
            entityManager.persist(person);
            return person;
        }
    };

    public final F<Stream<Person>, Stream<Person>> savePersons = new F<Stream<Person>, Stream<Person>>() {
        public Stream<Person> f(Stream<Person> persons) {
            Stream<Person> s = persons.map(savePerson);
            entityManager.flush();
            return s;
        }
    };

}
