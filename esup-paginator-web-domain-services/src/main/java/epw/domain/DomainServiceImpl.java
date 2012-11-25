/**
 * ESUP-Portail Blank Application - Copyright (c) 2010 ESUP-Portail consortium.
 */
package epw.domain;

import com.mysema.query.jpa.impl.JPAQuery;
import epw.dao.UserDao;
import epw.dao.utils.PaginatorFactory;
import epw.domain.beans.User;
import fj.F;
import fj.F2;
import fj.P2;
import fj.Unit;
import fj.data.Option;
import fj.data.Stream;
import org.esupportail.commons.services.logging.Logger;
import org.esupportail.commons.services.logging.LoggerImpl;
import org.primefaces.model.SortOrder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static epw.domain.beans.User.user;
import static fj.Function.uncurryF2;
import static fj.P2.split_;
import static fj.Show.intShow;
import static fj.Unit.unit;
import static fj.data.Stream.range;

public class DomainServiceImpl implements DomainService {

	@SuppressWarnings("unused")
	private final Logger logger = new LoggerImpl(this.getClass());

    private final UserDao userDao;

    private final PaginatorFactory paginatorFactory;

    private DomainServiceImpl(UserDao userDao, PaginatorFactory pf) {
        this.userDao = userDao;
        this.paginatorFactory = pf;
    }

    public static DomainService domainService(UserDao userDao, PaginatorFactory pf) {
        return new DomainServiceImpl(userDao, pf);
    }

    final F2<String, String, String> concat = new F2<String, String, String>() {
        public String f(String s1, String s2) {
            return s1 + s2;
        }
    };

    final F<Integer, F<String, String>> toStringAndConcat = intShow.showS_().andThen(concat.flip().curry());

    final F<Stream<User>, Stream<User>> expensiveComputation = new F<Stream<User>, Stream<User>>() {
        public Stream<User> f(Stream<User> users) {
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return users;
        }
    };

    @Transactional(propagation = Propagation.REQUIRED)
    public final Unit saveUser(User user) {
        return userDao.save.e().f(user);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public final Unit generateUsers() {
        F<Integer, String> firstname = uncurryF2(toStringAndConcat).flip().f("toto-");
        F<Integer, String> lastname = uncurryF2(toStringAndConcat).flip().f("tutu-");

        range(0, 30000).zipIndex().map(
                split_(firstname, lastname).andThen(user.tuple())).foreach(userDao.save);

        return unit();
    }

    public final P2<Long, Stream<User>> sliceOfUsers(
            Long first, Long pageSize,String sortField, SortOrder sortOrder, Map<String,String> filters) {
        return paginatorFactory.userPaginator().lazySliceOf(
                first, pageSize, sortField, sortOrder, filters, Option.<F<JPAQuery,JPAQuery>>none()).map2(expensiveComputation);
    }

}
