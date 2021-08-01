package com.epam.digital.data.platform.restapi.core.config;

import com.epam.digital.data.platform.starter.security.PermitAllWebSecurityConfig;
import com.epam.digital.data.platform.starter.security.config.Whitelist;
import com.epam.digital.data.platform.starter.security.jwt.TokenProvider;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ComponentScan(basePackages = {"com.epam.digital.data.platform.starter.security.jwt"})
@Import({TokenProvider.class, PermitAllWebSecurityConfig.class, Whitelist.class})
public @interface SecurityConfiguration {

}
