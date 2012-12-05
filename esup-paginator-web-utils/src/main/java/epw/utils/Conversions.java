package epw.utils;

import fj.F;
import org.primefaces.model.SortOrder;

import static epw.utils.Order.ASC;
import static epw.utils.Order.DSC;
import static org.primefaces.model.SortOrder.*;

public class Conversions {

    /**
     * Transforme un {@link SortOrder} en {@link Order}
     *
     * @param pfOrder Le {@link SortOrder} à transformer
     * @return Le {@link Order} équivalent
     */
    public static Order pfOrderToOrder(SortOrder pfOrder) {
        switch (pfOrder) {
            case ASCENDING:
                return ASC;
            case DESCENDING:
                return DSC;
            case UNSORTED:
                return ASC;
        }
        return ASC;
    }

    /**
     * {@link Conversions#pfOrderToOrder(org.primefaces.model.SortOrder)} de première classe
     */
    public static final F<SortOrder, Order> pfOrderToOrder = new F<SortOrder, Order>() {
        public Order f(SortOrder sortOrder) {
            return pfOrderToOrder(sortOrder);
        }
    };

    /**
     * Transforme un {@link Order} en {@link SortOrder}
     *
     * @param order Le {@link Order} à transformer
     * @return Le {@link SortOrder} équivalent
     */
    public static SortOrder orderToPfOrder(Order order) {
        switch (order) {
            case ASC:
                return ASCENDING;
            case DSC:
                return DESCENDING;
        }
        return UNSORTED;
    }

    /**
     * {@link Conversions#orderToPfOrder(Order)} de première classe
     */
    public static final F<Order, SortOrder> orderToPfOrder = new F<Order, SortOrder>() {
        public SortOrder f(Order order) {
            return orderToPfOrder(order);
        }
    };

    /**
     * Transforme un {@link com.mysema.query.types.Order} en {@link Order}
     *
     * @param qorder Le {@link com.mysema.query.types.Order} à transformer
     * @return L'{@link Order} équivalent
     */
    public static Order qorderToOrder(com.mysema.query.types.Order qorder) {
        switch (qorder) {
            case ASC:
                return ASC;
            case DESC:
                return DSC;
        }
        return ASC;
    }

    /**
     * {@link Conversions#qorderToOrder(com.mysema.query.types.Order)} de première classe
     */
    public static final F<com.mysema.query.types.Order, Order> qorderToOrder = new F<com.mysema.query.types.Order, Order>() {
        public Order f(com.mysema.query.types.Order qorder) {
            return qorderToOrder(qorder);
        }
    };

    /**
     *  Transforme un {@link Order} en {@link com.mysema.query.types.Order}
     *
     * @param order L'{@link Order} à transformer
     * @return L'{@link com.mysema.query.types.Order} équivalent
     */
    public static com.mysema.query.types.Order orderToQorder(Order order) {
        switch (order) {
            case ASC:
                return com.mysema.query.types.Order.ASC;
            case DSC:
                return com.mysema.query.types.Order.DESC;
        }
        return com.mysema.query.types.Order.ASC;
    }

    /**
     * {@link Conversions#orderToQorder(Order)} de première classe
     */
    public static final F<Order, com.mysema.query.types.Order> orderToQorder = new F<Order, com.mysema.query.types.Order>() {
        public com.mysema.query.types.Order f(Order order) {
            return orderToQorder(order);
        }
    };
}
