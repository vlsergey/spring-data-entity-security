package com.github.vlsergey.springdata.entitysecurity.bughhh14815;

import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = "logging.level.org.hibernate.query.criteria.internal=DEBUG")
@ContextConfiguration(classes = TestConfiguration.class)
class HibernateBugTest {

	@Autowired
	private EntityManager entityManager;

	@Test
	void itShouldBeOkayToChangeConditionBetweenCreateQueryCalls() {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<FirstEntity> query = cb.createQuery(FirstEntity.class);
		final Root<FirstEntity> root = query.from(FirstEntity.class);
		query.select(root);

		// first condition (will be replaced after first execution)
		{
			final Subquery<SecondEntity> sq1 = query.subquery(SecondEntity.class);
			final Root<SecondEntity> sq1From = sq1.from(SecondEntity.class);
			sq1.select(sq1From);
			sq1.where(cb.equal(sq1From.get("secondValue"), root.get("firstValue")));
			query.where(cb.exists(sq1));
		}

		// Rendered criteria query -> select generatedAlias0 from FirstEntity as
		// generatedAlias0 where exists (select generatedAlias1 from SecondEntity as
		// generatedAlias1 where generatedAlias1.secondValue=generatedAlias0.firstValue)
		entityManager.createQuery(query);

		// replace condition with new instance with subquery
		{
			final Subquery<SecondEntity> sq2 = query.subquery(SecondEntity.class);
			final Root<SecondEntity> sq2From = sq2.from(SecondEntity.class);
			sq2.select(sq2From);
			sq2.where(cb.equal(root.get("firstId"), sq2From.get("secondId")));
			query.where(cb.exists(sq2));
		}

		// https://hibernate.atlassian.net/browse/HHH-14815
		assertThrows(IllegalArgumentException.class, () -> entityManager.createQuery(query));
		// Rendered criteria query -> select generatedAlias0 from FirstEntity as
		// generatedAlias0 where exists (select generatedAlias0 from SecondEntity as
		// ^^^^^^^^^^^^^^^
		// generatedAlias0 where generatedAlias0.firstId=generatedAlias0.secondId) }
		// ^^^^^^^^^^^^^^^
	}
}
