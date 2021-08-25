package com.github.vlsergey.springdata.entitysecurity;

import static org.springframework.data.querydsl.QuerydslUtils.QUERY_DSL_PRESENT;

import java.util.Optional;

import javax.persistence.EntityManager;

import org.springframework.beans.BeanUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;

import lombok.NonNull;
import lombok.SneakyThrows;

public class SecuredJpaRepositoryFactory extends JpaRepositoryFactory {

	public SecuredJpaRepositoryFactory(EntityManager entityManager) {
		super(entityManager);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return metadata.getRepositoryInterface().getAnnotation(SecuredWith.class) != null ? SecuredJpaRepository.class
				: super.getRepositoryBaseClass(metadata);
	}

	@Override
	@SneakyThrows
	protected RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata, EntityManager entityManager,
			EntityPathResolver resolver, CrudMethodMetadata crudMethodMetadata) {

		final SecuredWith securedWith = metadata.getRepositoryInterface().getAnnotation(SecuredWith.class);
		if (securedWith == null) {
			return super.getRepositoryFragments(metadata, entityManager, resolver, crudMethodMetadata);
		}

		final SecurityMixin<?> securityMixin = securedWith.value().getDeclaredConstructor().newInstance();

		boolean isQueryDslRepository = QUERY_DSL_PRESENT
				&& QuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface());

		if (isQueryDslRepository) {

			if (metadata.isReactiveRepository()) {
				throw new InvalidDataAccessApiUsageException(
						"Cannot combine Querydsl and reactive repository support in a single interface");
			}

			if (!(securityMixin instanceof SecurityMixinWithQuerydsl<?>)) {
				throw new InvalidDataAccessApiUsageException("SecurityMixin class (" + securedWith.value().getName()
						+ ") must implement SecurityMixinWithQuerydsl interface if repository is Querydsl one");
			}

			return RepositoryFragments.just(newExecutor(metadata, entityManager, resolver, crudMethodMetadata,
					(SecurityMixinWithQuerydsl<?>) securityMixin));
		}

		return RepositoryFragments.empty();
	}

	@Override
	@SneakyThrows
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected JpaRepositoryImplementation<?, ?> getTargetRepository(RepositoryInformation information,
			EntityManager entityManager) {
		final JpaRepositoryImplementation<?, ?> repository = super.getTargetRepository(information, entityManager);

		if (repository instanceof SecuredJpaRepository<?, ?>) {
			final SecuredJpaRepository<?, ?> secured = (SecuredJpaRepository<?, ?>) repository;
			final SecurityMixin<?> securityMixin = getSecurityMixin(information);
			secured.setSecurityMixin((SecurityMixin) securityMixin);
		}

		return repository;
	}

	@SuppressWarnings("unchecked")
	private <T> SecurityMixin<T> getSecurityMixin(RepositoryInformation information) {
		return Optional.ofNullable(information.getRepositoryInterface().getAnnotation(SecuredWith.class)) //
				.map(SecuredWith::value) //
				.map(cls -> (Class<SecurityMixin<T>>) cls) //
				.map(BeanUtils::instantiateClass) //
				.orElseGet(() -> QuerydslPredicateExecutor.class.isAssignableFrom(information.getRepositoryInterface())
						? StandardConditions.alwaysAllowSecurityMixinWithQuerydsl()
						: StandardConditions.alwaysAllowSecurityMixin());
	}

	@SuppressWarnings("unchecked")
	private <T> SecuredQuerydslJpaPredicateExecutor<T> newExecutor(final @NonNull RepositoryMetadata metadata,
			final @NonNull EntityManager entityManager, final @NonNull EntityPathResolver resolver,
			final @NonNull CrudMethodMetadata crudMethodMetadata,
			final @NonNull SecurityMixinWithQuerydsl<T> securityMixin) {

		final JpaEntityInformation<T, ?> entityInformation = (JpaEntityInformation<T, ?>) getEntityInformation(
				metadata.getDomainType());

		return new SecuredQuerydslJpaPredicateExecutor<>(entityInformation, entityManager, resolver, crudMethodMetadata,
				securityMixin);
	}

}
