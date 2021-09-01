package com.github.vlsergey.springdata.entitysecurity;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.QuerydslJpaPredicateExecutor;
import org.springframework.data.querydsl.EntityPathResolver;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPQLQuery;

import lombok.NonNull;

public class SecuredQuerydslJpaPredicateExecutor<T> extends QuerydslJpaPredicateExecutor<T> {

	private final @NonNull SecurityMixinWithQuerydsl<T, ?> securityMixin;

	public SecuredQuerydslJpaPredicateExecutor(final @NonNull JpaEntityInformation<T, ?> entityInformation,
			final @NonNull EntityManager entityManager, final @NonNull EntityPathResolver resolver,
			final @NonNull CrudMethodMetadata metadata, final @NonNull SecurityMixinWithQuerydsl<T, ?> securityMixin) {
		super(entityInformation, entityManager, resolver, metadata);
		this.securityMixin = securityMixin;
	}

	@Override
	protected JPQLQuery<?> createCountQuery(Predicate... predicate) {
		final ConditionWithQuerydsl<T, ?> condition = securityMixin.buildCondition();
		if (condition.isAlwaysTrue()) {
			return super.createCountQuery(predicate);
		}

		// TODO: can it be optimized to return 0 if always false?

		JPQLQuery<?> query = super.createCountQuery(predicate);
		query.where(condition.asPredicate());
		return query;
	}

	@Override
	protected JPQLQuery<?> createQuery(Predicate... predicate) {
		final ConditionWithQuerydsl<T, ?> condition = securityMixin.buildCondition();
		if (condition.isAlwaysTrue()) {
			return super.createCountQuery(predicate);
		}

		// TODO: can it be optimized to return empty list if always false?

		JPQLQuery<?> query = super.createQuery(predicate);
		query.where(condition.asPredicate());
		return query;
	}

}
