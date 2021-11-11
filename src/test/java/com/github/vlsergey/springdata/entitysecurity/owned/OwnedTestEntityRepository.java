package com.github.vlsergey.springdata.entitysecurity.owned;

import java.util.List;
import java.util.UUID;

import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.github.vlsergey.springdata.entitysecurity.ConditionWithQuerydsl;
import com.github.vlsergey.springdata.entitysecurity.QueryType;
import com.github.vlsergey.springdata.entitysecurity.SecuredWith;
import com.github.vlsergey.springdata.entitysecurity.SecurityMixinWithQuerydsl;
import com.github.vlsergey.springdata.entitysecurity.StandardConditions;
import com.google.common.base.Objects;

import lombok.NonNull;

@SecuredWith(OwnedTestEntityRepository.OwnedTestEntitySecurityMixin.class)
public interface OwnedTestEntityRepository
		extends JpaRepository<OwnedTestEntity, UUID>, QuerydslPredicateExecutor<OwnedTestEntity> {

	List<OwnedTestEntity> findByValue(int value);

	class OwnedTestEntitySecurityMixin
			implements SecurityMixinWithQuerydsl<OwnedTestEntity, OwnedTestEntityRepository> {

		@Override
		public ConditionWithQuerydsl<OwnedTestEntity, OwnedTestEntityRepository> buildCondition() {
			final String login = SecurityContextHolder.getContext().getAuthentication().getName();

			if (login.equals("root")) {
				return StandardConditions.alwaysAllowConditionWithQuerydsl();
			}
			if (login == null || login.isEmpty()) {
				return StandardConditions.denyWithQuerydsl(() -> new RuntimeException("No rights exception"));
			}

			return new ConditionWithQuerydsl<OwnedTestEntity, OwnedTestEntityRepository>() {

				@Override
				public com.querydsl.core.types.@NonNull Predicate asPredicate() {
					return null;
				};

				@Override
				public void checkEntity(@NonNull OwnedTestEntityRepository repository, @NonNull OwnedTestEntity entity,
						@NonNull QueryType queryType) {
					if (!Objects.equal(entity.getOwner(), login)) {
						throw new AccessDeniedException("No rights exception");
					}
				}

				@Override
				public Object getCurrentUserSecurityCheckCacheKey() {
					return login;
				}

				@Override
				public Object getEntitySecurityCheckCacheKey(OwnedTestEntity entity) {
					return entity.getOwner();
				}

				@Override
				public Predicate toPredicate(@NonNull Root<OwnedTestEntity> root, @NonNull CommonAbstractCriteria cac,
						@NonNull CriteriaBuilder cb, QueryType queryType) {

					return cb.equal(root.get("owner"), login);
				}

			};
		}

		@Override
		public void onForbiddenOperation(OwnedTestEntity entity, QueryType queryType) {
			throw new AccessDeniedException("access denied");
		}

	}

}
