package io.github.vlsergey.springdata.entitysecurity;

import java.util.function.Supplier;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;

import com.querydsl.core.types.dsl.Expressions;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

public class StandardConditions {

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
		public javax.persistence.criteria.Predicate toPredicate(Root<T> root, CriteriaQuery<?> query,
				CriteriaBuilder criteriaBuilder) {
			return null;
		}

		@Override
		public javax.persistence.criteria.Predicate toPredicate(Root<T> root, CriteriaUpdate<?> query,
				CriteriaBuilder criteriaBuilder) {
			return null;
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
		public javax.persistence.criteria.Predicate toPredicate(Root<T> root, CriteriaQuery<?> query,
				CriteriaBuilder cb) {
			return cb.equal(cb.literal(Boolean.TRUE), cb.literal(Boolean.FALSE));
		}

		@Override
		public javax.persistence.criteria.Predicate toPredicate(Root<T> root, CriteriaUpdate<?> query,
				CriteriaBuilder cb) {
			return cb.equal(cb.literal(Boolean.TRUE), cb.literal(Boolean.FALSE));
		}
	}

	private static class AllowWithQuerydslCondition<T> extends AllowCondition<T> implements ConditionWithQuerydsl<T> {

		static final AllowWithQuerydslCondition<Object> INSTANCE = new AllowWithQuerydslCondition<>();

		private static final long serialVersionUID = 1L;

		@Override
		public com.querydsl.core.types.Predicate asPredicate() {
			return Expressions.TRUE.eq(Boolean.TRUE);
		}

	}

	@SuppressWarnings("unchecked")
	public static <T> Condition<T> allow() {
		return (Condition<T>) AllowCondition.INSTANCE;
	}

	public static <T, E extends Throwable> Condition<T> deny(Supplier<E> checkErrorSupplier) {
		return new DenyCondition<>(checkErrorSupplier);
	}

	@SuppressWarnings("unchecked")
	public static <T> ConditionWithQuerydsl<T> allowWithQuerydsl() {
		return (ConditionWithQuerydsl<T>) AllowWithQuerydslCondition.INSTANCE;
	}

}
