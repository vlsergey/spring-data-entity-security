package io.github.vlsergey.springdata.entitysecurity.noquerydsl;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ContextConfiguration;

import io.github.vlsergey.springdata.entitysecurity.TestQueryListener;

@DataJpaTest
@ContextConfiguration(classes = TestConfiguration.class)
class RepositoryQueriesTest {

	@Autowired
	private FileTestEntityRepository fileRepository;

	@Autowired
	private TestQueryListener queryListener;

	@BeforeAll
	static void initSecurityContext() {
		SecurityContextHolder.setContext(new SecurityContextImpl());
	}

	@AfterAll
	static void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void testFindAll() {
		SecurityContextHolder.getContext().setAuthentication(new AbstractAuthenticationToken(emptyList()) {

			private static final long serialVersionUID = 1L;

			@Override
			public Object getPrincipal() {
				return "testUser";
			}

			@Override
			public Object getCredentials() {
				return "testUser";
			}
		});

		final List<String> queries = queryListener.listen(() -> fileRepository.findAll());
		assertThat(queries, Matchers.hasSize(1));
		assertThat(queries.get(0),
				Matchers.matchesPattern("^select .* " + "from file_test_entity .* " + "cross join user_test_entity .*"
						+ "inner join group_test_entity .* on .*owner_group_gid=.*gid "
						+ "inner join group_test_entity_users .* on .*gid=.*group_test_entity_gid "
						+ "inner join user_test_entity .* on .*users_uid=.*uid .*" + "where .*login=\\? and \\("
						+ "substring\\(.*permissions, 0, 1\\)=\\? and .*owner_user_uid=.*uid or "
						+ "substring\\(.*permissions, 3, 4\\)=\\? and .*\\(.*uid in \\(.*uid\\)\\) or "
						+ "substring\\(.*permissions, 6, 7\\)=\\?\\)$"));
	}

}
