package com.github.vlsergey.springdata.entitysecurity;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import lombok.SneakyThrows;

class HibernateUtils {

	private static final Optional<Class<?>> CLASS_SESSION = findClass("org.hibernate.Session");

	private static final Optional<Method> METHOD_SESSION_GET_IDENTIFIER = findMethod(CLASS_SESSION, "getIdentifier",
			Object.class);

	@SuppressWarnings("unchecked")
	static <T> Optional<Class<? extends T>> findClass(String className) {
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

	@SneakyThrows
	static <ID extends Serializable> Optional<ID> getIdentifier(EntityManager entityManager, Object entity) {
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
}
