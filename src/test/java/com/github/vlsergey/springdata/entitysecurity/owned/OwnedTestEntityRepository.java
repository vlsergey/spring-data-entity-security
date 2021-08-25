package com.github.vlsergey.springdata.entitysecurity.owned;

import java.util.List;
import java.util.UUID;

import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.security.core.context.SecurityContextHolder;

import com.github.vlsergey.springdata.entitysecurity.ConditionWithQuerydsl;
import com.github.vlsergey.springdata.entitysecurity.SecuredWith;
import com.github.vlsergey.springdata.entitysecurity.SecurityMixinWithQuerydsl;
import com.github.vlsergey.springdata.entitysecurity.StandardConditions;

import lombok.NonNull;

@SecuredWith(OwnedTestEntityRepository.OwnedTestEntitySecurityMixin.class)
public interface OwnedTestEntityRepository
		extends JpaRepository<OwnedTestEntity, UUID>, QuerydslPredicateExecutor<OwnedTestEntity> {

	class OwnedTestEntitySecurityMixin implements SecurityMixinWithQuerydsl<OwnedTestEntity> {

		@Override
		public ConditionWithQuerydsl<OwnedTestEntity> buildCondition() {
			final String login = SecurityContextHolder.getContext().getAuthentication().getName();

			if (login.equals("root")) {
				return StandardConditions.allowWithQuerydsl();
			}
			if (login == null || login.isEmpty()) {
				return StandardConditions.denyWithQuerydsl(() -> new RuntimeException("No rights exception"));
			}

			return new ConditionWithQuerydsl<>() {

				private static final long serialVersionUID = 1L;

				@Override
				public com.querydsl.core.types.@NonNull Predicate asPredicate() {
					return null;
				}

				@Override
				public Predicate toPredicate(@NonNull Root<OwnedTestEntity> root, @NonNull CommonAbstractCriteria cac,
						@NonNull CriteriaBuilder cb) {

					return cb.equal(root.get("owner"), login);
				}

				@Override
				public void checkEntityUpdate(@NonNull OwnedTestEntity entity) {
					throw new UnsupportedOperationException("not used in test cases");
				}

				@Override
				public void checkEntityInsert(@NonNull OwnedTestEntity entity) {
					throw new UnsupportedOperationException("not used in test cases");
				}

				@Override
				public void checkEntityDelete(@NonNull OwnedTestEntity entity) {
					throw new UnsupportedOperationException("not used in test cases");
				}
			};
		}
	}

	List<OwnedTestEntity> findByValue(int value);

}
