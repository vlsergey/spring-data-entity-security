package com.github.vlsergey.springdata.entitysecurity;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import lombok.NonNull;
import lombok.SneakyThrows;

class HibernateUtils {

	private static final Optional<Class<?>> CLASS_ENTITY_ENTRY = findClass("org.hibernate.engine.spi.EntityEntry");

	private static final Optional<Class<?>> CLASS_PERSISTENCE_CONTEXT = findClass(
			"org.hibernate.engine.spi.PersistenceContext");

	private static final Optional<Class<?>> CLASS_SESSION = findClass("org.hibernate.Session");

	private static final Optional<Class<?>> CLASS_SHARED_SESSION_CONTRACT_IMPLEMENTOR = findClass(
			"org.hibernate.engine.spi.SharedSessionContractImplementor");

	private static final Optional<Method> METHOD_ENTITY_ENTRY_IS_EXISTS_IN_DATABASE = findMethod(CLASS_ENTITY_ENTRY,
			"isExistsInDatabase");

	private static final Optional<Method> METHOD_PERSISTENCE_CONTEXT_GET_ENTRY = findMethod(CLASS_PERSISTENCE_CONTEXT,
			"getEntry", Object.class);

	private static final Optional<Method> METHOD_SESSION_GET_IDENTIFIER = findMethod(CLASS_SESSION, "getIdentifier",
			Object.class);

	private static final Optional<Method> METHOD_SHARED_SESSION_CONTRACT_IMPLEMENTOR_GET_PERSISTENCE_CONTEXT_INTERNAL = findMethod(
			CLASS_SHARED_SESSION_CONTRACT_IMPLEMENTOR, "getPersistenceContextInternal");

	@SuppressWarnings("unchecked")
	static <T> Optional<Class<? extends T>> findClass(final @NonNull String className) {
		try {
			return Optional.of((Class<T>) Class.forName(className));
		} catch (Exception exc) {
			return Optional.empty();
		}
	}

	static Optional<Method> findMethod(Optional<Class<?>> cls, String methodName, Class<?>... paramArgsClasses) {
		return cls.flatMap(c -> Arrays.stream(c.getMethods()) //
				.filter(method -> Objects.equals(method.getName(), methodName))
				.filter(method -> method.getParameterCount() == paramArgsClasses.length) //
				.filter(method -> {
					Class<?>[] parameterTypes = method.getParameterTypes();
					for (int i = 0; i < parameterTypes.length; i++) {
						Class<?> arg = parameterTypes[i];
						if (!Objects.equals(arg.getName(), paramArgsClasses[i].getName())) {
							return false;
						}
					}
					return true;
				}).findAny());
	}

	private static Optional<Object> getEntry(final @NonNull Object persistenceContext, final @NonNull Object entity) {
		return METHOD_PERSISTENCE_CONTEXT_GET_ENTRY.map(method -> getOrNull(method, persistenceContext, entity));
	}

	@SneakyThrows
	static <ID extends Serializable> Optional<ID> getIdentifier(final @NonNull EntityManager entityManager,
			Object entity) {
		if (!CLASS_SESSION.isPresent() || !METHOD_SESSION_GET_IDENTIFIER.isPresent()) {
			return Optional.empty();
		}

		Object session;
		try {
			session = entityManager.unwrap(CLASS_SESSION.get());
		} catch (PersistenceException exc) {
			return Optional.empty();
		}

		try {
			@SuppressWarnings("unchecked")
			final ID id = (ID) METHOD_SESSION_GET_IDENTIFIER.get().invoke(session, entity);
			return Optional.of(id);
		} catch (InvocationTargetException exc) {
			throw exc.getTargetException();
		}
	}

	@SneakyThrows
	@SuppressWarnings("unchecked")
	private static <T> T getOrNull(final @NonNull Method method, Object obj, final @NonNull Class<T> resultClass,
			final @NonNull Object... args) {
		try {
			return (T) method.invoke(obj, args);
		} catch (Exception exc) {
			return null;
		}
	}

	@SneakyThrows
	private static Object getOrNull(Method method, Object obj, Object... args) {
		return getOrNull(method, obj, Object.class, args);
	}

	private static Optional<Object> getPersistenceContextInternal(final @NonNull Object hibernateSession) {
		return METHOD_SHARED_SESSION_CONTRACT_IMPLEMENTOR_GET_PERSISTENCE_CONTEXT_INTERNAL
				.map(method -> getOrNull(method, hibernateSession));
	}

	static Optional<Boolean> isExistsInDatabase(final @NonNull EntityManager entityManager,
			final @NonNull Object entity) {
		return toSharedSessionContractImplementor(entityManager) //
				.flatMap(HibernateUtils::getPersistenceContextInternal) //
				.flatMap(context -> getEntry(context, entity)) //
				.flatMap(HibernateUtils::isExistsInDatabase);
	}

	private static Optional<Boolean> isExistsInDatabase(final @NonNull Object entityEntry) {
		return METHOD_ENTITY_ENTRY_IS_EXISTS_IN_DATABASE.map(method -> getOrNull(method, entityEntry, boolean.class));
	}

	private static Optional<Object> toSharedSessionContractImplementor(final @NonNull EntityManager entityManager) {
		return CLASS_SHARED_SESSION_CONTRACT_IMPLEMENTOR.map(sessionImplClass -> {
			try {
				return entityManager.unwrap(sessionImplClass);
			} catch (Exception exc) {
				return null;
			}
		});
	}
}
