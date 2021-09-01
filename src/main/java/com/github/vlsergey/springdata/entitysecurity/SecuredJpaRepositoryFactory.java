package com.github.vlsergey.springdata.entitysecurity;

import static org.springframework.data.querydsl.QuerydslUtils.QUERY_DSL_PRESENT;

import java.io.Serializable;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.springframework.beans.BeanUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.DefaultJpaQueryMethodFactory;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy;
import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

import lombok.NonNull;
import lombok.SneakyThrows;

public class SecuredJpaRepositoryFactory extends JpaRepositoryFactory {

	private final EntityManager entityManager;
	private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;
	private final PersistenceProvider extractor;
	private JpaQueryMethodFactory queryMethodFactory;
	private ThreadLocal<Class<?>> currentlyProcessedRepositoryInterface = new ThreadLocal<>();

	@Override
	public <T> T getRepository(Class<T> repositoryInterface, RepositoryFragments fragments) {
		this.currentlyProcessedRepositoryInterface.set(repositoryInterface);
		try {
			return super.getRepository(repositoryInterface, fragments);
		} finally {
			this.currentlyProcessedRepositoryInterface.remove();
		}
	}

	public SecuredJpaRepositoryFactory(EntityManager entityManager) {
		super(entityManager);

		this.entityManager = entityManager;
		this.extractor = PersistenceProvider.fromEntityManager(entityManager);
		this.queryMethodFactory = new DefaultJpaQueryMethodFactory(extractor);
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		final Class<?> repositoryInterface = currentlyProcessedRepositoryInterface.get();
		final SecurityMixin<?, ?> securityMixin = getSecurityMixin(repositoryInterface);

		return Optional.of(JpaQueryLookupStrategy.create(EntityManagerWrapperFactory.wrap(entityManager, securityMixin),
				queryMethodFactory, key, evaluationContextProvider, escapeCharacter));
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

		final SecurityMixin<?, ?> securityMixin = securedWith.value().getDeclaredConstructor().newInstance();

		boolean isQueryDslRepository = QUERY_DSL_PRESENT
				&& QuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface());

		if (isQueryDslRepository) {

			if (metadata.isReactiveRepository()) {
				throw new InvalidDataAccessApiUsageException(
						"Cannot combine Querydsl and reactive repository support in a single interface");
			}

			if (!(securityMixin instanceof SecurityMixinWithQuerydsl<?, ?>)) {
				throw new InvalidDataAccessApiUsageException("SecurityMixin class (" + securedWith.value().getName()
						+ ") must implement SecurityMixinWithQuerydsl interface if repository is Querydsl one");
			}

			return RepositoryFragments.just(newExecutor(metadata, entityManager, resolver, crudMethodMetadata,
					(SecurityMixinWithQuerydsl<?, ?>) securityMixin));
		}

		return RepositoryFragments.empty();
	}

	// TODO: cache to use 1 instance per repository
	@SuppressWarnings("unchecked")
	private <T, R extends JpaRepository<T, ? extends Serializable>> SecurityMixin<T, R> getSecurityMixin(
			Class<?> repositoryInterface) {
		return Optional.ofNullable(repositoryInterface.getAnnotation(SecuredWith.class)) //
				.map(SecuredWith::value) //
				.map(cls -> (Class<SecurityMixin<T, R>>) cls) //
				.map(BeanUtils::instantiateClass) //
				.orElseGet(() -> QuerydslPredicateExecutor.class.isAssignableFrom(repositoryInterface)
						? (SecurityMixin<T, R>) StandardConditions.alwaysAllowSecurityMixinWithQuerydsl()
						: StandardConditions.alwaysAllowSecurityMixin());
	}

	@Override
	@SneakyThrows
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected JpaRepositoryImplementation<?, ?> getTargetRepository(RepositoryInformation information,
			EntityManager entityManager) {
		final JpaRepositoryImplementation<?, ?> repository = super.getTargetRepository(information, entityManager);

		if (repository instanceof SecuredJpaRepository<?, ?, ?>) {
			final SecuredJpaRepository<?, ?, ?> secured = (SecuredJpaRepository<?, ?, ?>) repository;
			final SecurityMixin<?, ?> securityMixin = getSecurityMixin(information.getRepositoryInterface());
			secured.setSecurityMixin((SecurityMixin) securityMixin);
		}

		return repository;
	}

	@SuppressWarnings("unchecked")
	private <T> SecuredQuerydslJpaPredicateExecutor<T> newExecutor(final @NonNull RepositoryMetadata metadata,
			final @NonNull EntityManager entityManager, final @NonNull EntityPathResolver resolver,
			final @NonNull CrudMethodMetadata crudMethodMetadata,
			final @NonNull SecurityMixinWithQuerydsl<T, ?> securityMixin) {

		final JpaEntityInformation<T, ?> entityInformation = (JpaEntityInformation<T, ?>) getEntityInformation(
				metadata.getDomainType());

		return new SecuredQuerydslJpaPredicateExecutor<>(entityInformation, entityManager, resolver, crudMethodMetadata,
				securityMixin);
	}

	@Override
	public void setEscapeCharacter(EscapeCharacter escapeCharacter) {
		super.setEscapeCharacter(escapeCharacter);
		this.escapeCharacter = escapeCharacter;
	}

	@Override
	public void setQueryMethodFactory(JpaQueryMethodFactory queryMethodFactory) {
		super.setQueryMethodFactory(queryMethodFactory);
		this.queryMethodFactory = queryMethodFactory;
	}

}
