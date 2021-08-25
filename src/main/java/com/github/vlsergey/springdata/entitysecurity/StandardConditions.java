package com.github.vlsergey.springdata.entitysecurity;

import java.util.function.Supplier;

import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

public class StandardConditions {

	@SuppressWarnings("unchecked")
	public static <T> Condition<T> allow() {
		return (Condition<T>) AllowCondition.INSTANCE;
	}

	@SuppressWarnings("unchecked")
	public static <T> ConditionWithQuerydsl<T> allowWithQuerydsl() {
		return (ConditionWithQuerydsl<T>) AllowWithQuerydslCondition.INSTANCE_WITH_QUERYDSL;
	}

	public static <T> SecurityMixin<T> alwaysAllowSecurityMixin() {
		return StandardConditions::allow;
	}

	public static <T> SecurityMixinWithQuerydsl<T> alwaysAllowSecurityMixinWithQuerydsl() {
		return StandardConditions::allowWithQuerydsl;
	}

	public static <T, E extends Throwable> Condition<T> deny(final @NonNull Supplier<E> checkErrorSupplier) {
		return new DenyCondition<>(checkErrorSupplier);
	}

	public static <T, E extends Throwable> ConditionWithQuerydsl<T> denyWithQuerydsl(
			final @NonNull Supplier<E> checkErrorSupplier) {
		return new DenyWithQuerydslCondition<>(checkErrorSupplier);
	}

	private static class AllowCondition<T> implements Condition<T> {

		static final AllowCondition<Object> INSTANCE = new AllowCondition<>();

		private static final long serialVersionUID = 1L;

		@Override
		public void checkEntityDelete(@NonNull T entity) {
			// NO OP
		}

		@Override
		public void checkEntityInsert(@NonNull T entity) {
			// NO OP
		}

		@Override
		public void checkEntityUpdate(@NonNull T entity) {
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
		public javax.persistence.criteria.Predicate toPredicate(Root<T> root, CriteriaDelete<?> query,
				CriteriaBuilder criteriaBuilder) {
			return null;
		}

		@Override
		public javax.persistence.criteria.Predicate toPredicate(Root<T> root, AbstractQuery<?> query,
				CriteriaBuilder criteriaBuilder) {
			return null;
		}

		@Override
		public javax.persistence.criteria.Predicate toPredicate(Root<T> root, CriteriaUpdate<?> query,
				CriteriaBuilder criteriaBuilder) {
			return null;
		}
	}

	private static class AllowWithQuerydslCondition<T> extends AllowCondition<T> implements ConditionWithQuerydsl<T> {

		static final AllowWithQuerydslCondition<Object> INSTANCE_WITH_QUERYDSL = new AllowWithQuerydslCondition<>();

		private static final long serialVersionUID = 1L;

		@Override
		public com.querydsl.core.types.Predicate asPredicate() {
			return Expressions.TRUE.eq(Boolean.TRUE);
		}

	}

	@AllArgsConstructor
	private static class DenyCondition<T, E extends Throwable> implements Condition<T> {

		private static final long serialVersionUID = 1L;

		private final @NonNull Supplier<E> checkErrorSupplier;

		@Override
		@SneakyThrows
		public void checkEntityDelete(@NonNull T entity) {
			throw checkErrorSupplier.get();
		}

		@Override
		@SneakyThrows
		public void checkEntityInsert(@NonNull T entity) {
			throw checkErrorSupplier.get();
		}

		@Override
		@SneakyThrows
		public void checkEntityUpdate(@NonNull T entity) {
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
		public javax.persistence.criteria.Predicate toPredicate(Root<T> root, CriteriaDelete<?> query,
				CriteriaBuilder cb) {
			return cb.equal(cb.literal(Boolean.TRUE), cb.literal(Boolean.FALSE));
		}

		@Override
		public javax.persistence.criteria.Predicate toPredicate(Root<T> root, AbstractQuery<?> query,
				CriteriaBuilder cb) {
			return cb.equal(cb.literal(Boolean.TRUE), cb.literal(Boolean.FALSE));
		}

		@Override
		public javax.persistence.criteria.Predicate toPredicate(Root<T> root, CriteriaUpdate<?> query,
				CriteriaBuilder cb) {
			return cb.equal(cb.literal(Boolean.TRUE), cb.literal(Boolean.FALSE));
		}
	}

	private static class DenyWithQuerydslCondition<T, E extends Throwable> extends DenyCondition<T, E>
			implements ConditionWithQuerydsl<T> {

		private static final long serialVersionUID = 1L;

		public DenyWithQuerydslCondition(final @NonNull Supplier<E> checkErrorSupplier) {
			super(checkErrorSupplier);
		}

		@Override
		public @NonNull Predicate asPredicate() {
			return Expressions.TRUE.eq(Boolean.FALSE);
		}

	}

}
