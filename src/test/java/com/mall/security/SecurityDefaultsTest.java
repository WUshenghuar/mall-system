package com.mall.security;

import com.mall.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityDefaultsTest {
    @Test
    void defaultConfigurationDoesNotEnableDevelopmentOrEmbedJwtSecret() throws Exception {
        var source = new YamlPropertySourceLoader()
                .load("application", new FileSystemResource("src/main/resources/application.yml"))
                .getFirst();

        assertThat(source.getProperty("spring.profiles.active")).isNull();
        assertThat(source.getProperty("jwt.secret")).isEqualTo("${JWT_SECRET:}");
        assertThat(source.getProperty("spring.datasource.password")).isEqualTo("${MYSQL_ROOT_PASSWORD:}");
        assertThat(source.getProperty("spring.rabbitmq.password")).isEqualTo("${RABBITMQ_PASSWORD:}");
        assertThat(source.getProperty("minio.secret-key")).isEqualTo("${MINIO_ROOT_PASSWORD:}");
        assertThat(source.getProperty("ai.service.token")).isEqualTo("${AI_SERVICE_TOKEN:}");
        assertThat(source.getProperty("trade.payment.simulation-enabled")).isEqualTo(false);
    }

    @Test
    void jwtProviderRejectsMissingOrWeakSecretWithActionableMessage() {
        assertThatThrownBy(() -> new JwtTokenProvider("short", 60_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
