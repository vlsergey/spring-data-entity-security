package com.github.vlsergey.springdata.entitysecurity;

import javax.annotation.concurrent.NotThreadSafe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import com.querydsl.core.types.Predicate;

import lombok.NonNull;

@NotThreadSafe
public interface ConditionWithQuerydsl<T, R extends JpaRepository<T, ?> & QuerydslPredicateExecutor<T>>
		extends Condition<T, R> {

	/**
	 * @return security filter as Querydsl Predicate. Example:
	 *         <tt>QMyEntity.project.allowedUsers.username.eq( SecurityContext.getCurrent().getAuthentication().getName() )</tt>
	 */
	@NonNull
	Predicate asPredicate();

}
