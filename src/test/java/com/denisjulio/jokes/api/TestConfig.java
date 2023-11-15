package com.denisjulio.jokes.api;

import com.denisjulio.jokes.api.utils.KeycloakClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KeycloakClientProperties.class)
public class TestConfig {
}
