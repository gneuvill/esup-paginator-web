/**
 * ESUP-Portail Blank Application - Copyright (c) 2010 ESUP-Portail consortium.
 */
package epw.domain;

import epw.domain.beans.Person;
import epw.utils.Order;
import fj.Effect;
import fj.P2;
import fj.Unit;
import fj.data.Stream;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public interface DomainService {

    @Transactional(propagation = Propagation.REQUIRED)
    public Unit savePerson(Person person);

    @Transactional(propagation = Propagation.REQUIRED)
    public Unit generatePersons(Effect<Person> callBack);

    @Transactional(propagation = Propagation.REQUIRED)
    public P2<Long, Stream<Person>> sliceOfPersons(
            Long first, Long pageSize, String sortField, Order order, Map<String, String> filters);

}
