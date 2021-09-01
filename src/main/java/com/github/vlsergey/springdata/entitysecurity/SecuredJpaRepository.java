package com.github.vlsergey.springdata.entitysecurity;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;

public class SecuredJpaRepository<T, ID extends Serializable, R extends SecuredJpaRepository<T, ID, R>>
		extends SimpleJpaRepository<T, ID> {

	private static final String UOE_MESSAGE_BY_EXAMPLE = "by-example methods are not supported by SecuredJpaRepository";

	private static final String UOE_MESSAGE_NON_SINGULAR_ID = "Repositories without singlular ID attribute are not supported yet";

	private final @NonNull JpaEntityInformation<T, ID> entityInformation;

	private final @NonNull EntityManager entityManager;

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

	private @NonNull Specification<T> buildIdCondition(ID id) {
		final SingularAttribute<? super T, ?> idAttribute = this.entityInformation.getIdAttribute();
		if (idAttribute == null) {
			throw new UnsupportedOperationException(UOE_MESSAGE_NON_SINGULAR_ID);
		}

		return (root, cq, cb) -> cb.equal(root.get(idAttribute.getName()), id);
	}

	private void checkSave(final Condition<T, R> condition, T entity) {
		if (entityInformation.isNew(entity)) {
			// conflict with existing db record is not our problem
			condition.checkEntityInsert((R) this, entity);
			return;
		}

		final ID currentId = entityInformation.getId(entity);
		final ID idToCheck;
		if (entityManager.getReference(getDomainClass(), currentId) != entity) {
			idToCheck = HibernateUtils.<ID>getIdentifier(entityManager, entity)
					.orElseThrow(() -> new UnsupportedOperationException("Changing ID is not supported yet"));
		} else {
			idToCheck = currentId;
		}

		if (!existsById(QueryType.UPDATE, idToCheck)) {
			securityMixin.onForbiddenUpdate(entity);
			return;
		}
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
		// restore old entity ID
		if (!entityInformation.isNew(entity)) {
			try {
				entityManager.refresh(entity);
			} catch (EntityNotFoundException exc) {
				return;
			}
		}
		this.deleteById(entityInformation.getId(entity));
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
		switchByConditionVoid(() -> {
			throw new EntityNotFoundException();
		}, () -> super.deleteById(id), condition -> {
			if (!existsById(QueryType.DELETE, id)) {
				throw new EntityNotFoundException();
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
		return switchByCondition(QueryType.SELECT, Collections::emptyList, () -> findAllById(ids),
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
		final Condition<T, R> condition = securityMixin.buildCondition();
		checkSave(condition, entity);
		return super.save(entity);
	}

	@Override
	public <S extends T> List<S> saveAll(Iterable<S> entities) {
		final Condition<T, R> condition = securityMixin.buildCondition();
		entities.forEach(entity -> this.checkSave(condition, entity));
		return super.saveAll(entities);
	}

	@Override
	public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) {
		final Condition<T, R> condition = securityMixin.buildCondition();
		entities.forEach(entity -> this.checkSave(condition, entity));
		return super.saveAllAndFlush(entities);
	}

	protected <E> E switchByCondition(final QueryType queryType, final @NonNull Supplier<E> alwaysFalse,
			final @NonNull Supplier<E> alwaysTrue, final @NonNull Function<Specification<T>, E> other) {
		final Condition<T, R> condition = securityMixin.buildCondition();
		if (condition.isAlwaysTrue()) {
			return alwaysTrue.get();
		} else if (condition.isAlwaysFalse()) {
			return alwaysFalse.get();
		} else {
			return other.apply(condition.toSpecification(queryType));
		}
	}

	protected void switchByConditionVoid(final @NonNull Runnable alwaysFalse, final @NonNull Runnable alwaysTrue,
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
}
