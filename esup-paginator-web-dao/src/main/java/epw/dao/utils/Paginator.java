package epw.dao.utils;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Ops;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.PredicateOperation;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.PathBuilder;
import com.mysema.util.ReflectionUtils;
import epw.utils.Order;
import fj.*;
import fj.data.Either;
import fj.data.List;
import fj.data.Option;
import fj.data.Stream;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.persistence.EntityManager;
import java.lang.Class;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import static epw.utils.Conversions.orderToQorder;
import static fj.Bottom.error;
import static fj.Function.curry;
import static fj.P.p;
import static fj.P2.tuple;
import static fj.Unit.unit;
import static fj.data.Either.left;
import static fj.data.Either.right;
import static fj.data.List.iterableList;
import static fj.data.Option.fromNull;
import static fj.data.Stream.iterableStream;

/**
 * Une classe utilitaire pour effectuer des requêtes paginées sur une BDD.
 * <p>
 * Elle s'appuie sur la librairie <a href="http://www.querydsl.com/">QueryDSL</a> pour la construction des requêtes.
 * Cette classe doit être instanciée <b>anonymement</b>, c'est-à dire de la manière suivante :
 * </p>
 * <p><code>new Paginator() {} //à noter la présence d'accolades</code></p>
 *
 * et <b>non</b> :
 *
 * <p><code>new Paginator() // Erreur : instanciation anonyme obligatoire !</code></p>
 * </p>
 *
 * @param <Q> Le type (querydsl) de requête à construire ({@link JPAQuery} ou {@link HibernateQuery})
 * @param <T> Le type des entités retournées par les requêtes
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class Paginator<Q extends JPQLQuery, T> {

    private final Class<T> ttype;

    private final Either<P1<EntityManager>, P1<SessionFactory>> dataProvider;

    private final EntityPathBase<T> ent;

    private final PathBuilder<T> tPath;

    private Paginator() {
        ttype = null;
        dataProvider = null;
        ent = null;
        tPath = null;
    }

    /**
     * @param mgr un 1-tuple contenant l' {@link EntityManager} JPA si utilisation conjointe à JPA (donc pour {@link Q} = {@link JPAQuery})
     * ou la {@link Session} Hibernate si utilisation conjointe à Hibernate (donc pour {@link Q} = {@link HibernateQuery})
     */
    public Paginator(P1<?> mgr) {
        final Type genParam = getTType(mgr.getClass().getGenericSuperclass(), 0);
        dataProvider = (Either<P1<EntityManager>, P1<SessionFactory>>)
                ((genParam.equals(SessionFactory.class)) ? right(mgr) : left(mgr));
        ttype = (Class<T>) getTType(Paginator.this.getClass().getGenericSuperclass(), 1);
        ent = new EntityPathBase<T>(ttype, "ent");
        tPath = new PathBuilder<T>(ttype, ent.getMetadata());
    }

    // ################ reflection utilities ##############

    /**
     * Méthode utilitaire pour retrouver la valeur à l'exécution d'un paramètre de type (i.e un générique),
     * et ce en dépit de l'<a href="http://en.wikipedia.org/wiki/Type_erasure">effacement des types</a>
     * en vigueur sur la jvm. Le procédé qu'elle emploie nécessite que la classe dont le type est passé
     * en paramètre soit instanciée anonymement
     * (cf. <a href="http://www.jquantlib.org/index.php/Using_TypeTokens_to_retrieve_generic_parameters">ici</a>
     * et <a href="http://www.artima.com/weblogs/viewpost.jsp?thread=208860">là</a>).
     *
     * @param type le {@link Type} de la classe générique dont on veut identifier le type d'un
     *             des paramètres
     * @param typeIndex index du type recherché dans le tableau de paramètres de type
     * @return le {@link Type} correspondant au paramètre {@link T} 
     */
    private Type getTType(Type type, int typeIndex) {
        // Ne devrait jamais se produire, Paginator étant abstrait et non sous-classable => TODO : supprimer ?
        if (type instanceof Class)
            error("Paginator class and its P1 parameter must be instantiated anonymously !!");
        final ParameterizedType ptype = (ParameterizedType) type;
        return ptype.getActualTypeArguments()[typeIndex];
    }

    /**
     * Une {@link F}onction qui, étant donné le nom d'un attribut de la classe de
     * type {@link T}, retourne son type.
     */
    private final F<String, Class> getType = new F<String, Class>() {
        public Class f(String fieldName) {
            return fromNull(ReflectionUtils.getFieldOrNull(
                    ttype, fieldName)).option(
                    new Object().getClass(),
                    new F<Field, Class>() {
                        public Class f(Field field) {
                            return field.getClass();
                        }});
        }};

    // ########## Filtering ############

    /**
     * Une {@link F}onction qui étant donnée :
     * <p>une Map<String, String> (cad une liste de 2-tuples)
     * associant au nom d'un attribut de la classe de type {@link T}, la valeur
     * saisie dans le champ de filtrage correspondant</p>
     * <p>retourne : </p>
     * <p>une liste de 3-tuples ({@link P3}) associant
     * <ul>
     * <li> le nom d'un attribut de la classe de type {@link T}
     * <li> la valeur saisie dans le champ de filtrage correspondant
     * <li> le type de cet attribut
     * </ul></p>
     */
    private final F<Map<String, String>, List<P3<String, String, Class>>> typedFilters =
            new F<Map<String, String>, List<P3<String, String, Class>>>() {
                public List<P3<String, String, Class>> f(Map<String, String> filters) {
                    return iterableList(filters.keySet()).zip(
                            iterableList(filters.values())).map(
                            new F<P2<String, String>, P3<String, String, Class>>() {
                                public P3<String, String, Class> f(P2<String, String> kv) {
                                    return p(kv._1(), kv._2(), getType.f(kv._1()));}});
                }};

    /**
     * Une fonction d'arité 2 ({@link F2}) prenant en arguments :
     * <ul>
     * <li> une requête querydsl ({@link Q})
     * <li> une liste de 3-tuples
     * </ul>
     * et retournant la requête {@link Q} augmentée des
     * clauses <code>where</code> correspondant aux filtres
     * décrits par la liste de 3-tuples.
     *
     */
    private final F2<Q, List<P3<String, String, Class>>, Q> filterFunc=
            new F2<Q, List<P3<String, String, Class>>, Q>() {
                public Q f(Q q, List<P3<String, String, Class>> filters) {
                    return filters.foldLeft(
                            new F2<Q, P3<String, String, Class>, Q>() {
                                public Q f(Q cq, P3<String, String, Class> fvt) {
                                    return (Q) cq.where(
                                            new PredicateOperation(
                                                    Ops.STARTS_WITH,
                                                    tPath.get(fvt._1(), fvt._3()),
                                                    Expressions.template(String.class, "str({0})",
                                                            Expressions.constant(fvt._2()))));
                                }}, q);
                }};

    /**
     * La composition inversée (cf. {@link F#andThen(F)}) de {@link Paginator#typedFilters}
     * et {@link Paginator#filterFunc}
     */
    private final F<Map<String,String>,F<Q,Q>> filter = typedFilters.andThen(filterFunc.flip().curry());

    // ########## Sorting ############

    /**
     * Application de la clause <code>orderBy</code> à la requête
     * <br />
     */
    private final F3<Q, String, Order, Q> orderBy =
            new F3<Q, String, Order, Q>() {
                public Q f(Q q, String sortField, Order order) {
                    return (Q) q.orderBy(new OrderSpecifier(
                            orderToQorder(order),
                            tPath.get(sortField, getType.f(sortField))));
                }};

    // ################ querying #####################

    /**
     * Requête de base pour la pagination (numéro de page + nombre d'éléments)
     */
    private final F2<Long, Long, Q> slice = new F2<Long, Long, Q>() {
        public Q f(Long offset, Long limit) {
            return (Q) from(ent).offset(offset).limit(limit);
        }};

    /**
     * Requête de base (simple select from)
     */
    private final P1<Q> full = new P1<Q>() { public Q _1() { return from(ent); }};

    /**
     * La requête complète : base + filtres + filtres supplémentaires + ordre
     *
     * @param base
     * @param filters
     * @param customFilter
     * @return Une fonction retournant la requête complète
     */
    private <A> F<A, F<String, F<Order, Q>>> query(F<A, Q> base, Map<String,String> filters, F<Q, Q> customFilter) {
        return base.andThen(filter.f(filters)).andThen(customFilter).andThen(curry(orderBy));
    }

    private Q from(final EntityPath<T>... o) {
        return dataProvider.either(
                new F<EntityManager, Q>() {
                    public Q f(EntityManager entMgr) {
                        return (Q) new JPAQuery(entMgr).from(o);
                    }}.mapP1(),
                new F<SessionFactory, Q>() {
                    public Q f(SessionFactory sessionFactory) {
                        return (Q) new HibernateQuery(sessionFactory.getCurrentSession()).from(o);
                    }}.mapP1())._1();
    }

    // ############################ The public API ##################

    /**
     * Requête de pagination
     *
     *
     * @param offset Où commence-t-on ?
     * @param limit Combien d'éléments ?
     * @param sortField Sur quel champ trie-t-on ?
     * @param order
     *@param filters Sur quels champs filtrer ? Avec quelles valeurs ?
     * @param optCustomfilter Un ou des filtres supplémentaires optionnels   @return Un 2-tuple constitué du nombre d'éléments correspondant à la requête filtrée mais <b>non</b> paginée
     * et de la liste des éléments retournés par la requête complète (paginée, filtrée et ordonnée)
     */
    public final P2<Long, java.util.List<T>> sliceOf(Long offset, Long limit, String sortField,
                                                     Order order, Map<String,String> filters, Option<F<Q, Q>> optCustomfilter) {
        final F<Q, Q> customFilter = optCustomfilter.orSome(Function.<Q>identity());
        return p(
                query(full.constant(), filters, customFilter).f(unit()).f(sortField).f(order).count(),
                query(tuple(slice), filters, customFilter).f(p(offset, limit)).f(sortField).f(order).list(ent));
    }

    /**
     * Comme {@link Paginator#sliceOf(Long, Long, String, epw.utils.Order, java.util.Map, fj.data.Option)} mais retourne
     * les éléments dans un {@link Stream} par commodité
     */
    public final P2<Long, Stream<T>> lazySliceOf(
            Long offset, Long limit,String sortField, Order sortOrder, Map<String,String> filters,
            Option<F<Q, Q>> optCustomfilter) {
        P2<Long, java.util.List<T>> t = sliceOf(offset, limit, sortField, sortOrder, filters, optCustomfilter);
        return p(t._1(), iterableStream(t._2()));
    }

    /**
     * Utilité pour la construction de filtres supplémentaires à fournir à
     * {@link Paginator#sliceOf(Long, Long, String, epw.utils.Order, java.util.Map, fj.data.Option)}
     * @return un {@link PathBuilder} associé à l'entité courante (de type {@link Q})
     */
    public final PathBuilder<T> getTPathBuilder() { return tPath; }
}

