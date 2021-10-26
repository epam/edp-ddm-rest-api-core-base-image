package com.epam.digital.data.platform.restapi.core.config;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@ContextConfiguration(classes = {
    TestDatabase.class,
    JooqConfig.class,
    JooqAutoConfiguration.class
})
public @interface TestConfiguration {
}
