# spring-data-entity-security
Extension to Spring Data to add security filters to repositories

## Installation

Add GitHub package repository to `build.gradle`:
```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/vlsergey/spring-data-entity-security")
    }
}
```

Add package as a dependency:
```groovy
dependencies {
    implementation group: 'io.github.vlsergey', name: 'spring-data-entity-security', version: '0.0.1'
}```


Add `repositoryFactoryBeanClass` parameter to your `@EnableJpaRepositories` annotation:
```java
@EnableJpaRepositories(value = "com.mycompany.data",
    repositoryFactoryBeanClass = io.github.vlsergey.springdata.entitysecurity.SecuredJpaRepositoryFactoryBean.class)
```

For each repository you want to enforce entity security implement `SecurityMixin` (that describes details of how to build security constrains for each domain entity) and add `@SecuredWith` annotation to repository interface.

If querydsl is used implement `SecurityMixinWithQuerydsl` instead of `SecurityMixin` for such repository.

