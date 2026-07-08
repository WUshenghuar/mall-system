package com.mall.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mallOpenAPI() {
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("登录获取 Token 后填入此处");

        return new OpenAPI()
                .info(new Info()
                        .title("B2C 跨境电商后台管理系统 API")
                        .version("1.0.0")
                        .description("商品、订单、会员、营销、财务五大模块的 REST API 文档")
                        .contact(new Contact()
                                .name("Yantao Liu")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .schemaRequirement("Bearer", jwtScheme);
    }
}
