package com.github.vlsergey.springdata.entitysecurity.noquerydsl;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.context.SecurityContextHolder;

import com.github.vlsergey.springdata.entitysecurity.Condition;
import com.github.vlsergey.springdata.entitysecurity.SecuredWith;
import com.github.vlsergey.springdata.entitysecurity.SecurityMixin;
import com.github.vlsergey.springdata.entitysecurity.StandardConditions;
import com.github.vlsergey.springdata.entitysecurity.noquerydsl.FileTestEntityRepository.FileTestEntitySecurityMixin;

import lombok.NonNull;

@SecuredWith(FileTestEntitySecurityMixin.class)
public interface FileTestEntityRepository extends JpaRepository<FileTestEntity, String> {

	class FileTestEntitySecurityMixin implements SecurityMixin<FileTestEntity> {

		@Override
		public Condition<FileTestEntity> buildCondition() {
			final String login = SecurityContextHolder.getContext().getAuthentication().getName();

			if (login.equals("root")) {
				return StandardConditions.allow();
			}
			if (login == null || login.isEmpty()) {
				return StandardConditions.deny(() -> new RuntimeException("No rights exception"));
			}

			return new Condition<FileTestEntity>() {

				private static final long serialVersionUID = 1L;

				@Override
				public Predicate toPredicate(Root<FileTestEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
					final Predicate allowedByOwner = cb.equal(cb.substring(root.get("permissions"), 0, 1), "r");
					final Predicate allowedByGroup = cb.equal(cb.substring(root.get("permissions"), 3, 4), "r");
					final Predicate allowedByOther = cb.equal(cb.substring(root.get("permissions"), 6, 7), "r");

					final Root<UserTestEntity> currentUser = query.from(UserTestEntity.class);

					final Predicate currentIsOwner = cb.equal(root.get("ownerUser"), currentUser);
					final Predicate currentUserInOwnerGroup = currentUser.in(root.join("ownerGroup").join("users"));

					return cb.and(cb.equal(currentUser.get("login"), login),
							cb.or(cb.and(allowedByOwner, currentIsOwner),
									cb.and(allowedByGroup, currentUserInOwnerGroup), allowedByOther));
				}

				@Override
				public Predicate toPredicate(Root<FileTestEntity> root, CriteriaUpdate<?> query,
						CriteriaBuilder criteriaBuilder) {
					throw new UnsupportedOperationException("not used in test cases");
				}

				@Override
				public Predicate toPredicate(Root<FileTestEntity> root, CriteriaDelete<?> query,
						CriteriaBuilder criteriaBuilder) {
					throw new UnsupportedOperationException("not used in test cases");
				}

				@Override
				public void checkEntityUpdate(@NonNull FileTestEntity entity) {
					throw new UnsupportedOperationException("not used in test cases");
				}

				@Override
				public void checkEntityInsert(@NonNull FileTestEntity entity) {
					throw new UnsupportedOperationException("not used in test cases");
				}

				@Override
				public void checkEntityDelete(@NonNull FileTestEntity entity) {
					throw new UnsupportedOperationException("not used in test cases");
				}
			};
		}

	}

}
