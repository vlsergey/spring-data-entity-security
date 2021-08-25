# spring-data-entity-security
Extension to Spring Data to add security filters to repositories

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

