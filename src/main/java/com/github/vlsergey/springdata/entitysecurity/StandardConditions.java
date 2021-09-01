package com.github.vlsergey.springdata.entitysecurity;

import java.io.Serializable;
import java.util.function.Supplier;

import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

public class StandardConditions {

	@SuppressWarnings("unchecked")
	public static <T, R extends JpaRepository<T, ? extends Serializable>> Condition<T, R> allow() {
		return (Condition<T, R>) AllowCondition.INSTANCE;
	}

	@SuppressWarnings("unchecked")
	public static <T, R extends JpaRepository<T, ? extends Serializable> & QuerydslPredicateExecutor<T>> //
	ConditionWithQuerydsl<T, R> allowWithQuerydsl() {
		return (ConditionWithQuerydsl<T, R>) AllowWithQuerydslCondition.INSTANCE_WITH_QUERYDSL;
	}

	public static <T, R extends JpaRepository<T, ? extends Serializable>> SecurityMixin<T, R> alwaysAllowSecurityMixin() {
		return new SecurityMixin<T, R>() {
			@Override
			public Condition<T, R> buildCondition() {
				return allow();
			}

			@Override
			public void onForbiddenUpdate(T entity) {
				throw new AssertionError("This method is not supposed to be called because it's always-allow condtion");
			}
		};
	}

	public static <T, R extends JpaRepository<T, ? extends Serializable> & QuerydslPredicateExecutor<T>> SecurityMixinWithQuerydsl<T, R> alwaysAllowSecurityMixinWithQuerydsl() {
		return new SecurityMixinWithQuerydsl<T, R>() {
			@Override
			public ConditionWithQuerydsl<T, R> buildCondition() {
				return allowWithQuerydsl();
			}

			@Override
			public void onForbiddenUpdate(T entity) {
				throw new AssertionError("This method is not supposed to be called because it's always-allow condtion");
			}
		};
	}

	public static <T, R extends JpaRepository<T, ? extends Serializable>, E extends Throwable> Condition<T, R> deny(
			final @NonNull Supplier<E> checkErrorSupplier) {
		return new DenyCondition<>(checkErrorSupplier);
	}

	public static <T, R extends JpaRepository<T, ? extends Serializable> & QuerydslPredicateExecutor<T>, E extends Throwable> //
	ConditionWithQuerydsl<T, R> denyWithQuerydsl(final @NonNull Supplier<E> checkErrorSupplier) {
		return new DenyWithQuerydslCondition<>(checkErrorSupplier);
	}

	private static class AllowCondition<T, R extends JpaRepository<T, ? extends Serializable>>
			implements Condition<T, R> {

		static final AllowCondition<?, ?> INSTANCE = new AllowCondition<>();

		@Override
		public void checkEntityInsert(R repository, T entity) {
			// NO OP
		}

		@Override
		public boolean isAlwaysFalse() {
			return false;
		}

		@Override
		public boolean isAlwaysTrue() {
			return true;
		}

		@Override
		public javax.persistence.criteria.Predicate toPredicate(@NonNull Root<T> root,
				@NonNull CommonAbstractCriteria cac, @NonNull CriteriaBuilder cb, QueryType queryType) {
			return null;
		}

	}

	private static class AllowWithQuerydslCondition<T, R extends JpaRepository<T, ? extends Serializable> & QuerydslPredicateExecutor<T>>
			extends AllowCondition<T, R> implements ConditionWithQuerydsl<T, R> {

		static final AllowWithQuerydslCondition<?, ?> INSTANCE_WITH_QUERYDSL = new AllowWithQuerydslCondition<>();

		@Override
		public com.querydsl.core.types.Predicate asPredicate() {
			return Expressions.TRUE.eq(Boolean.TRUE);
		}

	}

	@AllArgsConstructor
	private static class DenyCondition<T, R extends JpaRepository<T, ? extends Serializable>, E extends Throwable>
			implements Condition<T, R> {

		private final @NonNull Supplier<E> checkErrorSupplier;

		@Override
		@SneakyThrows
		public void checkEntityInsert(@NonNull R repository, @NonNull T entity) {
			throw checkErrorSupplier.get();
		}

		@Override
		public boolean isAlwaysFalse() {
			return true;
		}

		@Override
		public boolean isAlwaysTrue() {
			return false;
		}

		@Override
		public javax.persistence.criteria.Predicate toPredicate(@NonNull Root<T> root,
				@NonNull CommonAbstractCriteria cac, @NonNull CriteriaBuilder cb, QueryType queryType) {
			return cb.equal(cb.literal(Boolean.TRUE), cb.literal(Boolean.FALSE));
		}

	}

	private static class DenyWithQuerydslCondition<T, R extends JpaRepository<T, ? extends Serializable> & QuerydslPredicateExecutor<T>, E extends Throwable>
			extends DenyCondition<T, R, E> implements ConditionWithQuerydsl<T, R> {

		public DenyWithQuerydslCondition(final @NonNull Supplier<E> checkErrorSupplier) {
			super(checkErrorSupplier);
		}

		@Override
		public @NonNull Predicate asPredicate() {
			return Expressions.TRUE.eq(Boolean.FALSE);
		}

	}

}
