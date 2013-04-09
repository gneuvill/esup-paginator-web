package epw.web.controllers;

import epw.domain.DomainService;
import epw.domain.beans.Person;
import epw.utils.Order;
import epw.web.utils.LazyDataModel;
import fj.*;
import fj.control.parallel.Actor;
import fj.control.parallel.Promise;
import fj.data.Stream;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;

import javax.faces.application.FacesMessage;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static epw.utils.ParallelModule.parMod;
import static epw.web.utils.LazyDataModel.lazyDataModel;
import static fj.Show.showS;

public class PersonController {

    private final DomainService domainService;

    private final PushContext pushContext = PushContextFactory.getDefault().getPushContext();

    final Show<Person> personShow = showS(new F<Person, String>() {
        public String f(Person p) {
            return "Person(" + p.getId() + ", " + p.getFirstName() + ", " + p.getLastName() + ")";
        }
    });

    private PersonController(DomainService domainService) {
        this.domainService = domainService;
    }

    public static PersonController personController(DomainService domainService) {
        return new PersonController(domainService);
    }

    final F<FacesMessage, Future<FacesMessage>> pushMessage = new F<FacesMessage, Future<FacesMessage>>() {
        public Future<FacesMessage> f(final FacesMessage facesMessage) {
            return Integer.parseInt(facesMessage.getDetail()) % 100 == 0 ?
                pushContext.push("/personSavedNotif", facesMessage) :
                new FutureTask<>(new Callable<FacesMessage>() {
                  public FacesMessage call() throws Exception {
                    return facesMessage;
                  }
                });
        }
    };

    final F<Person, FacesMessage> persToMessage = new F<Person, FacesMessage>() {
        public FacesMessage f(Person person) {
            return new FacesMessage("Saved Person", person.getId().toString());
        }
    };

    final Actor<Future<FacesMessage>> messageActor = parMod.actor(new Effect<Future<FacesMessage>>() {
        public void e(Future<FacesMessage> future) {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
        }});

    private final LazyDataModel<Person> ldm = lazyDataModel(
            new F5<Integer, Integer, String, Order, Map<String, String>, P2<Long, Stream<Person>>>() {
                public P2<Long, Stream<Person>> f(
                        Integer first, Integer pageSize, String sortField, Order order, Map<String, String> filters) {
                    return domainService.sliceOfPersons((long) first, (long) pageSize, sortField, order, filters);
                }
            },
            new F2<String, Person, Boolean>() {
                public Boolean f(String rowKey, Person person) {
                    return person.getId().toString().equals(rowKey);
                }
            }
    );

    public void generatePersons() {
      domainService.generatePersons()
          .map(parMod.promise(domainService.savePerson_().andThen(persToMessage.andThen(pushMessage))))
          .foreach(new Effect<Promise<Future<FacesMessage>>>() {
            public void e(Promise<Future<FacesMessage>> futurePromise) {
              futurePromise.to(messageActor);
            }
          });
//        parMod.mapM(
//                domainService.generatePersons(),
//                parMod.promise(domainService.savePerson_().andThen(persToMessage.andThen(pushMessage)))
//        ).to(messageActor);
    }

    public LazyDataModel<Person> getLdm() {
        return ldm;
    }
}
