package de.elite12.discord_notify_proxy.config;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

class MyRuntimeHintsTest {

	@Test
	void registersHibernateValidatorMessageBundleInstanceField() {
		RuntimeHints hints = new RuntimeHints();
		new MyRuntimeHints().registerHints(hints, getClass().getClassLoader());

		assertThat(RuntimeHintsPredicates.reflection()
				.onFieldAccess(org.hibernate.validator.internal.util.logging.Messages_$bundle.class, "INSTANCE"))
				.accepts(hints);
	}
}
