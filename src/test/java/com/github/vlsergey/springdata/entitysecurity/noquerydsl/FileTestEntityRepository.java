package com.github.vlsergey.springdata.entitysecurity.noquerydsl;

import javax.persistence.criteria.CommonAbstractCriteria;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.context.SecurityContextHolder;

import com.github.vlsergey.springdata.entitysecurity.Condition;
import com.github.vlsergey.springdata.entitysecurity.QueryType;
import com.github.vlsergey.springdata.entitysecurity.SecuredWith;
import com.github.vlsergey.springdata.entitysecurity.SecurityMixin;
import com.github.vlsergey.springdata.entitysecurity.StandardConditions;
import com.github.vlsergey.springdata.entitysecurity.noquerydsl.FileTestEntityRepository.FileTestEntitySecurityMixin;

import lombok.NonNull;

@SecuredWith(FileTestEntitySecurityMixin.class)
public interface FileTestEntityRepository extends JpaRepository<FileTestEntity, String> {

	class FileTestEntitySecurityMixin implements SecurityMixin<FileTestEntity, FileTestEntityRepository> {
		@Override
		public Condition<FileTestEntity, FileTestEntityRepository> buildCondition() {
			final String login = SecurityContextHolder.getContext().getAuthentication().getName();

			if (login.equals("root")) {
				return StandardConditions.alwaysAllowCondition();
			}
			if (login == null || login.isEmpty()) {
				return StandardConditions.deny(() -> new RuntimeException("No rights exception"));
			}

			return new Condition<FileTestEntity, FileTestEntityRepository>() {

				@Override
				public void checkEntity(@NonNull FileTestEntityRepository repository, @NonNull FileTestEntity entity,
						@NonNull QueryType queryType) {
					throw new UnsupportedOperationException("not used in test cases");
				}

				@Override
				public Predicate toPredicate(@NonNull Root<FileTestEntity> root, @NonNull CommonAbstractCriteria query,
						@NonNull CriteriaBuilder cb, QueryType queryType) {

					final Subquery<Integer> subquery = query.subquery(Integer.class);
					subquery.select(cb.literal(1));

					final Predicate allowedByOwner = cb.equal(cb.substring(root.get("permissions"), 0, 1), "r");
					final Predicate allowedByGroup = cb.equal(cb.substring(root.get("permissions"), 3, 4), "r");
					final Predicate allowedByOther = cb.equal(cb.substring(root.get("permissions"), 6, 7), "r");

					final Root<UserTestEntity> currentUser = subquery.from(UserTestEntity.class);

					final Predicate currentIsOwner = cb.equal(root.get("ownerUser"), currentUser);
					final Predicate currentUserInOwnerGroup = root.get("ownerGroup").in(currentUser.join("groups"));

					subquery.where(cb.and(cb.equal(currentUser.get("login"), login)),
							cb.or(cb.and(allowedByOwner, currentIsOwner),
									cb.and(allowedByGroup, currentUserInOwnerGroup), allowedByOther));

					return cb.exists(subquery);
				}

			};
		}

		@Override
		public void onForbiddenOperation(FileTestEntity entity, QueryType queryType) {
			throw new UnsupportedOperationException("not used in test cases");
		}

	}

}
