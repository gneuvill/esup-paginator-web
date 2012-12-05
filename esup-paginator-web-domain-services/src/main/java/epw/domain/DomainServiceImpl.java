/**
 * ESUP-Portail Blank Application - Copyright (c) 2010 ESUP-Portail consortium.
 */
package epw.domain;

import com.mysema.query.jpa.impl.JPAQuery;
import epw.dao.PersonDao;
import epw.dao.utils.PaginatorFactory;
import epw.domain.beans.Person;
import epw.utils.Order;
import fj.*;
import fj.data.List;
import fj.data.Option;
import fj.data.Stream;
import org.esupportail.commons.services.logging.Logger;
import org.esupportail.commons.services.logging.LoggerImpl;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static epw.domain.beans.Person.person;
import static fj.Function.uncurryF2;
import static fj.P2.split_;
import static fj.Show.intShow;
import static fj.Unit.unit;
import static fj.data.List.list;
import static fj.data.List.sequence_;
import static fj.data.Stream.range;

public class DomainServiceImpl implements DomainService {

	@SuppressWarnings("unused")
	private final Logger logger = new LoggerImpl(this.getClass());

    private final PersonDao personDao;

    private final PaginatorFactory paginatorFactory;

    private DomainServiceImpl(PersonDao personDao, PaginatorFactory pf) {
        this.personDao = personDao;
        this.paginatorFactory = pf;
    }

    public static DomainService domainService(PersonDao personDao, PaginatorFactory pf) {
        return new DomainServiceImpl(personDao, pf);
    }

    final F2<String, String, String> concat = new F2<String, String, String>() {
        public String f(String s1, String s2) {
            return s1 + s2;
        }
    };

    final F<Integer, F<String, String>> toStringAndConcat = intShow.showS_().andThen(concat.flip().curry());

    final F<Stream<Person>, Stream<Person>> expensiveComputation = new F<Stream<Person>, Stream<Person>>() {
        public Stream<Person> f(Stream<Person> persons) {
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return persons;
        }
    };

    final F<Person, Unit> savePerson = new F<Person, Unit>() {
        public Unit f(Person person) {
            return savePerson(person);
        }
    };

    @Transactional(propagation = Propagation.REQUIRED)
    public final Unit savePerson(Person person) {
        return personDao.savePerson.e().f(person);
    }

    public final Unit generatePersons(Effect<Person> callBack) {
        final F<Integer, String> firstname = uncurryF2(toStringAndConcat).flip().f("toto-");
        final F<Integer, String> lastname = uncurryF2(toStringAndConcat).flip().f("tutu-");

        final F<Person, Unit> personEffect =
                sequence_(list(savePerson, callBack.e())).andThen(Function.<List<Unit>, Unit>constant(unit()));

        range(0, 10000).zipIndex().map(
                split_(firstname, lastname).andThen(person.tuple())).foreach(personEffect);

        return unit();
    }

    public final P2<Long, Stream<Person>> sliceOfPersons(
            Long first, Long pageSize, String sortField, Order order, Map<String, String> filters) {
        return paginatorFactory.userPaginator().lazySliceOf(
                first, pageSize, sortField, order, filters, Option.<F<JPAQuery,JPAQuery>>none()).map2(expensiveComputation);
    }

}
