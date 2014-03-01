/**
 * ESUP-Portail Blank Application - Copyright (c) 2010 ESUP-Portail consortium.
 */
package epw.domain;

import epw.model.Person;
import epw.utils.Order;
import fj.F;
import fj.P2;
import fj.data.Stream;

import java.util.Map;

public interface DomainService {

    public Person savePerson(Person person);

    public F<Person, Person> savePerson_();

    public Stream<Person> generatePersons();

    public P2<Long, Stream<Person>> sliceOfPersons(
            Long first, Long pageSize, String sortField, Order order, Map<String, String> filters);

}
