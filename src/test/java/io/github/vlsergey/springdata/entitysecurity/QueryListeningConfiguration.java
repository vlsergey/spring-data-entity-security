package io.github.vlsergey.springdata.entitysecurity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorBeanPostProcessor;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceDecoratorProperties;
import com.github.gavlyukovskiy.boot.jdbc.decorator.DataSourceNameResolver;
import com.github.gavlyukovskiy.boot.jdbc.decorator.dsproxy.DataSourceProxyConfiguration;

@Configuration
@Import(DataSourceProxyConfiguration.class)
@EnableConfigurationProperties(DataSourceDecoratorProperties.class)
public class QueryListeningConfiguration {

	@Bean
	public static DataSourceDecoratorBeanPostProcessor dataSourceDecoratorBeanPostProcessor() {
		return new DataSourceDecoratorBeanPostProcessor();
	}

	@Bean
	@ConditionalOnMissingBean
	public DataSourceNameResolver dataSourceNameResolver(ApplicationContext applicationContext) {
		return new DataSourceNameResolver(applicationContext);
	}

	@Bean
	public TestQueryListener threadQueryExecutionListener() {
		return new TestQueryListener();
	}

}
