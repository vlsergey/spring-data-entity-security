package com.github.vlsergey.springdata.entitysecurity.owned;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.github.vlsergey.springdata.entitysecurity.QueryListeningConfiguration;
import com.github.vlsergey.springdata.entitysecurity.SecuredJpaRepositoryFactoryBean;

@Configuration
@EnableJpaRepositories(repositoryFactoryBeanClass = SecuredJpaRepositoryFactoryBean.class)
@ComponentScan
@EntityScan
@Import(QueryListeningConfiguration.class)
public class TestConfiguration {

}
