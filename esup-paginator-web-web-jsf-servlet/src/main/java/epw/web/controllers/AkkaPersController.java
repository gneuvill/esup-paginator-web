package epw.web.controllers;


import akka.actor.*;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.japi.Creator;
import epw.domain.DomainService;
import epw.model.Person;
import fj.Effect;
import fj.F;
import fj.P1;
import fj.P2;
import fj.data.Stream;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import javax.faces.application.FacesMessage;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static fj.data.Stream.cycle;
import static fj.data.Stream.range;

public final class AkkaPersController {

  private final DomainService domainService;

  static final PushContext pushContext = PushContextFactory.getDefault().getPushContext();

  static final ActorSystem system = ActorSystem.create("myActorSystem");
  static final ExecutionContext context = system.dispatcher();

//  static final ActorRef messageActor = system.actorOf(Props.create(new Creator<Actor>() {
//    public UntypedActor create() throws Exception {
//      return new UntypedActor() {
//        public void onReceive(Object message) throws Exception {
//          pushContext.push("/personSavedNotif", message);
//          //System.out.println("In actor : " + ((FacesMessage) message).getDetail());
//        }
//      };
//    }
//  }));

  static final AtomicInteger count = new AtomicInteger(0);

  static final Creator<Actor> actorCreator = new Creator<Actor>() {
    public UntypedActor create() throws Exception {
      return new UntypedActor() {
        public void onReceive(Object message) throws Exception {
          pushContext.push("/personSavedNotif", message);
          pushContext.push("/nbrPersSavedNotif", new FacesMessage("", Integer.toString(count.incrementAndGet())));
        }
      };
    }
  };
  static final P1<ActorRef> createActor = new P1<ActorRef>() {
    public ActorRef _1() {
      return system.actorOf(Props.create(actorCreator));
    }
  };
  static final Stream<ActorRef> tenThousandActors = range(0, 9999).map(createActor.<Integer>constant());

  final Mapper<Person, FacesMessage> persToMessage = new Mapper<Person, FacesMessage>() {
    public FacesMessage apply(Person person) {
      return new FacesMessage("Saved Person", person.getFirstName());
    }
  };

  final F<Person, Future<Person>> savePers = new F<Person, Future<Person>>() {
    public Future<Person> f(final Person person) {
      return Futures.future(new Callable<Person>() {
        public Person call() throws Exception {
          return domainService.savePerson(person);
        }
      }, context);
    }
  };

  private AkkaPersController(DomainService domainService) {
    this.domainService = domainService;
  }

  public static AkkaPersController akkaPersController(DomainService domainService) {
    return new AkkaPersController(domainService);
  }

  public void generatePersons() {
    domainService.generatePersons()
        .map(savePers.andThen(new F<Future<Person>, Future<FacesMessage>>() {
          public Future<FacesMessage> f(Future<Person> fp) {
            return fp.map(persToMessage, context);
          }
        }))
//        .foreach(new Effect<Future<FacesMessage>>() {
//          public void e(Future<FacesMessage> ff) {
//            pipe(ff, context).to(messageActor);
//          }
//        });
//        .foreach(new Effect<Future<FacesMessage>>() {
//          public void e(Future<FacesMessage> ff) {
//            ff.onSuccess(new OnSuccess<FacesMessage>() {
//              public void onSuccess(FacesMessage message) throws Throwable {
//                pushContext.push("/personSavedNotif", message);
//              }
//            }, context);
//          }
//        })
        .zip(cycle(tenThousandActors))
        .foreach(new Effect<P2<Future<FacesMessage>, ActorRef>>() {
          public void e(P2<Future<FacesMessage>, ActorRef> pair) {
            akka.pattern.Patterns.pipe(pair._1(), context).to(pair._2());
          }
        });

  }

}
