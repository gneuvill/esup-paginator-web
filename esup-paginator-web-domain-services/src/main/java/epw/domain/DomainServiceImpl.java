/**
 * ESUP-Portail Blank Application - Copyright (c) 2010 ESUP-Portail consortium.
 */
package epw.domain;

import com.mysema.query.jpa.impl.JPAQuery;
import epw.dao.PersonDao;
import epw.dao.utils.PaginatorFactory;
import epw.model.Person;
import epw.utils.Order;
import fj.F;
import fj.F2;
import fj.P2;
import fj.data.Option;
import fj.data.Stream;
import org.esupportail.commons.services.logging.Logger;
import org.esupportail.commons.services.logging.LoggerImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static epw.model.Address.address;
import static epw.model.Person.person;
import static fj.Function.uncurryF2;
import static fj.P.p;
import static fj.P2.split_;
import static fj.Show.intShow;
import static fj.data.Stream.cons;
import static fj.data.Stream.range;

public class DomainServiceImpl implements DomainService {

  @SuppressWarnings("unused")
  private final Logger logger = new LoggerImpl(this.getClass());

  private final TransactionTemplate txTemplate;

  private final PersonDao personDao;

  private final PaginatorFactory paginatorFactory;

  private DomainServiceImpl(
      PlatformTransactionManager transactionManager,
      PersonDao personDao,
      PaginatorFactory pf) {
    this.txTemplate = new TransactionTemplate(transactionManager);
    this.personDao = personDao;
    this.paginatorFactory = pf;
  }

  public static DomainService domainService(
      PlatformTransactionManager transactionManager,
      PersonDao personDao,
      PaginatorFactory pf) {
    return new DomainServiceImpl(transactionManager, personDao, pf);
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

  @Override
  public final Person savePerson(final Person person) {
    return txTemplate.execute(new TransactionCallback<Person>() {
      public Person doInTransaction(TransactionStatus status) {
        return personDao.savePerson.f(person);
      }
    });
  }

  @Override
  public final F<Person, Person> savePerson_() {
    return new F<Person, Person>() {
      public Person f(Person person) {
        return savePerson(person);
      }
    };
  }

  @Override
  public final Stream<Person> generatePersons() {
    final F<Integer, String> firstname = uncurryF2(toStringAndConcat).flip().f("toto-");
    final F<Integer, String> lastname = uncurryF2(toStringAndConcat).flip().f("tutu-");

    return range(0, 1000000).zipIndex().map(split_(firstname, lastname).andThen(person.tuple()));
  }

  @Override
  public final P2<Long, Stream<Person>> sliceOfPersons(
      Long first, Long pageSize, String sortField, Order order, Map<String, String> filters) {
    final P2<Long, Stream<Person>> result = paginatorFactory.userPaginator()
        .lazySliceOf(first, pageSize, sortField, order, filters, Option.<F<JPAQuery, JPAQuery>>none())
        .map2(expensiveComputation);
    return p(result._1() + 1, cons(person("titi", "titi", address("tata")), p(result._2())));
  }

}
