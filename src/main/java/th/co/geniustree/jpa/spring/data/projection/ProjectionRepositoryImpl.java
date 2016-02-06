package th.co.geniustree.jpa.spring.data.projection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static org.springframework.data.jpa.repository.query.QueryUtils.toOrders;

/**
 *
 * @author pramoth
 * @param <T> Entity type
 * @param <ID> PK
 */
public class ProjectionRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements ProjectionRepository<T, ID> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectionRepositoryImpl.class);
    private final EntityManager em;
    private final JpaEntityInformation<T, ?> entityInformation;

    public ProjectionRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager em) {
        super(entityInformation, em);
        this.em = em;
        this.entityInformation = entityInformation;
    }

    public ProjectionRepositoryImpl(Class<T> domainClass, EntityManager em) {
        super(domainClass, em);
        this.em = em;
        this.entityInformation = JpaEntityInformationSupport.getEntityInformation(domainClass, em);
    }

    @Override
    public <C, X> Page<C> findAll(Specification<T> spec, Class<C> clazz, Selections<T, C, X> selection, Pageable pageable) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<C> query = builder.createQuery(clazz);
        Root<T> root = query.from(getDomainClass());
        Predicate predicate = spec.toPredicate(root, query, builder);

        if (predicate != null) {
            query.where(predicate);
        }
        query.select(builder.construct(clazz, selection.select(root, query, builder)));
        List<Expression<?>> groupBy = query.getGroupList();
        Predicate having = query.getGroupRestriction();
        if (pageable.getSort() != null) {
            query.orderBy(toOrders(pageable.getSort(), root, builder));
        }
        TypedQuery<C> typeQuery = em.createQuery(query);
        return pageable == null ? new PageImpl<>(typeQuery.getResultList()) : readPage(typeQuery, pageable, spec, selection, groupBy, having);
    }

    private <C, X> Page<C> readPage(TypedQuery<C> query, Pageable pageable, Specification<T> spec, Selections<T, C, X> selection, List<Expression<?>> groupBy, Predicate having) {

        query.setFirstResult(pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        Long total = executeCountQuery(getCountQuery(spec, selection, groupBy, having));
        List<C> content = total > pageable.getOffset() ? query.getResultList() : Collections.<C>emptyList();
        return new PageImpl<>(content, pageable, total);
    }

    private static Long executeCountQuery(TypedQuery<Long> query) {

        Assert.notNull(query);

        List<Long> totals = query.getResultList();
        Long total = totals.stream()
                .filter(e -> e != null)
                .reduce(0L, (accumulator, e) -> accumulator + e);
        return total;
    }

    private <C, X> TypedQuery<Long> getCountQuery(Specification<T> spec, Selections<T, C, X> selection, List<Expression<?>> groupBy, Predicate having) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        Root<T> root = countQuery.from(getDomainClass());

        Subquery<?> subquery = createCountSubQuery(root,countQuery, selection, builder, spec, groupBy, having);
        Path<?> path = root.get(entityInformation.getIdAttribute());
        countQuery.select(builder.countDistinct(path)).where(builder.exists(subquery));
        return em.createQuery(countQuery);
    }

    private <C, X> Subquery<?> createCountSubQuery(Root<T> root,CriteriaQuery<Long> countQuery,Selections<T, C, X> selection, CriteriaBuilder builder, Specification<T> spec, List<Expression<?>> groupBy, Predicate having) {
        
        Subquery<?> subquery = countQuery.subquery(entityInformation.getIdType());
        Root subRoot = subquery.from(entityInformation.getJavaType());
        Expression[] select = selection.select(subRoot, (AbstractQuery<C>) subquery, builder);
        subquery.select(select[0]);
        selection.join(subRoot, builder);
        Predicate predicate = builder.equal(subRoot.get(entityInformation.getIdAttribute()), root.get(entityInformation.getIdAttribute()));
        if (predicate != null) {
            subquery.where(predicate);
        }
        if (groupBy != null) {
            subquery.groupBy(groupBy);
        }
        if (having != null) {
            subquery.having(having);
        }
        return subquery;
    }

}
