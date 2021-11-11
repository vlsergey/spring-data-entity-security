package com.github.vlsergey.springdata.entitysecurity;

import javax.annotation.concurrent.ThreadSafe;

import org.springframework.data.jpa.repository.JpaRepository;

@ThreadSafe
public interface SecurityMixin<T, R extends JpaRepository<T, ?>> {

	Condition<T, R> buildCondition();

	default void onForbiddenDelete(T entity) {
		onForbiddenOperation(entity, QueryType.DELETE);
	}

	/**
	 * This method is called when {@link JpaRepository#delete(Object)} or
	 * {@link JpaRepository#save(Object)} methods (or alike) are called with entity
	 * that is not returned by condition with
	 * {@link QueryType#DELETE}/{@link QueryType#UPDATE}/{@link QueryType#INSERT}
	 * query type. The entity may be still readable by user, but he must have no
	 * access to delete it.
	 */
	void onForbiddenOperation(T entity, QueryType queryType);

	default void onForbiddenUpdate(T entity) {
		onForbiddenOperation(entity, QueryType.UPDATE);
	}

}
