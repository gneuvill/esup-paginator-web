package epw.web.controllers;

import epw.domain.DomainService;
import epw.model.Person;
import epw.utils.Order;
import epw.web.utils.LazyDataModel;
import fj.*;
import fj.control.parallel.Actor;
import fj.control.parallel.Promise;
import fj.control.parallel.Strategy;
import fj.data.Stream;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;

import javax.faces.application.FacesMessage;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static epw.utils.ParallelModule.execService;
import static epw.utils.ParallelModule.unitStrategy;
import static epw.web.utils.LazyDataModel.lazyDataModel;
import static fj.control.parallel.Actor.actor;
import static fj.data.Stream.cycle;
import static fj.data.Stream.range;


public final class PersonController {

  private final DomainService domainService;

  private final PushContext pushContext = PushContextFactory.getDefault().getPushContext();

  private PersonController(DomainService domainService) {
    this.domainService = domainService;
  }

  public static PersonController personController(DomainService domainService) {
    return new PersonController(domainService);
  }

  final AtomicInteger count = new AtomicInteger(0);

  final F<Person, FacesMessage> persToMessage = new F<Person, FacesMessage>() {
    public FacesMessage f(Person person) {
      return new FacesMessage("Saved Person", person.getFirstName());
    }
  };

//  final Actor<FacesMessage> messageActor = actor(unitStrategy, new Effect<FacesMessage>() {
//    public void e(FacesMessage message) {
//      pushContext.push("/personSavedNotif", message);
//    }
//  });

  final P1< Actor<FacesMessage>> createActor = new P1<Actor<FacesMessage>>() {
    public Actor<FacesMessage> _1() {
      return actor(unitStrategy, new Effect<FacesMessage>() {
        public void e(FacesMessage message) {
          pushContext.push("/personSavedNotif", message);
          pushContext.push("/nbrPersSavedNotif", new FacesMessage("", Integer.toString(count.incrementAndGet())));
        }
      });
    }
  };

  final Stream<Actor<FacesMessage>> lotsOfActors = range(0, 999).map(createActor.<Integer>constant());

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
    final Strategy<Promise<FacesMessage>> messageStrategy =
        Strategy.<Promise<FacesMessage>>executorStrategy(execService).errorStrategy(new Effect<Error>() {
          public void e(Error error) {
            error.printStackTrace();
          }
        });
    domainService.generatePersons()
        .map(messageStrategy.concurry(domainService.savePerson_().promiseK(unitStrategy).andThen(persToMessage.mapPromise())))
//        .foreach(new Effect<P1<Promise<FacesMessage>>>() {
//          public void e(P1<Promise<FacesMessage>> p) {
//            Promise.join(unitStrategy, p).to(messageActor);
//          }
//        });
        .zip(cycle(lotsOfActors))
        .foreach(new Effect<P2<P1<Promise<FacesMessage>>, Actor<FacesMessage>>>() {
          public void e(P2<P1<Promise<FacesMessage>>, Actor<FacesMessage>> pair) {
            Promise.join(unitStrategy, pair._1()).to(pair._2());
          }
        });
//    domainService.generatePersons()
//        .foreach(new F<Person, Unit>() {
//          public Unit f(Person person) {
//            return unitStrategy.concurry(
//                domainService.savePerson_().promiseK(unitStrategy)
//                    .andThen(persToMessage.andThen(new F<FacesMessage, Unit>() {
//                      public Unit f(FacesMessage message) {
//                        pushContext.push("/personSavedNotif", message);
//                        return unit();
//                      }
//                    }).mapPromise()
//                    .andThen(p(unit()).<Promise<Unit>>constant()))).f(person)._1();
//          }
//        });
//
//
//
//    parMod.parMap(
//        domainService.generatePersons(),
//        domainService.savePerson_().promiseK(unitStrategy)
//            .andThen(persToMessage.mapPromise()
//                .andThen(new F<Promise<FacesMessage>, Unit>() {
//                  public Unit f(Promise<FacesMessage> promise) {
//                    promise.to(messageActor);
//                    return Unit.unit();
//                  }
//                })));
  }

  public LazyDataModel<Person> getLdm() {
    return ldm;
  }
}
