# spring-data-entity-security
[![](https://jitpack.io/v/vlsergey/spring-data-entity-security.svg)](https://jitpack.io/#vlsergey/spring-data-entity-security)
Extension to Spring Data to add security filters to repositories

- ✨ Adds security conditions to all standard JPA methods from JpaRepository and JpaSpecificationExecutor
- ✨ Also adds conditions to QuerydslPredicateExecutor if querydsl is enabled for the repository
- ✨ Injects security conditions into queries generated from user-specific methods in JpaRepository (like `findBySomeField`)


- 🚧 Find-by-example is not implemented (will throw `UnsupportedOperationException`)
- 🚧 Compound IDs will not work for all operations


- ⚠️ Only direct work with JPA repository is affected. Thus, any code working with EntityRepository will not be affected.
- ⚠️ Also, any links from one entity to another (`@OneToOne`, `@ManyToOne`, `@OneToMany`, `@ManyToMany`) are not affected. The code will receive entities without security filtering using such link methods.  

Examples and test-cases:
* [Simple entity with `owner` field, but `root` is allowed to see all entities](https://github.com/vlsergey/spring-data-entity-security/tree/master/src/test/java/com/github/vlsergey/springdata/entitysecurity/owned)
* [File-alike permission check with owner user and group](https://github.com/vlsergey/spring-data-entity-security/tree/master/src/test/java/com/github/vlsergey/springdata/entitysecurity/noquerydsl)

## Installation

### Gradle

Add the JitPack repository to your `build.gradle`:

```grooxmlvy
repositories {
    maven {
        url = uri("https://jitpack.io")
    }
}
```

Add package as a dependency:

```groovy
dependencies {
    implementation group: 'com.github.vlsergey', name: 'spring-data-entity-security', version: '${version}'
}
```

### Maven
Add the JitPack repository to your build file:

```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

Step 2. Add the dependency

```xml
    <dependency>
        <groupId>com.github.vlsergey</groupId>
        <artifactId>spring-data-entity-security</artifactId>
        <version>${version}</version>
    </dependency>
```

## Usage

Add `repositoryFactoryBeanClass` parameter to your `@EnableJpaRepositories` annotation:
```java
@EnableJpaRepositories(value = "com.mycompany.data",
    repositoryFactoryBeanClass = com.github.vlsergey.springdata.entitysecurity.SecuredJpaRepositoryFactoryBean.class)
```

For each repository you want to enforce entity security implement `SecurityMixin` (that describes details of how to build security constrains for each domain entity) and add `@SecuredWith` annotation to repository interface.

If querydsl is used implement `SecurityMixinWithQuerydsl` instead of `SecurityMixin` for such repository.

