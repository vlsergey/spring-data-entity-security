package com.github.vlsergey.springdata.entitysecurity;

import javax.annotation.concurrent.ThreadSafe;

import org.springframework.data.jpa.repository.JpaRepository;

@ThreadSafe
public interface SecurityMixin<T, R extends JpaRepository<T, ?>> {

	Condition<T, R> buildCondition();

	/**
	 * This method is called when {@link JpaRepository#save(Object)} method (or
	 * alike) is called with entity that is not returned by condition with
	 * {@link QueryType#UPDATE} query type. The entity is still readable by user,
	 * but he must have no access to updating it.
	 */
	void onForbiddenUpdate(T entity);

}
