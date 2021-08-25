package com.github.vlsergey.springdata.entitysecurity.owned;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;

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

	@Autowired
	private OwnedTestEntityRepository testRepository;

	@Autowired
	private TestQueryListener queryListener;

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
	void testFindAll() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("testUser", null, emptyList()));

		assertWhenDoThenQueryMatchesPattern(testRepository::findAll,
				"^select .* from owned_test_entity .* where .*owner=\\?$");
	}

	@Test
	void testFindAllUnderRoot() {
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("root", null, emptyList()));

		assertWhenDoThenQueryMatchesPattern(testRepository::findAll, "^select .* from owned_test_entity [a-z0-9_]+$");
	}

	@Test
	void testExistsById() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("testUser", null, emptyList()));

		assertWhenDoThenQueryMatchesPattern(() -> testRepository.existsById(UUID.randomUUID()),
				"^select count\\(.*id\\)>0 as .* from owned_test_entity .* where .*id=\\? and .*owner=\\?$");
	}

	@Test
	void testFindByValue() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("testUser", null, emptyList()));

		assertWhenDoThenQueryMatchesPattern(() -> testRepository.findByValue(42),
				"^select .* from owned_test_entity .* where .*value=\\? and .*owner=\\?$");
	}

}
