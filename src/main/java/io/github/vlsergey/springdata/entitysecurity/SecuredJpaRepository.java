package io.github.vlsergey.springdata.entitysecurity;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.SingularAttribute;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import lombok.NonNull;

public class SecuredJpaRepository<T, ID> extends SimpleJpaRepository<T, ID> implements SecuredJpaRepositoryImpl<T> {

	private static final String UOE_MESSAGE_BY_EXAMPLE = "by-example methods are not supported by SecuredJpaRepository";

	private static final String UOE_MESSAGE_NON_SINGULAR_ID = "Repositories without singlular ID attribute are not supported yet";

	@Nullable
	protected final static <T> Specification<T> and(@Nullable Specification<T> a, @Nullable Specification<T> b) {
		return a == null ? b : a.and(b);
	}

	private final @NonNull JpaEntityInformation<T, ?> entityInformation;

	private final @NonNull EntityManager entityManager;

	private @NonNull SecurityMixin<T> securityMixin;

	public SecuredJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
		super(entityInformation, entityManager);
		this.entityInformation = entityInformation;
		this.entityManager = entityManager;
	}

	private @NonNull Specification<T> buildIdCondition(ID id) {
		final SingularAttribute<? super T, ?> idAttribute = this.entityInformation.getIdAttribute();
		if (idAttribute == null) {
			throw new UnsupportedOperationException(UOE_MESSAGE_NON_SINGULAR_ID);
		}

		return (root, cq, cb) -> cb.equal(root.get(idAttribute.getName()), id);
	}

	private void checkSave(final Condition<T> condition, T entity) {
		if (entityInformation.isNew(entity)) {
			condition.checkEntityInsert(entity);
		} else {
			condition.checkEntityUpdate(entity);
		}
	}

	@Override
	public long count() {
		return switchByCondition(() -> 0L, super::count, condition -> super.count(condition));
	}

	@Override
	public <S extends T> long count(Example<S> example) {
		throw new UnsupportedOperationException(UOE_MESSAGE_BY_EXAMPLE);
	}

	@Override
	public long count(Specification<T> spec) {
		return switchByCondition(() -> 0L, () -> super.count(spec), condition -> super.count(and(condition, spec)));
	}

	@Override
	public void delete(T entity) {
		securityMixin.buildCondition().checkEntityDelete(entity);
		super.delete(entity);
	}

	@Override
	@Transactional
	public void deleteAll() {
		final Condition<T> condition = securityMixin.buildCondition();
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
		cd.where(condition.toPredicate(root, cd, cb));
		entityManager.createQuery(cd).executeUpdate();
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void deleteAllById(Iterable<? extends ID> ids) {
		throw new UnsupportedOperationException("NYI");
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
	public void deleteById(ID id) {
		throw new UnsupportedOperationException("NYI");
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
		return switchByCondition(() -> false, () -> super.existsById(id), condition -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Boolean> rootQuery = cb.createQuery(Boolean.class);

			final Subquery<Integer> subquery = rootQuery.subquery(Integer.class);
			subquery.select(cb.literal(1));
			final Root<T> subFrom = subquery.from(getDomainClass());
			subquery.where(and(buildIdCondition(id), condition).toPredicate(subFrom, rootQuery, cb));
			rootQuery.select(cb.exists(subquery));

			return entityManager.createQuery(rootQuery).getSingleResult();
		});
	}

	@Override
	public List<T> findAll() {
		return switchByCondition(Collections::emptyList, super::findAll, super::findAll);
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
		return switchByCondition(() -> Page.empty(pageable), () -> super.findAll(pageable),
				condition -> super.findAll(condition, pageable));
	}

	@Override
	public List<T> findAll(Sort sort) {
		return switchByCondition(Collections::emptyList, () -> super.findAll(sort),
				condition -> super.findAll(condition, sort));
	}

	@Override
	public List<T> findAll(Specification<T> spec) {
		return switchByCondition(Collections::emptyList, () -> super.findAll(spec),
				condition -> super.findAll(and(spec, condition)));
	}

	@Override
	public Page<T> findAll(Specification<T> spec, Pageable pageable) {
		return switchByCondition(() -> Page.empty(pageable), () -> super.findAll(spec, pageable),
				condition -> super.findAll(and(spec, condition), pageable));
	}

	@Override
	public List<T> findAll(Specification<T> spec, Sort sort) {
		return switchByCondition(Collections::emptyList, () -> super.findAll(spec, sort),
				condition -> super.findAll(and(spec, condition), sort));
	}

	@Override
	public List<T> findAllById(Iterable<ID> ids) {
		return switchByCondition(Collections::emptyList, () -> findAllById(ids), condition -> {
			// TODO: optimize with batch query by ID
			return StreamSupport.stream(ids.spliterator(), false).flatMap(id -> findById(id).stream())
					.collect(toList());
		});
	}

	@Override
	public Optional<T> findById(ID id) {
		return switchByCondition(Optional::empty, () -> super.findById(id),
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
		return switchByCondition(Optional::empty, () -> super.findOne(spec),
				condition -> super.findOne(and(spec, condition)));
	}

	@Override
	public T getById(ID id) {
		return findById(id).get();
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public T getOne(ID id) {
		return switchByCondition(() -> {
			throw new EntityNotFoundException();
		}, () -> super.getOne(id),
				condition -> findOne(and(buildIdCondition(id), condition)).orElseThrow(EntityNotFoundException::new));
	}

	@Override
	public <S extends T> S save(S entity) {
		final Condition<T> condition = securityMixin.buildCondition();
		checkSave(condition, entity);
		return super.save(entity);
	}

	@Override
	public <S extends T> List<S> saveAll(Iterable<S> entities) {
		final Condition<T> condition = securityMixin.buildCondition();
		entities.forEach(entity -> this.checkSave(condition, entity));
		return super.saveAll(entities);
	}

	@Override
	public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) {
		final Condition<T> condition = securityMixin.buildCondition();
		entities.forEach(entity -> this.checkSave(condition, entity));
		return super.saveAllAndFlush(entities);
	}

	@Override
	public void setSecurityMixin(@NonNull SecurityMixin<T> securityMixin) {
		this.securityMixin = securityMixin;
	}

	protected <R> R switchByCondition(final @NonNull Supplier<R> alwaysFalse, final @NonNull Supplier<R> alwaysTrue,
			final @NonNull Function<Condition<T>, R> other) {
		final Condition<T> condition = securityMixin.buildCondition();
		if (condition.isAlwaysTrue()) {
			return alwaysTrue.get();
		}
		if (condition.isAlwaysFalse()) {
			return alwaysFalse.get();
		}
		return other.apply(condition);
	}
}
