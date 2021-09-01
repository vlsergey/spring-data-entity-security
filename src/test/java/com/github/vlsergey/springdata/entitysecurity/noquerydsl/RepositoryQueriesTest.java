package com.github.vlsergey.springdata.entitysecurity.noquerydsl;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

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
class RepositoryQueriesTest {

	@Autowired
	private FileTestEntityRepository fileRepository;

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

		assertWhenDoThenQueryMatchesPattern(fileRepository::findAll, "^select .* from file_test_entity .* "
				+ "where exists \\(" + "select 1 "
				+ "from user_test_entity .* inner join user_test_entity_groups .* on .*uid=.*user_test_entity_uid "
				+ "inner join group_test_entity .* on .*groups_gid=.*gid "
				+ "where .*login=\\? and \\(substring\\(.*permissions, 0, 1\\)=\\? and .*owner_user_uid=.*uid or "
				+ "substring\\(.*permissions, 3, 4\\)=\\? and \\(.*owner_group_gid in \\(groupteste3_.gid\\)\\) "
				+ "or substring\\(.*permissions, 6, 7\\)=\\?\\)" + "\\)$");
	}

	@Test
	void testFindAllUnderRoot() {
		SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("root", null, emptyList()));

		assertWhenDoThenQueryMatchesPattern(fileRepository::findAll, "^select .* from file_test_entity [a-z0-9_]+$");
	}

	@Test
	void testExistsById() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("testUser", null, emptyList()));

		assertWhenDoThenQueryMatchesPattern(() -> fileRepository.existsById("testFile"),
				"^select 1 as .* from file_test_entity .* where .*path=\\? and \\(exists \\(" + "select 1 "
						+ "from user_test_entity .* inner join user_test_entity_groups .* on .*uid=.*user_test_entity_uid "
						+ "inner join group_test_entity .* on .*groups_gid=.*gid "
						+ "where .*login=\\? and \\(substring\\(.*permissions, 0, 1\\)=\\? and .*owner_user_uid=.*uid or "
						+ "substring\\(.*permissions, 3, 4\\)=\\? and \\(.*owner_group_gid in \\(groupteste3_.gid\\)\\) "
						+ "or substring\\(.*permissions, 6, 7\\)=\\?\\)" + "\\)\\)$");
	}

}
