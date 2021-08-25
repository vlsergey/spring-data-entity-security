package com.github.vlsergey.springdata.entitysecurity;

import javax.annotation.concurrent.NotThreadSafe;

import com.querydsl.core.types.Predicate;

import lombok.NonNull;

@NotThreadSafe
public interface ConditionWithQuerydsl<T> extends Condition<T> {

	/**
	 * @return security filter as Querydsl Predicate. Example:
	 *         <tt>QMyEntity.project.allowedUsers.username.eq( SecurityContext.getCurrent().getAuthentication().getName() )</tt>
	 */
	@NonNull
	Predicate asPredicate();

}
