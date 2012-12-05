package epw.web.controllers;

import epw.domain.DomainService;
import epw.domain.beans.Person;
import epw.utils.Order;
import epw.web.utils.LazyDataModel;
import fj.*;
import fj.control.parallel.Actor;
import fj.control.parallel.Strategy;
import fj.data.Stream;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;

import javax.faces.application.FacesMessage;
import java.util.Map;
import java.util.concurrent.Executors;

import static epw.web.utils.LazyDataModel.lazyDataModel;
import static fj.control.parallel.Actor.actor;
import static fj.control.parallel.Promise.promise;
import static fj.control.parallel.Strategy.obtain;

public class PersonController {

    private final DomainService domainService;

    private final PushContext pushContext = PushContextFactory.getDefault().getPushContext();

    final Strategy<Unit> strategy = Strategy.executorStrategy(Executors.newFixedThreadPool(10));

    final Actor<FacesMessage> fmActor = actor(strategy, new Effect<FacesMessage>() {
        public void e(FacesMessage facesMessage) {
            facesMessage.setSummary("TOTO : ");
        }});

    private final LazyDataModel<Person> ldm = lazyDataModel(
            new F5<Integer, Integer, String, Order, Map<String, String>, P2<Long, Stream<Person>>>() {
                public P2<Long, Stream<Person>> f(
                        Integer first, Integer pageSize, String sortField, Order order, Map<String, String> filters) {
                    return domainService.sliceOfPersons(new Long(first), new Long(pageSize), sortField, order, filters);
                }
            },
            new F2<String, Person, Boolean>() {
                public Boolean f(String rowKey, Person person) {
                    return person.getId().toString().equals(rowKey);
                }
            }
    );

    private PersonController(DomainService domainService) {
        this.domainService = domainService;
    }

    public static PersonController personController(DomainService domainService) {
        return new PersonController(domainService);
    }

    public Unit generateUsers() {
        return domainService.generatePersons(new Effect<Person>() {
            public void e(Person person) {
                promise(strategy, obtain(pushContext.push("/personSavedNotif",
                        new FacesMessage("Saved Person", person.getId().toString())))).to(fmActor);
            }
        });
    }

    public LazyDataModel<Person> getLdm() { return ldm; }
}
