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

	@Test
	void registersConstructorsForUsedHibernateConstraintValidators() throws NoSuchMethodException {
		RuntimeHints hints = new RuntimeHints();
		new MyRuntimeHints().registerHints(hints, getClass().getClassLoader());

		assertThat(RuntimeHintsPredicates.reflection()
				.onConstructorInvocation(org.hibernate.validator.internal.constraintvalidators.bv.NotBlankValidator.class.getDeclaredConstructor()))
				.accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection()
				.onConstructorInvocation(org.hibernate.validator.internal.constraintvalidators.bv.NotNullValidator.class.getDeclaredConstructor()))
				.accepts(hints);
	}
}
