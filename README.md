# spring-data-entity-security
[![](https://jitpack.io/v/vlsergey/spring-data-entity-security.svg)](https://jitpack.io/#vlsergey/spring-data-entity-security)
Extension to Spring Data to add security filters to repositories

- ✨ Adds security conditions to all standard JPA methods from JpaRepository and JpaSpecificationExecutor
- ✨ Also adds conditions to QuerydslPredicateExecutor is querydsl is enabled for repository
- ✨ Injects security conditions into queries generated from user-specific methods in JpaRepository (like `findBySomeField`)


- 🚧 find-by-example is not implemented (will throw `UnsupportedOperationException`)
- 🚧 Compound IDs will not work for all operations

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
    implementation group: 'com.github.vlsergey', name: 'spring-data-entity-security', version: '0.1.0'
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
        <version>0.1.0</version>
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

