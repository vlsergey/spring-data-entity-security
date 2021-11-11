package com.github.vlsergey.springdata.entitysecurity;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;

public class SecuredJpaRepository<T, ID extends Serializable, R extends JpaRepository<T, ID>>
		extends SimpleJpaRepository<T, ID> {

	private static final Object TXSM_KEY_USER_ID = new Object();

	private static final String UOE_MESSAGE_BY_EXAMPLE = "by-example methods are not supported by SecuredJpaRepository";

	private static final String UOE_MESSAGE_NON_SINGULAR_ID = "Repositories without singlular ID attribute are not supported yet";

	private final @NonNull JpaEntityInformation<T, ID> entityInformation;

	private final @NonNull EntityManager entityManager;

	@Setter(AccessLevel.PACKAGE)
	private @NonNull R repositoryBean;

	@Setter(AccessLevel.PACKAGE)
	private @NonNull SecurityMixin<T, R> securityMixin;

	public SecuredJpaRepository(final @NonNull JpaEntityInformation<T, ID> entityInformation,
			final @NonNull EntityManager entityManager) {
		super(entityInformation, entityManager);
		this.entityInformation = entityInformation;
		this.entityManager = entityManager;
	}

	@Nullable
	protected final static <T> Specification<T> and(@Nullable Specification<T> a, @Nullable Specification<T> b) {
		return a == null ? b : a.and(b);
	}

	private static void invalidateSuccessCacheIfUserChanged(final @NonNull Object actualUserKey) {
		final Object stored = TransactionSynchronizationManager.getResource(TXSM_KEY_USER_ID);

		if (stored == null) {
			TransactionSynchronizationManager.bindResource(TXSM_KEY_USER_ID, actualUserKey);
			TransactionSynchronizationManager.registerSynchronization(UnbindUserIdTxSync.INSTANCE);
			return;
		}

		if (!Objects.equals(stored, actualUserKey)) {
			for (QueryType qt : QueryType.values()) {
				final Set<?> cache = (Set<?>) TransactionSynchronizationManager.getResource(qt);
				if (cache != null) {
					cache.clear();
				}
			}
			TransactionSynchronizationManager.unbindResource(TXSM_KEY_USER_ID);
			TransactionSynchronizationManager.bindResource(TXSM_KEY_USER_ID, actualUserKey);
		}
	}

	private @NonNull Specification<T> buildIdCondition(ID id) {
		final SingularAttribute<? super T, ?> idAttribute = this.entityInformation.getIdAttribute();
		if (idAttribute == null) {
			throw new UnsupportedOperationException(UOE_MESSAGE_NON_SINGULAR_ID);
		}

		return (root, cq, cb) -> cb.equal(root.get(idAttribute.getName()), id);
	}

	/**
	 * return {@literal false} if entity deletion shall be ignored, {@literal true}
	 * is proceeded
	 */
	private boolean checkDelete(T entity) {
		if (entityInformation.isNew(entity)) {
			return false;
		}

		final ID currentId = entityInformation.getId(entity);
		if (!entityManager.contains(entity)) {
			// deleting detached entity is just deleting by ID
			if (super.existsById(currentId)) {
				if (existsById(QueryType.DELETE, currentId)) {
					return true;
				}
				securityMixin.onForbiddenDelete(entity);
			}
			return false;
		}

		// restore old entity information
		try {
			entityManager.refresh(entity);
		} catch (EntityNotFoundException exc) {
			return false;
		}

		checkWithCache(securityMixin.buildCondition(), entity, QueryType.DELETE);
		return true;
	}

	private void checkSave(final Condition<T, R> condition, T entity) {
		if (entityInformation.isNew(entity)) {
			// conflict with existing DB record on INSERT is not our problem
			checkWithCache(condition, entity, QueryType.INSERT);
			return;
		}

		final ID currentId = entityInformation.getId(entity);
		if (!entityManager.contains(entity)) {
			// saving detached entity is just update... if we have entity in database... but
			// do we?

			T fromDb = entityManager.find(getDomainClass(), currentId);
			if (fromDb != null) {
				checkWithCache(condition, fromDb, QueryType.UPDATE);
				checkWithCache(condition, entity, QueryType.UPDATE);
				return;
			} else {
				checkWithCache(condition, entity, QueryType.INSERT);
				return;
			}
		}

		// the problem here -- we MAY have object in DB with different values, that lead
		// to different check results (like changing owner attribute of file). We again
		// need to check both

		/*
		 * Hibernate-only optimization (trying to do something in case when current
		 * object is not yet in DB, thus reducing DB lookup queries)
		 */
		final Optional<Boolean> existsInDatabase = HibernateUtils.isExistsInDatabase(entityManager, entity);
		if (!existsInDatabase.orElse(true)) {
			// If entity does not exists in DB (i.e. not saved yet)
			checkWithCache(condition, entity, QueryType.INSERT);
			return;
		}

		final ID idToCheck;
		final T otherEntityWithCurrentId = entityManager.getReference(getDomainClass(), currentId);
		if (otherEntityWithCurrentId != entity) {
			idToCheck = HibernateUtils.<ID>getIdentifier(entityManager, entity)
					.orElseThrow(() -> new UnsupportedOperationException("Changing ID is not supported yet"));
		} else {
			idToCheck = currentId;
		}

		if (!existsById(QueryType.UPDATE, idToCheck) && super.existsById(idToCheck)) {
			securityMixin.onForbiddenUpdate(entity);
		} else {
			checkWithCache(condition, entity, QueryType.INSERT);
		}
	}

	void checkWithCache(final Condition<T, R> condition, final T entity, final QueryType queryType) {
		if (condition.isAlwaysTrue()) {
			return;
		}
		if (condition.isAlwaysFalse()) {
			securityMixin.onForbiddenOperation(entity, queryType);
			return;
		}

		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			condition.checkEntity(repositoryBean, entity, queryType);
			return;
		}

		final Object entityCacheKey = condition.getEntitySecurityCheckCacheKey(entity);
		final Object currentUserSecurityCheckCacheKey = condition.getCurrentUserSecurityCheckCacheKey();

		if (entityCacheKey == null || currentUserSecurityCheckCacheKey == null) {
			condition.checkEntity(repositoryBean, entity, queryType);
			return;
		}

		invalidateSuccessCacheIfUserChanged(currentUserSecurityCheckCacheKey);

		@SuppressWarnings("unchecked")
		Set<Object> successChecksKeys = (Set<Object>) TransactionSynchronizationManager.getResource(queryType);
		if (successChecksKeys != null && successChecksKeys.contains(entityCacheKey)) {
			return;
		}

		condition.checkEntity(repositoryBean, entity, queryType);

		if (successChecksKeys == null) {
			successChecksKeys = new HashSet<>();
			TransactionSynchronizationManager.bindResource(queryType, successChecksKeys);
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					TransactionSynchronizationManager.unbindResource(queryType);
				}
			});
		}
		successChecksKeys.add(entityCacheKey);
	}

	@Override
	public long count() {
		return switchByCondition(QueryType.SELECT, () -> 0L, super::count, super::count);
	}

	@Override
	public <S extends T> long count(Example<S> example) {
		throw new UnsupportedOperationException(UOE_MESSAGE_BY_EXAMPLE);
	}

	@Override
	public long count(Specification<T> spec) {
		return switchByCondition(QueryType.SELECT, () -> 0L, () -> super.count(spec),
				condition -> super.count(and(condition, spec)));
	}

	@Override
	public void delete(T entity) {
		switchByConditionVoid(() -> {
		}, () -> super.delete(entity), condition -> {
			if (checkDelete(entity)) {
				super.delete(entity);
			}
		});
	}

	@Override
	@Transactional
	public void deleteAll() {
		final Condition<T, R> condition = securityMixin.buildCondition();
		if (condition.isAlwaysFalse()) {
			return;
		}
		if (condition.isAlwaysTrue()) {
			super.deleteAll();
			return;
		}

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaDelete<T> cd = cb.createCriteriaDelete(getDomainClass());
		final Root<T> root = cd.from(getDomainClass());
		cd.where(condition.toPredicate(root, cd, cb, QueryType.DELETE));
		entityManager.createQuery(cd).executeUpdate();
	}

	@Override
	public void deleteAllByIdInBatch(Iterable<ID> ids) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void deleteAllInBatch() {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void deleteAllInBatch(Iterable<T> entities) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void deleteById(final @NonNull ID id) {
		Supplier<EmptyResultDataAccessException> errorSupplier = () -> new EmptyResultDataAccessException(
				String.format("No %s entity with id %s exists!", entityInformation.getJavaType(), id), 1);

		switchByConditionVoid(() -> {
			throw errorSupplier.get();
		}, () -> super.deleteById(id), condition -> {
			if (!existsById(QueryType.DELETE, id)) {
				throw errorSupplier.get();
			}
			super.deleteById(id);
		});
	}

	@Override
	public void deleteInBatch(Iterable<T> entities) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		throw new UnsupportedOperationException(UOE_MESSAGE_BY_EXAMPLE);
	}

	@Override
	public boolean existsById(ID id) {
		return existsById(QueryType.SELECT, id);
	}

	protected boolean existsById(QueryType queryType, ID id) {
		return switchByCondition(queryType, () -> false, () -> super.existsById(id), condition -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Integer> rootQuery = cb.createQuery(Integer.class);

			final Root<T> root = rootQuery.from(getDomainClass());
			rootQuery.select(cb.literal(1));
			rootQuery.where(buildIdCondition(id).toPredicate(root, rootQuery, cb),
					condition.toPredicate(root, rootQuery, cb));

			final TypedQuery<Integer> query = entityManager.createQuery(rootQuery);
			return !query.getResultList().isEmpty();
		});
	}

	@Override
	public List<T> findAll() {
		return switchByCondition(QueryType.SELECT, Collections::emptyList, super::findAll, super::findAll);
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example) {
		throw new UnsupportedOperationException(UOE_MESSAGE_BY_EXAMPLE);
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
		throw new UnsupportedOperationException(UOE_MESSAGE_BY_EXAMPLE);
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
		throw new UnsupportedOperationException(UOE_MESSAGE_BY_EXAMPLE);
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		return switchByCondition(QueryType.SELECT, () -> Page.empty(pageable), () -> super.findAll(pageable),
				condition -> super.findAll(condition, pageable));
	}

	@Override
	public List<T> findAll(Sort sort) {
		return switchByCondition(QueryType.SELECT, Collections::emptyList, () -> super.findAll(sort),
				condition -> super.findAll(condition, sort));
	}

	@Override
	public List<T> findAll(Specification<T> spec) {
		return switchByCondition(QueryType.SELECT, Collections::emptyList, () -> super.findAll(spec),
				condition -> super.findAll(and(spec, condition)));
	}

	@Override
	public Page<T> findAll(Specification<T> spec, Pageable pageable) {
		return switchByCondition(QueryType.SELECT, () -> Page.empty(pageable), () -> super.findAll(spec, pageable),
				condition -> super.findAll(and(spec, condition), pageable));
	}

	@Override
	public List<T> findAll(Specification<T> spec, Sort sort) {
		return switchByCondition(QueryType.SELECT, Collections::emptyList, () -> super.findAll(spec, sort),
				condition -> super.findAll(and(spec, condition), sort));
	}

	@Override
	public List<T> findAllById(Iterable<ID> ids) {
		return switchByCondition(QueryType.SELECT, Collections::emptyList, () -> super.findAllById(ids),
				// TODO: optimize with batch query by ID
				condition -> StreamSupport.stream(ids.spliterator(), false).map(SecuredJpaRepository.this::findById)
						.map(op -> op.map(Collections::singletonList).orElse(emptyList())).flatMap(Collection::stream)
						.collect(toList()));
	}

	@Override
	public Optional<T> findById(ID id) {
		return switchByCondition(QueryType.SELECT, Optional::empty, () -> super.findById(id),
				// we need to take care about lock hits and other things, so select twice
				condition -> findOne(and(condition, buildIdCondition(id))).isPresent() //
						? super.findById(id)
						: Optional.empty());
	}

	@Override
	public <S extends T> Optional<S> findOne(Example<S> example) {
		throw new UnsupportedOperationException(UOE_MESSAGE_BY_EXAMPLE);
	}

	@Override
	public Optional<T> findOne(Specification<T> spec) {
		return switchByCondition(QueryType.SELECT, Optional::empty, () -> super.findOne(spec),
				condition -> super.findOne(and(spec, condition)));
	}

	@Override
	public T getById(ID id) {
		return findById(id).orElseThrow(EntityNotFoundException::new);
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public T getOne(ID id) {
		return switchByCondition(QueryType.SELECT, () -> {
			throw new EntityNotFoundException();
		}, () -> super.getOne(id),
				condition -> findOne(and(buildIdCondition(id), condition)).orElseThrow(EntityNotFoundException::new));
	}

	@Override
	public <S extends T> S save(S entity) {
		return switchByCondition(() -> {
			securityMixin.onForbiddenUpdate(entity);
			return entity;
		}, () -> super.save(entity), condition -> {
			checkSave(condition, entity);
			return super.save(entity);
		});
	}

	@Override
	public <S extends T> List<S> saveAll(Iterable<S> entities) {
		return switchByCondition(() -> {
			entities.forEach(securityMixin::onForbiddenUpdate);
			return StreamSupport.stream(entities.spliterator(), false).collect(toList());
		}, () -> super.saveAll(entities), condition -> {
			// TODO: optimize batch check
			entities.forEach(entity -> this.checkSave(condition, entity));
			return super.saveAll(entities);
		});
	}

	@Override
	public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) {
		return switchByCondition(() -> {
			entities.forEach(securityMixin::onForbiddenUpdate);
			return StreamSupport.stream(entities.spliterator(), false).collect(toList());
		}, () -> super.saveAllAndFlush(entities), condition -> {
			// TODO: optimize batch check
			entities.forEach(entity -> this.checkSave(condition, entity));
			return super.saveAllAndFlush(entities);
		});
	}

	private <E> E switchByCondition(final @NonNull QueryType queryType, final @NonNull Supplier<E> alwaysFalse,
			final @NonNull Supplier<E> alwaysTrue, final @NonNull Function<Specification<T>, E> other) {
		return switchByCondition(alwaysFalse, alwaysTrue,
				condition -> other.apply(condition.toSpecification(queryType)));
	}

	private <E> E switchByCondition(final @NonNull Supplier<E> alwaysFalse, final @NonNull Supplier<E> alwaysTrue,
			final @NonNull Function<Condition<T, R>, E> other) {
		final Condition<T, R> condition = securityMixin.buildCondition();
		if (condition.isAlwaysTrue()) {
			return alwaysTrue.get();
		} else if (condition.isAlwaysFalse()) {
			return alwaysFalse.get();
		} else {
			return other.apply(condition);
		}
	}

	private void switchByConditionVoid(final @NonNull Runnable alwaysFalse, final @NonNull Runnable alwaysTrue,
			final @NonNull Consumer<Condition<T, R>> other) {
		final Condition<T, R> condition = securityMixin.buildCondition();
		if (condition.isAlwaysTrue()) {
			alwaysTrue.run();
		} else if (condition.isAlwaysFalse()) {
			alwaysFalse.run();
		} else {
			other.accept(condition);
		}
	}

	private static final class UnbindUserIdTxSync implements TransactionSynchronization {

		static final UnbindUserIdTxSync INSTANCE = new UnbindUserIdTxSync();

		@Override
		public void afterCompletion(int status) {
			TransactionSynchronizationManager.unbindResource(TXSM_KEY_USER_ID);
		}
	}
}
