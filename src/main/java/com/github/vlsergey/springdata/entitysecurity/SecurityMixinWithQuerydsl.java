package com.github.vlsergey.springdata.entitysecurity;

import java.io.Serializable;

import javax.annotation.concurrent.ThreadSafe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

@ThreadSafe
public interface SecurityMixinWithQuerydsl<T, R extends JpaRepository<T, ? extends Serializable> & QuerydslPredicateExecutor<T>>
		extends SecurityMixin<T, R> {

	@Override
	ConditionWithQuerydsl<T, R> buildCondition();

}
