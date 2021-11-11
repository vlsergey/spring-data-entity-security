package com.github.vlsergey.springdata.entitysecurity.owned;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ContextConfiguration;

import com.github.vlsergey.springdata.entitysecurity.TestQueryListener;

@DataJpaTest
@ContextConfiguration(classes = TestConfiguration.class)
class OwnedEntityTest {

	private static final String USERNAME_ROOT = "root";

	@Autowired
	private TestQueryListener queryListener;

	@Autowired
	private OwnedTestEntityRepository testRepository;

	@AfterAll
	static void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@BeforeAll
	static void initSecurityContext() {
		SecurityContextHolder.setContext(new SecurityContextImpl());
	}

	private void assertWhenDoThenQueryMatchesPattern(Runnable runnable, String pattern) {
		final List<String> queries = queryListener.listen(runnable);
		assertThat(queries, Matchers.hasSize(1));
		assertThat(queries.get(0), Matchers.matchesPattern(pattern));
	}

	@Test
	void findAllByIdsUnderRootReturnsEmptyResultForEmptyArg() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken(USERNAME_ROOT, null, emptyList()));

		assertThat(testRepository.findAllById(emptyList()), emptyCollectionOf(OwnedTestEntity.class));
	}

	@Test
	void findAllByIdsUnderRootReturnsEmptyResultForUnknownIds() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken(USERNAME_ROOT, null, emptyList()));

		assertThat(testRepository.findAllById(singletonList(UUID.randomUUID())),
				emptyCollectionOf(OwnedTestEntity.class));
	}

	@Test
	void testDoubleSaveWithoutFlash() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("testUser", null, emptyList()));

		OwnedTestEntity test = new OwnedTestEntity();
		test.setOwner("testUser");
		test.setValue(42);

		test = testRepository.save(test);
		assertNotNull(test);

		OwnedTestEntity beforeSecondSave = test;
		List<String> queries = queryListener.listen(() -> {
			testRepository.save(beforeSecondSave);
		});

		assertThat(queries, hasSize(0));
	}

	@Test
	void testDoubleSaveWithFlash() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("testUser", null, emptyList()));

		OwnedTestEntity test = new OwnedTestEntity();
		test.setOwner("testUser");
		test.setValue(42);

		// flash in DB -- thus on second save DB check is mandatory
		// TODO: ignore second check if entity is not dirty
		test = testRepository.saveAndFlush(test);
		assertNotNull(test);

		OwnedTestEntity beforeSecondSave = test;
		List<String> queries = queryListener.listen(() -> {
			testRepository.save(beforeSecondSave);
		});

		assertThat(queries, hasSize(1));
		assertThat(queries.get(0),
				matchesPattern("^select 1 as .* from owned_test_entity .* where .*id=\\? and .*owner=\\?$"));
	}

	@Test
	void testExistsById() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("testUser", null, emptyList()));

		assertWhenDoThenQueryMatchesPattern(() -> testRepository.existsById(UUID.randomUUID()),
				"^select 1 as .* from owned_test_entity .* where .*id=\\? and .*owner=\\?$");
	}

	@Test
	void testFindAll() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("testUser", null, emptyList()));

		assertWhenDoThenQueryMatchesPattern(testRepository::findAll,
				"^select .* from owned_test_entity .* where .*owner=\\?$");
	}

	@Test
	void testFindAllUnderRoot() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken(USERNAME_ROOT, null, emptyList()));

		assertWhenDoThenQueryMatchesPattern(testRepository::findAll, "^select .* from owned_test_entity [a-z0-9_]+$");
	}

	@Test
	void testFindByValue() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("testUser", null, emptyList()));

		assertWhenDoThenQueryMatchesPattern(() -> testRepository.findByValue(42),
				"^select .* from owned_test_entity .* where .*value=\\? and .*owner=\\?$");
	}

}
