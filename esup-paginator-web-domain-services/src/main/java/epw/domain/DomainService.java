/**
 * ESUP-Portail Blank Application - Copyright (c) 2010 ESUP-Portail consortium.
 */
package epw.domain;

import epw.domain.beans.User;
import fj.P2;
import fj.Unit;
import fj.data.Stream;
import org.primefaces.model.SortOrder;

import java.util.Map;

public interface DomainService {

    public Unit saveUser(User user);

    public Unit generateUsers();

    public P2<Long, Stream<User>> sliceOfUsers(
            Long first, Long pageSize,String sortField, SortOrder sortOrder, Map<String,String> filters);

}
