package com.github.vlsergey.springdata.entitysecurity;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

public interface Condition<T> extends Specification<T> {

	/**
	 * Throw an error if operation is not allowed for specified entity
	 */
	void checkEntityDelete(@lombok.NonNull @org.springframework.lang.NonNull T entity);

	/**
	 * Throw an error if operation is not allowed for specified entity
	 */
	void checkEntityInsert(@lombok.NonNull @org.springframework.lang.NonNull T entity);

	/**
	 * Throw an error if operation is not allowed for specified entity
	 */
	void checkEntityUpdate(@lombok.NonNull @org.springframework.lang.NonNull T entity);

	default boolean isAlwaysFalse() {
		return false;
	}

	default boolean isAlwaysTrue() {
		return false;
	}

	@Nullable
	Predicate toPredicate(Root<T> root, CriteriaDelete<?> query, CriteriaBuilder cb);

	@Nullable
	Predicate toPredicate(Root<T> root, CriteriaUpdate<?> query, CriteriaBuilder cb);

}
