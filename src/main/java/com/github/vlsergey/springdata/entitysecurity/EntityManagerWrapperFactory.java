package com.github.vlsergey.springdata.entitysecurity;

import static java.util.Collections.synchronizedMap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.repository.query.JpaQueryCreator;

import com.google.common.base.Objects;

import lombok.NonNull;

/**
 * There is no simple way to customize {@link JpaQueryCreator}, so we injecting
 * additional condition directly into CriteriaQuery via wrapping EntityManager,
 * CriteriaBuilder and CriteriaQuery.
 *
 * Yeah, i know, it's ugly and may break any moment. Feel free to do better.
 */
class EntityManagerWrapperFactory {

	private static Map<CriteriaBuilder, CriteriaBuilder> cachedCbProxies = synchronizedMap(new WeakHashMap<>());
	private static Map<EntityManager, Map<SecurityMixin<?, ?>, EntityManager>> cachedEmProxies = synchronizedMap(
			new WeakHashMap<>());

	private static final ClassLoader CLASS_LOADER = EntityManagerWrapperFactory.class.getClassLoader();

	private static final Class<?>[] CRITERIA_BUILDER_WRAPPER_INTERFACES = new Class[] { CriteriaBuilder.class };
	private static final Class<?>[] CRITERIA_QUERY_WRAPPER_INTERFACES = new Class[] { CriteriaQuery.class,
			WrappedCriteriaQuery.class };
	private static final Class<?>[] ENTITY_MANAGER_WRAPPER_INTERFACES = new Class[] { EntityManager.class };

	static CriteriaBuilder wrap(final @NonNull CriteriaBuilder original) {
		return cachedCbProxies.computeIfAbsent(original, cb -> (CriteriaBuilder) Proxy.newProxyInstance(CLASS_LOADER,
				CRITERIA_BUILDER_WRAPPER_INTERFACES, new CriteriaBuilderInvocationHandler(cb)));
	}

	@SuppressWarnings("unchecked")
	static <T> CriteriaQuery<T> wrap(final @NonNull CriteriaBuilder cb, final @NonNull CriteriaQuery<T> original) {
		return (CriteriaQuery<T>) Proxy.newProxyInstance(CLASS_LOADER, CRITERIA_QUERY_WRAPPER_INTERFACES,
				new CriteriaQueryHandler(cb, original));
	}

	static EntityManager wrap(final @NonNull EntityManager original, final @NonNull SecurityMixin<?, ?> securityMixin) {
		return cachedEmProxies.computeIfAbsent(original, em -> new WeakHashMap<>()).computeIfAbsent(securityMixin,
				sm -> (EntityManager) Proxy.newProxyInstance(CLASS_LOADER, ENTITY_MANAGER_WRAPPER_INTERFACES,
						new EntityManagerInvocationHandler(original, securityMixin)));
	}

	private static final class CriteriaBuilderInvocationHandler implements InvocationHandler {
		private final CriteriaBuilder original;

		private CriteriaBuilderInvocationHandler(CriteriaBuilder original) {
			this.original = original;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (Objects.equal(method.getName(), "createQuery")) {
				CriteriaQuery<?> criteriaQuery = (CriteriaQuery<?>) method.invoke(original, args);
				return wrap((CriteriaBuilder) proxy, criteriaQuery);
			}

			return method.invoke(original, args);
		}
	}

	private static final class CriteriaQueryHandler implements InvocationHandler, WrappedCriteriaQuery {
		private static final Predicate[] EMPTY_PREDICATES_ARRAY = new Predicate[0];

		private final CriteriaBuilder cb;
		private final CriteriaQuery<?> original;
		private Expression<Boolean> restrictionA = null;
		private Predicate[] restrictionB = null;
		// we need roots to create predicates via SecurityMixin
		private List<Root<?>> roots = new ArrayList<>(1);

		private CriteriaQueryHandler(CriteriaBuilder cb, CriteriaQuery<?> original) {
			this.cb = cb;
			this.original = original;
		}

		@Override
		public CriteriaQuery<?> getDelegate() {
			return original;
		}

		@Override
		public void injectConditions(SecurityMixin<?, ?> securityMixin) {
			final Condition<?, ?> сondition = securityMixin.buildCondition();

			if (
			// unsupported case
			roots.size() != 1 || сondition.isAlwaysTrue()) {
				if (restrictionA != null) {
					original.where(restrictionA);
				} else if (restrictionB != null) {
					original.where(restrictionB);
				} else {
					original.where(EMPTY_PREDICATES_ARRAY);
				}
				return;
			}

			final Predicate secPredicate = сondition.toPredicate((Root) roots.get(0), original, cb, QueryType.SELECT);

			if (restrictionA != null) {
				original.where((Predicate) restrictionA, secPredicate);
			} else if (restrictionB != null && restrictionB.length > 0) {
				Predicate[] joined = new Predicate[restrictionB.length + 1];
				Arrays.copyOf(joined, restrictionB.length + 1);
				joined[restrictionB.length] = secPredicate;
				original.where(joined);
			} else {
				original.where(secPredicate);
			}
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (Objects.equal(method.getName(), "getDelegate") || Objects.equal(method.getName(), "injectConditions")) {
				return method.invoke(this, args);
			}

			if (Objects.equal(method.getName(), "from")) {
				Root<?> root = (Root<?>) method.invoke(original, args);
				roots.add(root);
				return root;
			}

			if (Objects.equal(method.getName(), "where")) {
				if (method.getParameterTypes()[0].isArray()
						&& method.getParameterTypes()[0].getComponentType().equals(Predicate.class)) {
					restrictionA = null;
					restrictionB = (Predicate[]) args[0];
				} else if (method.getParameterTypes()[0].equals(Expression.class)) {
					restrictionA = (Expression<Boolean>) args[0];
					restrictionB = null;
				}
			}

			final Object result = method.invoke(original, args);
			if (result == original) {
				return proxy;
			}
			return result;
		}
	}

	private static final class EntityManagerInvocationHandler implements InvocationHandler {
		private final @NonNull EntityManager original;
		private final SecurityMixin<?, ?> securityMixin;

		private EntityManagerInvocationHandler(@NonNull EntityManager original, SecurityMixin<?, ?> securityMixin) {
			this.original = original;
			this.securityMixin = securityMixin;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (Objects.equal(method.getName(), "getCriteriaBuilder")) {
				return wrap(original.getCriteriaBuilder());
			}

			if (Objects.equal(method.getName(), "createQuery") && args[0] instanceof WrappedCriteriaQuery) {
				final WrappedCriteriaQuery wrappedCriteriaQuery = (WrappedCriteriaQuery) args[0];
				final CriteriaQuery<?> originalQuery = wrappedCriteriaQuery.getDelegate();

				// we don't want to have problems with cross-thread executions due to
				// CriteriaQuery caching
				synchronized (originalQuery) {
					wrappedCriteriaQuery.injectConditions(securityMixin);

					Object[] newArgs = Arrays.copyOf(args, args.length);
					newArgs[0] = originalQuery;

					return method.invoke(original, newArgs);
				}
			}

			return method.invoke(original, args);
		}
	}

	private interface WrappedCriteriaQuery {
		CriteriaQuery<?> getDelegate();

		void injectConditions(SecurityMixin<?, ?> securityMixin);
	}

}
