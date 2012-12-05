package epw.web.utils;

import epw.utils.Order;
import fj.*;
import fj.control.parallel.Actor;
import fj.control.parallel.Strategy;
import fj.data.Stream;
import org.primefaces.model.SortOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static epw.utils.Conversions.pfOrderToOrder;
import static fj.Function.curry;
import static fj.P.p;
import static fj.control.parallel.Actor.queueActor;
import static fj.control.parallel.Promise.promise;
import static fj.data.Stream.nil;

public final class LazyDataModel<T> extends org.primefaces.model.LazyDataModel<T> {

    static final int PROCS = Runtime.getRuntime().availableProcessors();

    static final int THREADS = PROCS == 1 ? 1 : PROCS * 4;

    final ExecutorService pool = Executors.newFixedThreadPool(THREADS);

    final Strategy<Unit> strategy = Strategy.executorStrategy(pool);

    final F2<String, T, Boolean> findByRowKey;

    final F5<Integer, String, Order, Map<String, String>, Integer, P2<Long, Stream<T>>> getData;

    final P3<Integer, Long, Stream<T>> empty = p(0, 0L, Stream.<T>nil());

    private P3<Integer, Long, Stream<T>> formerPage = empty;

    private P3<Integer, Long, Stream<T>> nextPage = empty;

    final Actor<P3<Integer, Long, Stream<T>>> fpActor = queueActor(strategy, new Effect<P3<Integer, Long, Stream<T>>>() {
        public void e(P3<Integer, Long, Stream<T>> p3) {
            formerPage = p(p3._1(), p3._2(), p3._3());
        }
    });

    final Actor<P3<Integer, Long, Stream<T>>> npActor = queueActor(strategy, new Effect<P3<Integer, Long, Stream<T>>>() {
        public void e(P3<Integer, Long, Stream<T>> p3) {
            nextPage = p(p3._1(), p3._2(), p3._3());
        }
    });

    private Stream<T> centralPage = nil();

    private LazyDataModel(
            F5<Integer, Integer, String, Order, Map<String, String>, P2<Long, Stream<T>>> getData,
            F2<String, T, Boolean> findByRowKey) {
        this.getData = flipF5.f(getData);
        this.findByRowKey = findByRowKey;
    }

    public static <TT> LazyDataModel<TT> lazyDataModel(
            F5<Integer, Integer, String, Order, Map<String, String>, P2<Long, Stream<TT>>> getData,
            F2<String, TT, Boolean> findByRowKey) {
        return new LazyDataModel<>(getData, findByRowKey);
    }

    @Override
    public T getRowData(final String rowKey) {
        return centralPage.find(findByRowKey.f(rowKey)).orSome((T) null);
    }

    @Override
    public List<T> load(final int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
        // requête sans l'offset (first)
        final F<Integer, P1<P2<Long, Stream<T>>>> lazyGetData =
                curry(getData).f(pageSize).f(sortField).f(pfOrderToOrder(sortOrder)).f(filters).lazy();
        // requête pour la page précédente
        final P1<P3<Integer, Long, Stream<T>>> formerData =
                first > 0 ? lazyGetData.f(first - pageSize).map(consP2(first - pageSize)) : p(empty);
        // requête pour la page suivante
        final P1<P3<Integer, Long, Stream<T>>> nextData =
                first == 0 || first + pageSize < getRowCount() ?
                        lazyGetData.f(first + pageSize).map(consP2(first + pageSize)) : p(empty);

        // préchargement en arrière plan de la page précédente
        promise(strategy, formerData).to(fpActor);
        // préchargement en arrière plan de la page suivante
        promise(strategy, nextData).to(npActor);

        // le 2-tuple de résultat
        final P2<Long, Stream<T>> centralData =
                nextPage._1() == first && nextPage._3().isNotEmpty() ? p(nextPage._2(), nextPage._3()) :
                        formerPage._1() == first && formerPage._3().isNotEmpty() ? p(formerPage._2(), formerPage._3()) :
                                lazyGetData.f(first)._1();

        // le nombre total d'enregistrements
        setRowCount(centralData._1().intValue());
        // les données
        centralPage = centralData._2();
        // retour en collection jaja
        return new ArrayList<>(centralPage.toCollection());
    }

    public List<T> getCentralPage() {
        return new ArrayList<>(centralPage.toCollection());
    }

    final F<F5<Integer, Integer, String, Order, Map<String, String>, P2<Long, Stream<T>>>,
            F5<Integer, String, Order, Map<String, String>, Integer, P2<Long, Stream<T>>>> flipF5 =
            new F<F5<Integer, Integer, String, Order, Map<String, String>, P2<Long, Stream<T>>>,
                    F5<Integer, String, Order, Map<String, String>, Integer, P2<Long, Stream<T>>>>() {
                @Override
                public F5<Integer, String, Order, Map<String, String>, Integer, P2<Long, Stream<T>>> f(
                        final F5<Integer, Integer, String, Order, Map<String, String>, P2<Long, Stream<T>>> ff) {
                    return new F5<Integer, String, Order, Map<String, String>, Integer, P2<Long, Stream<T>>>() {
                        @Override
                        public P2<Long, Stream<T>>
                        f(Integer pageSize, String sortField, Order order, Map<String, String> filters, Integer first) {
                            return ff.f(first, pageSize, sortField, order, filters);
                        }
                    };
                }
            };

    final F<P2<Long, Stream<T>>, P3<Integer, Long, Stream<T>>> consP2(final Integer i) {
        return new F<P2<Long, Stream<T>>, P3<Integer, Long, Stream<T>>>() {
            public P3<Integer, Long, Stream<T>> f(P2<Long, Stream<T>> p2) {
                return p(i, p2._1(), p2._2());
            }
        };
    }
}