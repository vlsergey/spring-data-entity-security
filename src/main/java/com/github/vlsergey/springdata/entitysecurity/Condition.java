package com.github.vlsergey.springdata.entitysecurity;

import javax.annotation.concurrent.NotThreadSafe;
import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;

import lombok.NonNull;

@NotThreadSafe
public interface Condition<T, R extends JpaRepository<T, ?>> {

	/**
	 * Throw an error if operation is not allowed for specified entity
	 */
	void checkEntityInsert(@lombok.NonNull @org.springframework.lang.NonNull R repository,
			@lombok.NonNull @org.springframework.lang.NonNull T entity);

	/**
	 * It's safe to return {@literal false} here, but returning {@literal true} will
	 * optimize execution in some cases
	 */
	default boolean isAlwaysFalse() {
		return false;
	}

	/**
	 * It's safe to return {@literal false} here, but returning {@literal true} will
	 * optimize execution in some cases
	 */
	default boolean isAlwaysTrue() {
		return false;
	}

	/**
	 * Creates a WHERE clause for filtering entities. Should not add additional
	 * tables to main query (but may create subqueries via
	 * {@link CommonAbstractCriteria#subquery(Class)}).
	 * 
	 * @param root            must not be {@literal null}.
	 * @param cac             The appropriate {@link CriteriaQuery},
	 *                        {@link CriteriaDelete} or {@link CriteriaUpdate}. Do
	 *                        not cast&call <tt>from</tt> methods on this argument,
	 *                        because it will add table to root query. Create
	 *                        subquery instead.
	 * @param criteriaBuilder must not be {@literal null}.
	 */
	@Nullable
	Predicate toPredicate(final @NonNull Root<T> root, final @NonNull CommonAbstractCriteria cac,
			final @NonNull CriteriaBuilder cb, QueryType queryType);

	default Specification<T> toSpecification(final @NonNull QueryType queryType) {
		return (root, cq, cb) -> toPredicate(root, cq, cb, queryType);
	}
}
