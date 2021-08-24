package io.github.vlsergey.springdata.entitysecurity.noquerydsl;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import io.github.vlsergey.springdata.entitysecurity.QueryListeningConfiguration;
import io.github.vlsergey.springdata.entitysecurity.SecuredJpaRepositoryFactoryBean;

@Configuration
@EnableJpaRepositories(value = "io.github.vlsergey.springdata.entitysecurity.noquerydsl", repositoryFactoryBeanClass = SecuredJpaRepositoryFactoryBean.class)
@ComponentScan("io.github.vlsergey.springdata.entitysecurity.noquerydsl")
@EntityScan("io.github.vlsergey.springdata.entitysecurity.noquerydsl")
@Import(QueryListeningConfiguration.class)
public class TestConfiguration {

}
