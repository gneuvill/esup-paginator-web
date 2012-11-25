package epw.web.controllers;

import epw.web.utils.LazyDataModel;
import epw.domain.DomainService;
import epw.domain.beans.User;
import fj.*;
import fj.data.Stream;
import org.primefaces.model.SortOrder;

import java.util.Map;

import static epw.web.utils.LazyDataModel.lazyDataModel;

public class UserController {

    private final DomainService domainService;

    private final LazyDataModel<User> ldm = lazyDataModel(
            new F5<Integer, Integer, String, SortOrder, Map<String, String>, P2<Long, Stream<User>>>() {
                public P2<Long, Stream<User>> f(
                        Integer first, Integer pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                    return domainService.sliceOfUsers(new Long(first), new Long(pageSize), sortField, sortOrder, filters);
                }
            },
            new F2<String, User, Boolean>() {
                public Boolean f(String rowKey, User user) {
                    return user.getId().toString().equals(rowKey);
                }
            }
    );

    private UserController(DomainService domainService) {
        this.domainService = domainService;
    }

    public static UserController userController(DomainService domainService) {
        return new UserController(domainService);
    }

    public Unit generateUsers() {
        return domainService.generateUsers();
    }

    public LazyDataModel<User> getLdm() { return ldm; }
}
