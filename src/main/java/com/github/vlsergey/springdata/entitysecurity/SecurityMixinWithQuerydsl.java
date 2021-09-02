package com.github.vlsergey.springdata.entitysecurity;

import javax.annotation.concurrent.ThreadSafe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

@ThreadSafe
public interface SecurityMixinWithQuerydsl<T, R extends JpaRepository<T, ?> & QuerydslPredicateExecutor<T>>
		extends SecurityMixin<T, R> {

	@Override
	ConditionWithQuerydsl<T, R> buildCondition();

}
