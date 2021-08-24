package io.github.vlsergey.springdata.entitysecurity;

import com.querydsl.core.types.Predicate;

import lombok.NonNull;

public interface ConditionWithQuerydsl<T> extends Condition<T> {

	/**
	 * @return security filter as Querydsl Predicate. Example:
	 *         <tt>QMyEntity.project.allowedUsers.username.eq( SecurityContext.getCurrent().getAuthentication().getName() )</tt>
	 */
	@NonNull
	Predicate asPredicate();

}
