package com.mall.common.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** 开发环境切换 Tomcat 连接器实现，生产配置不受影响。 */
@Configuration
@Profile("dev")
public class DevTomcatConfig {
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> useNio2ForWindowsDev() {
        return factory -> factory.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");
    }
}
