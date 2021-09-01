package com.github.vlsergey.springdata.entitysecurity;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

public class SecuredJpaRepositoryFactoryBean<R extends SecuredJpaRepository<T, ID, R>, T, ID extends Serializable>
		extends JpaRepositoryFactoryBean<R, T, ID> {

	public SecuredJpaRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
		super(repositoryInterface);
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
		return new SecuredJpaRepositoryFactory(entityManager);
	}

}