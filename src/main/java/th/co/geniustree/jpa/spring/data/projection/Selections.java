package th.co.geniustree.jpa.spring.data.projection;

import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

/**
 * @author pramoth
 */
public interface Selections<T, C, X> {

    Expression[] select(Root<T> root, AbstractQuery<C> query, CriteriaBuilder cb);

    Join<T, X> join(Root<T> root, CriteriaBuilder cb);
}
