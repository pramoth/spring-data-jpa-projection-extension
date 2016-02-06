package th.co.geniustree.jpa.spring.data.projection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.io.Serializable;

/**
 *
 * @author pramoth
 * @param <T> Entity type
 * @param <ID> PK
 */
@NoRepositoryBean
public interface ProjectionRepository <T, ID extends Serializable>  extends PagingAndSortingRepository<T, ID>,JpaRepository<T, ID>{
    public <C,X> Page<C> findAll(Specification<T> spec, Class<C> clazz,Selections<T,C,X> selectionCallBack,Pageable pageable);
}
