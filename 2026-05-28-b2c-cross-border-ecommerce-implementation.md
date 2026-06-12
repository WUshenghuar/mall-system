# B2C 跨境电商后台管理系统 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一套完整的 B2C 跨境电商后台管理系统，含商品管理（SPU/SKU）、订单流转、会员管理、营销优惠券、财务对账 5 大模块，基于 RBAC 权限控制 + 高并发架构（Redis/MQ/ES）。

**Architecture:** 前后端分离的模块化单体架构（Modular Monolith）。后端按业务领域分包（mall-product、mall-order、mall-member、mall-marketing、mall-finance、mall-search），共享统一的 mall-security 和 mall-common 基础模块。前端按角色动态路由，基于 Ant Design Vue 构建电商风格管理后台 SPA。

**Tech Stack:** Java 8 + Spring Boot 2.7 + Spring Security + JWT + MyBatis-Plus 3.5 + MySQL 8 + Redis 7 + RabbitMQ + Elasticsearch 8 + Vue 3 (Composition API) + Ant Design Vue + Maven + Git

---

## 文件结构总览

```
mall-system/                            # 后端根项目 (Maven)
├── mall-common/                        # 公共模块
│   └── src/main/java/com/mall/common/
│       ├── entity/                     # BaseEntity 基类
│       ├── result/                     # Result, ResultCode
│       ├── exception/                  # GlobalExceptionHandler, BusinessException
│       └── config/                     # MyBatisPlusConfig, MetaObjectHandler
├── mall-security/                      # 安全模块
│   └── src/main/java/com/mall/security/
│       ├── jwt/                        # JwtTokenProvider, JwtAuthenticationFilter
│       ├── config/                     # SecurityConfig
│       ├── annotation/                 # @DataScope
│       ├── aspect/                     # DataScopeAspect, DataScopeContext
│       └── user/                       # LoginUser, UserDetailsServiceImpl
├── mall-product/                       # 商品模块
│   └── src/main/java/com/mall/product/
│       ├── controller/                 # SpuController, SkuController, CategoryController, BrandController
│       ├── service/                    # SpuService, SkuService, CategoryService, BrandService
│       ├── mapper/                     # SpuMapper, SkuMapper, CategoryMapper, BrandMapper
│       └── entity/                     # Spu, Sku, Category, Brand, SkuStock
├── mall-order/                         # 订单模块
│   └── src/main/java/com/mall/order/
│       ├── controller/                 # OrderController, RefundController
│       ├── service/                    # OrderService, RefundService, LogisticsService
│       ├── mapper/                     # OrderMapper, OrderItemMapper, RefundMapper
│       ├── entity/                     # Order, OrderItem, OrderPay, OrderRefund, OrderLogistics
│       └── mq/                         # OrderMessageListener, OrderMessageSender
├── mall-member/                        # 会员模块
│   └── src/main/java/com/mall/member/
│       ├── controller/                 # MemberController
│       ├── service/                    # MemberService, PointsService
│       ├── mapper/                     # MemberMapper, MemberAddrMapper
│       └── entity/                     # Member, MemberAddr, MemberPointsLog
├── mall-marketing/                     # 营销模块
│   └── src/main/java/com/mall/marketing/
│       ├── controller/                 # CouponController, ActivityController
│       ├── service/                    # CouponService, ActivityService, SeckillService
│       ├── mapper/                     # CouponMapper, ActivityMapper
│       └── entity/                     # Coupon, CouponIssue, Activity, ActivitySku
├── mall-finance/                       # 财务模块
│   └── src/main/java/com/mall/finance/
│       ├── controller/                 # StatementController, TaxConfigController
│       ├── service/                    # StatementService, TaxConfigService, ExchangeRateService
│       ├── mapper/                     # StatementMapper, StatementItemMapper, TaxConfigMapper
│       └── entity/                     # Statement, StatementItem, TaxConfig
├── mall-search/                        # 搜索模块
│   └── src/main/java/com/mall/search/
│       ├── service/                    # ProductSearchService, IndexService
│       └── job/                        # ProductIndexJob (ES 同步)
├── mall-web/                           # Web 启动层
│   ├── MallApplication.java
│   ├── controller/                     # 各模块 Controller (实际放在此处)
│   │   ├── product/
│   │   ├── order/
│   │   ├── member/
│   │   ├── marketing/
│   │   ├── finance/
│   │   └── system/                     # AuthController, UserController, RoleController, MenuController
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── mapper/                     # MyBatis-Plus XML 映射
├── pom.xml                             # 父 POM

mall-web/                               # 前端项目
├── src/
│   ├── api/                            # API 接口封装
│   │   ├── product.js
│   │   ├── order.js
│   │   ├── member.js
│   │   ├── marketing.js
│   │   ├── finance.js
│   │   └── system.js
│   ├── layouts/
│   │   └── AdminLayout.vue
│   ├── router/
│   │   └── index.js
│   ├── store/
│   │   └── user.js
│   ├── utils/
│   │   ├── request.js
│   │   └── permission.js
│   ├── views/
│   │   ├── login/index.vue
│   │   ├── dashboard/index.vue
│   │   └── ... (按模块分页)
│   └── components/
│       └── Sidebar.vue
├── package.json
└── vite.config.js
```

---

## Phase 1：项目脚手架搭建

### Task 1.1：创建 Maven 父项目与模块结构

**Files:**
- Create: `mall-system/pom.xml`
- Create: `mall-system/mall-common/pom.xml`
- Create: `mall-system/mall-security/pom.xml`
- Create: `mall-system/mall-product/pom.xml`
- Create: `mall-system/mall-order/pom.xml`
- Create: `mall-system/mall-member/pom.xml`
- Create: `mall-system/mall-marketing/pom.xml`
- Create: `mall-system/mall-finance/pom.xml`
- Create: `mall-system/mall-search/pom.xml`
- Create: `mall-system/mall-web/pom.xml`

- [ ] **Step 1：创建父 POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.mall</groupId>
    <artifactId>mall-system</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>mall-common</module>
        <module>mall-security</module>
        <module>mall-product</module>
        <module>mall-order</module>
        <module>mall-member</module>
        <module>mall-marketing</module>
        <module>mall-finance</module>
        <module>mall-search</module>
        <module>mall-web</module>
    </modules>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>1.8</java.version>
        <mybatis-plus.version>3.5.3.1</mybatis-plus.version>
        <jjwt.version>0.12.3</jjwt.version>
        <elasticsearch.version>8.11.0</elasticsearch.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>easyexcel</artifactId>
            <version>3.3.2</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2：创建各子模块 POM**

每个业务模块均依赖 mall-common。mall-web 额外依赖 mall-security 和所有业务模块。

`mall-common/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.mall</groupId>
        <artifactId>mall-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>mall-common</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>
</project>
```

`mall-security/pom.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.mall</groupId>
        <artifactId>mall-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>mall-security</artifactId>
    <dependencies>
        <dependency><groupId>com.mall</groupId><artifactId>mall-common</artifactId><version>1.0.0</version></dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
```

`mall-web/pom.xml`（依赖所有模块）:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.mall</groupId>
        <artifactId>mall-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>mall-web</artifactId>
    <dependencies>
        <dependency><groupId>com.mall</groupId><artifactId>mall-common</artifactId><version>1.0.0</version></dependency>
        <dependency><groupId>com.mall</groupId><artifactId>mall-security</artifactId><version>1.0.0</version></dependency>
        <dependency><groupId>com.mall</groupId><artifactId>mall-product</artifactId><version>1.0.0</version></dependency>
        <dependency><groupId>com.mall</groupId><artifactId>mall-order</artifactId><version>1.0.0</version></dependency>
        <dependency><groupId>com.mall</groupId><artifactId>mall-member</artifactId><version>1.0.0</version></dependency>
        <dependency><groupId>com.mall</groupId><artifactId>mall-marketing</artifactId><version>1.0.0</version></dependency>
        <dependency><groupId>com.mall</groupId><artifactId>mall-finance</artifactId><version>1.0.0</version></dependency>
        <dependency><groupId>com.mall</groupId><artifactId>mall-search</artifactId><version>1.0.0</version></dependency>
    </dependencies>
</project>
```

其余子模块（mall-product、mall-order、mall-member、mall-marketing、mall-finance、mall-search）POM 模板:
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.mall</groupId>
        <artifactId>mall-system</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>mall-xxx</artifactId>
    <dependencies>
        <dependency><groupId>com.mall</groupId><artifactId>mall-common</artifactId><version>1.0.0</version></dependency>
    </dependencies>
</project>
```

- [ ] **Step 3：创建 Spring Boot 启动类**

`mall-web/src/main/java/com/mall/web/MallApplication.java`:
```java
package com.mall.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mall")
public class MallApplication {
    public static void main(String[] args) {
        SpringApplication.run(MallApplication.class, args);
    }
}
```

- [ ] **Step 4：创建 application.yml 和 application-dev.yml**

`mall-web/src/main/resources/application.yml`:
```yaml
server:
  port: 8080

spring:
  profiles:
    active: dev
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mall_system?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
  redis:
    host: localhost
    port: 6379
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.mall.**.entity
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
    banner: false
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# ES 配置
elasticsearch:
  hosts: localhost:9200

# JWT
jwt:
  secret: mall-system-secret-key-2026-b2c-b2b
```

`mall-web/src/main/resources/application-dev.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mall_system?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
```

- [ ] **Step 5：验证项目可编译**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6：Commit**

```bash
git init mall-system && cd mall-system
git add pom.xml mall-common/pom.xml mall-security/pom.xml mall-product/pom.xml mall-order/pom.xml mall-member/pom.xml mall-marketing/pom.xml mall-finance/pom.xml mall-search/pom.xml mall-web/pom.xml mall-web/src/
git commit -m "feat: init Maven multi-module project scaffold"
```

---

### Task 1.2：初始化前端 Vue 3 项目

**Files:**
- Create: `mall-web/package.json`
- Create: `mall-web/vite.config.js`
- Create: `mall-web/index.html`
- Create: `mall-web/src/main.js`
- Create: `mall-web/src/App.vue`

- [ ] **Step 1：用 Vite 创建 Vue 3 项目**

```bash
cd mall-system
npm create vite@latest mall-web -- --template vue
cd mall-web
npm install
npm install ant-design-vue @ant-design/icons-vue axios pinia vue-router@4
```

- [ ] **Step 2：配置 vite.config.js**

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': '/src'
    }
  }
})
```

- [ ] **Step 3：配置 main.js**

```javascript
import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'

const app = createApp(App)
app.use(Antd)
app.mount('#app')
```

- [ ] **Step 4：Commit**

```bash
cd ..
git add mall-web/
git commit -m "feat: init Vue 3 + Ant Design Vue frontend scaffold"
```

---

## Phase 2：基础设施层 — 统一返回 + 异常处理 + 基础实体

### Task 2.1：创建统一返回结果与异常处理

**Files:**
- Create: `mall-common/src/main/java/com/mall/common/result/ResultCode.java`
- Create: `mall-common/src/main/java/com/mall/common/result/Result.java`
- Create: `mall-common/src/main/java/com/mall/common/exception/BusinessException.java`
- Create: `mall-common/src/main/java/com/mall/common/exception/GlobalExceptionHandler.java`
- Create: `mall-common/src/main/java/com/mall/common/entity/BaseEntity.java`
- Create: `mall-common/src/main/java/com/mall/common/config/MyMetaObjectHandler.java`
- Create: `mall-common/src/main/java/com/mall/common/config/MyBatisPlusConfig.java`

- [ ] **Step 1：创建 ResultCode 枚举**

```java
package com.mall.common.result;

public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    VALIDATE_FAILED(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
```

- [ ] **Step 2：创建统一返回 Result**

```java
package com.mall.common.result;

import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;

@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    private Result() {}

    public static <T> Result<T> success(T data) {
        return new Result<T>()
            .setCode(ResultCode.SUCCESS.getCode())
            .setMessage(ResultCode.SUCCESS.getMessage())
            .setData(data)
            .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> failed(String message) {
        return new Result<T>()
            .setCode(ResultCode.FAILED.getCode())
            .setMessage(message)
            .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<T>()
            .setCode(ResultCode.FORBIDDEN.getCode())
            .setMessage(message)
            .setTimestamp(System.currentTimeMillis());
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<T>()
            .setCode(ResultCode.UNAUTHORIZED.getCode())
            .setMessage(message)
            .setTimestamp(System.currentTimeMillis());
    }
}
```

- [ ] **Step 3：创建 BusinessException**

```java
package com.mall.common.exception;

public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(500, message);
    }

    public int getCode() { return code; }
}
```

- [ ] **Step 4：创建 GlobalExceptionHandler**

```java
package com.mall.common.exception;

import com.mall.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.failed(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result handleAccessDenied(AccessDeniedException e) {
        return Result.forbidden("权限不足，无法访问");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .reduce((a, b) -> a + "; " + b)
            .orElse("参数校验失败");
        return Result.failed(msg);
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return Result.failed("系统繁忙，请稍后重试");
    }
}
```

- [ ] **Step 5：创建 BaseEntity 基类**

```java
package com.mall.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 6：创建 MyBatis-Plus 自动填充处理器**

```java
package com.mall.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

- [ ] **Step 7：创建 MyBatis-Plus 分页配置**

```java
package com.mall.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 8：Commit**

```bash
git add mall-common/src/
git commit -m "feat: add unified response, global exception handler, base entity"
```

---

## Phase 3：RBAC 权限体系 — Spring Security + JWT

### Task 3.1：数据库初始化脚本

**Files:**
- Create: `mall-web/src/main/resources/schema.sql`
- Create: `mall-web/src/main/resources/data.sql`

- [ ] **Step 1：编写建表 SQL（schema.sql）**

```sql
CREATE DATABASE IF NOT EXISTS mall_system DEFAULT CHARSET utf8mb4;
USE mall_system;

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    real_name   VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    avatar      VARCHAR(255),
    status      TINYINT      DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    role_name   VARCHAR(50)  NOT NULL,
    role_key    VARCHAR(50)  NOT NULL UNIQUE,
    role_sort   INT          DEFAULT 0,
    status      TINYINT      DEFAULT 1,
    remark      VARCHAR(255),
    create_time DATETIME,
    update_time DATETIME,
    deleted     TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 菜单/权限表
CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    menu_name   VARCHAR(50)  NOT NULL,
    parent_id   BIGINT       DEFAULT 0,
    order_num   INT          DEFAULT 0,
    path        VARCHAR(200),
    component   VARCHAR(255),
    perms       VARCHAR(100),
    icon        VARCHAR(50),
    menu_type   TINYINT      COMMENT 'M目录 C菜单 F按钮',
    visible     TINYINT      DEFAULT 1,
    status      TINYINT      DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户-角色关联
CREATE TABLE IF NOT EXISTS sys_user_role (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色-菜单关联
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2：编写初始数据 SQL（data.sql）**

```sql
USE mall_system;

-- 预置角色 (店长/运营/客服/财务)
INSERT IGNORE INTO sys_role VALUES
(1, '店长',       'store_manager',   1, 1, '全部权限', NOW(), NOW(), 0),
(2, '运营专员',   'ops_specialist',  2, 1, '商品与活动运营', NOW(), NOW(), 0),
(3, '客服专员',   'cs_specialist',   3, 1, '订单与退款处理', NOW(), NOW(), 0),
(4, '财务专员',   'finance',         4, 1, '查看对账单', NOW(), NOW(), 0);

-- 默认管理员账号 (密码为 admin123 的 BCrypt 加密占位 - 需运行时生成)
INSERT IGNORE INTO sys_user VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', '13800000000', 'admin@mall.com', NULL, 1, NOW(), NOW(), 0),
(2, 'ops',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '运营专员',   '13800000001', 'ops@mall.com',   NULL, 1, NOW(), NOW(), 0),
(3, 'cs',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '客服专员',   '13800000002', 'cs@mall.com',    NULL, 1, NOW(), NOW(), 0),
(4, 'finance','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '财务专员',   '13800000003', 'finance@mall.com',NULL, 1, NOW(), NOW(), 0);

-- 分配角色
INSERT IGNORE INTO sys_user_role VALUES
(1, 1, 1),   -- admin → 店长
(2, 2, 2),   -- ops → 运营专员
(3, 3, 3),   -- cs → 客服专员
(4, 4, 4);   -- finance → 财务专员
```

- [ ] **Step 3：配置 SQL 自动执行**

在 `application.yml` 中添加:
```yaml
spring:
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql
```

- [ ] **Step 4：Commit**

```bash
git add mall-web/src/main/resources/
git commit -m "feat: add database schema and seed data"
```

---

### Task 3.2：JWT 认证与 Spring Security 配置

**Files:**
- Create: `mall-security/src/main/java/com/mall/security/jwt/JwtTokenProvider.java`
- Create: `mall-security/src/main/java/com/mall/security/jwt/JwtAuthenticationFilter.java`
- Create: `mall-security/src/main/java/com/mall/security/config/SecurityConfig.java`
- Create: `mall-security/src/main/java/com/mall/security/user/LoginUser.java`
- Create: `mall-security/src/main/java/com/mall/security/user/UserDetailsServiceImpl.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/system/AuthController.java`
- Create: `mall-system/src/main/java/com/mall/system/entity/SysUser.java`
- Create: `mall-system/src/main/java/com/mall/system/mapper/SysUserMapper.java`
- Create: `mall-system/src/main/java/com/mall/system/mapper/SysMenuMapper.java`
- Create: `mall-web/src/main/resources/mapper/system/SysMenuMapper.xml`

- [ ] **Step 1：创建 LoginUser 实现 UserDetails**

```java
package com.mall.security.user;

import com.mall.system.entity.SysUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class LoginUser implements UserDetails {
    private final Long userId;
    private final String username;
    private final String password;
    private final String realName;
    private final List<String> permissions;

    public LoginUser(SysUser user, List<String> permissions) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.realName = user.getRealName();
        this.permissions = permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
```

- [ ] **Step 2：创建 JwtTokenProvider**

```java
package com.mall.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long expiration = 86400000L; // 24h

    public JwtTokenProvider(@Value("${jwt.secret:mall-system-secret-key-2026}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
            .subject(username)
            .claim("userId", userId)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expiration))
            .signWith(key)
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        return parseToken(token).get("userId", Long.class);
    }
}
```

- [ ] **Step 3：创建 JwtAuthenticationFilter**

```java
package com.mall.security.jwt;

import com.mall.security.user.LoginUser;
import com.mall.security.user.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            LoginUser loginUser = userDetailsService.loadByUserId(userId);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 4：创建 UserDetailsServiceImpl**

```java
package com.mall.security.user;

import com.mall.system.entity.SysUser;
import com.mall.system.mapper.SysUserMapper;
import com.mall.system.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public LoginUser loadUserByUsername(String username) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        List<String> permissions = menuMapper.selectPermsByUserId(user.getId());
        return new LoginUser(user, permissions);
    }

    public LoginUser loadByUserId(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        List<String> permissions = menuMapper.selectPermsByUserId(userId);
        return new LoginUser(user, permissions);
    }
}
```

- [ ] **Step 5：创建 Spring Security 配置**

```java
package com.mall.security.config;

import com.mall.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
            .antMatchers("/api/auth/login").permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 6：创建 SysUser 实体和 Mapper**

```java
// SysUser.java — mall-system 模块 (若没有 mall-system module, 放在 mall-web 下)
package com.mall.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private Integer status;
}

// SysUserMapper.java
package com.mall.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.system.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser selectByUsername(@Param("username") String username);
}

// SysMenuMapper.java
package com.mall.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.system.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {
    List<String> selectPermsByUserId(@Param("userId") Long userId);
}
```

- [ ] **Step 7：编写 SysMenuMapper.xml**

`mall-web/src/main/resources/mapper/system/SysMenuMapper.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.mall.system.mapper.SysMenuMapper">
    <select id="selectPermsByUserId" resultType="java.lang.String">
        SELECT DISTINCT m.perms
        FROM sys_menu m
        INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
        INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND m.status = 1
          AND m.perms IS NOT NULL
          AND m.perms != ''
    </select>
</mapper>
```

- [ ] **Step 8：创建 AuthController**

```java
package com.mall.web.controller.system;

import com.mall.common.exception.BusinessException;
import com.mall.common.result.Result;
import com.mall.security.jwt.JwtTokenProvider;
import com.mall.security.user.LoginUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
            LoginUser loginUser = (LoginUser) auth.getPrincipal();
            String token = jwtTokenProvider.generateToken(loginUser.getUserId(), loginUser.getUsername());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", loginUser.getUserId());
            data.put("realName", loginUser.getRealName());
            data.put("permissions", loginUser.getPermissions());
            return Result.success(data);
        } catch (BadCredentialsException e) {
            throw new BusinessException(401, "用户名或密码错误");
        }
    }

    @GetMapping("/userinfo")
    public Result getUserInfo(Authentication auth) {
        LoginUser loginUser = (LoginUser) auth.getPrincipal();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", loginUser.getUserId());
        data.put("username", loginUser.getUsername());
        data.put("realName", loginUser.getRealName());
        data.put("permissions", loginUser.getPermissions());
        return Result.success(data);
    }

    @Data
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }
}
```

- [ ] **Step 9：启动并测试登录**

Run: `mvn spring-boot:run -pl mall-web -am`

Test:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```
Expected: 返回包含 token、userId、permissions 的 JSON。

- [ ] **Step 10：Commit**

```bash
git add mall-security/src/ mall-web/src/main/java/com/mall/web/controller/system/ mall-web/src/main/resources/mapper/
git commit -m "feat: implement JWT auth and Spring Security RBAC"
```

---

### Task 3.3：数据权限注解与 AOP 切面

**Files:**
- Create: `mall-security/src/main/java/com/mall/security/annotation/DataScope.java`
- Create: `mall-security/src/main/java/com/mall/security/aspect/DataScopeAspect.java`
- Create: `mall-security/src/main/java/com/mall/security/aspect/DataScopeContext.java`

- [ ] **Step 1：创建 @DataScope 注解**

```java
package com.mall.security.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    String deptAlias() default "";
    String userAlias() default "";
}
```

- [ ] **Step 2：创建 DataScopeContext（ThreadLocal）**

```java
package com.mall.security.aspect;

public class DataScopeContext {
    private static final ThreadLocal<Long> DEPT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> DEPT_ALIAS = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ALIAS = new ThreadLocal<>();

    public static void setDeptId(Long deptId) { DEPT_ID.set(deptId); }
    public static Long getDeptId() { return DEPT_ID.get(); }
    public static void setUserId(Long userId) { USER_ID.set(userId); }
    public static Long getUserId() { return USER_ID.get(); }
    public static void setDeptAlias(String alias) { DEPT_ALIAS.set(alias); }
    public static String getDeptAlias() { return DEPT_ALIAS.get(); }
    public static void setUserAlias(String alias) { USER_ALIAS.set(alias); }
    public static String getUserAlias() { return USER_ALIAS.get(); }

    public static void clear() {
        DEPT_ID.remove();
        USER_ID.remove();
        DEPT_ALIAS.remove();
        USER_ALIAS.remove();
    }
}
```

- [ ] **Step 3：创建 DataScopeAspect 切面**

```java
package com.mall.security.aspect;

import com.mall.security.annotation.DataScope;
import com.mall.security.user.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint point, DataScope dataScope) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser)) return;

        LoginUser loginUser = (LoginUser) auth.getPrincipal();
        boolean isAdmin = loginUser.getPermissions().stream()
            .anyMatch(p -> p.equals("*:*:*"));

        if (isAdmin) return; // 管理员不限制数据范围

        DataScopeContext.setUserId(loginUser.getUserId());
        DataScopeContext.setDeptAlias(dataScope.deptAlias());
        DataScopeContext.setUserAlias(dataScope.userAlias());
    }
}
```

- [ ] **Step 4：Commit**

```bash
git add mall-security/src/
git commit -m "feat: add DataScope annotation and AOP interceptor"
```

---

## Phase 4：商品模块

### Task 4.1：分类管理

**Files:**
- Create: `mall-product/src/main/java/com/mall/product/entity/Category.java`
- Create: `mall-product/src/main/java/com/mall/product/mapper/CategoryMapper.java`
- Create: `mall-product/src/main/java/com/mall/product/service/CategoryService.java`
- Create: `mall-product/src/main/java/com/mall/product/service/impl/CategoryServiceImpl.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/product/CategoryController.java`

- [ ] **Step 1：创建 Category 实体**

```java
package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_category")
public class Category extends BaseEntity {
    private String categoryName;
    private Long parentId;
    private Integer level;
    private String icon;
    private Integer orderNum;
    private Integer status;
}
```

- [ ] **Step 2：创建 CategoryMapper**

```java
package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.entity.Category;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface CategoryMapper extends BaseMapper<Category> {
    @Select("SELECT * FROM pm_category WHERE deleted = 0 ORDER BY order_num ASC")
    List<Category> selectAll();
}
```

- [ ] **Step 3：创建 CategoryService**

```java
package com.mall.product.service;

import com.mall.product.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> selectTree();
    Category getById(Long id);
    void save(Category category);
    void update(Category category);
    void delete(Long id);
}
```

- [ ] **Step 4：创建 CategoryServiceImpl**

```java
package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.exception.BusinessException;
import com.mall.product.entity.Category;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;

    @Override
    public List<Category> selectTree() {
        return categoryMapper.selectAll();
    }

    @Override
    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public void save(Category category) {
        categoryMapper.insert(category);
    }

    @Override
    public void update(Category category) {
        categoryMapper.updateById(category);
    }

    @Override
    public void delete(Long id) {
        Long count = categoryMapper.selectCount(
            Wrappers.<Category>lambdaQuery().eq(Category::getParentId, id));
        if (count > 0) throw new BusinessException("请先删除子分类");
        categoryMapper.deleteById(id);
    }
}
```

- [ ] **Step 5：创建 CategoryController**

```java
package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.product.entity.Category;
import com.mall.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/product/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result tree() {
        return Result.success(categoryService.selectTree());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result get(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result add(@Valid @RequestBody Category category) {
        categoryService.save(category);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result update(@PathVariable Long id, @Valid @RequestBody Category category) {
        category.setId(id);
        categoryService.update(category);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:category:config')")
    public Result delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success(null);
    }
}
```

- [ ] **Step 6：Commit**

```bash
git add mall-product/src/ mall-web/src/main/java/com/mall/web/controller/product/
git commit -m "feat: implement category CRUD with tree structure"
```

---

### Task 4.2：商品 SPU 管理

**Files:**
- Create: `mall-product/src/main/java/com/mall/product/entity/Spu.java`
- Create: `mall-product/src/main/java/com/mall/product/mapper/SpuMapper.java`
- Create: `mall-product/src/main/java/com/mall/product/service/SpuService.java`
- Create: `mall-product/src/main/java/com/mall/product/service/impl/SpuServiceImpl.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/product/SpuController.java`

- [ ] **Step 1：创建 Spu 实体**

```java
package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_spu")
public class Spu extends BaseEntity {
    private String spuCode;
    private String spuName;
    private Long categoryId;
    private Long brandId;
    private String description;    // JSON: {"en":"...","zh":"...","fr":"..."}
    private String customsCode;    // HS Code
    private String originCountry;
    private Integer status;        // 0草稿 1上架 2下架
    private Integer salesCount;
}
```

- [ ] **Step 2：创建 SpuMapper**

```java
package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.entity.Spu;

public interface SpuMapper extends BaseMapper<Spu> {
}
```

- [ ] **Step 3：创建 SpuService**

```java
package com.mall.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.product.entity.Spu;

public interface SpuService {
    IPage<Spu> selectPage(Integer page, Integer size, Long categoryId, Integer status, String keyword);
    Spu getById(Long id);
    void save(Spu spu);
    void update(Spu spu);
    void delete(Long id);
    void publish(Long id, Integer status);
}
```

- [ ] **Step 4：创建 SpuServiceImpl**

```java
package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.product.entity.Spu;
import com.mall.product.mapper.SpuMapper;
import com.mall.product.service.SpuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SpuServiceImpl implements SpuService {
    private final SpuMapper spuMapper;

    @Override
    public IPage<Spu> selectPage(Integer page, Integer size, Long categoryId,
                                  Integer status, String keyword) {
        LambdaQueryWrapper<Spu> wrapper = Wrappers.<Spu>lambdaQuery()
            .eq(categoryId != null, Spu::getCategoryId, categoryId)
            .eq(status != null, Spu::getStatus, status)
            .like(StringUtils.hasText(keyword), Spu::getSpuName, keyword)
            .orderByDesc(Spu::getCreateTime);
        return spuMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Spu getById(Long id) {
        return spuMapper.selectById(id);
    }

    @Override
    public void save(Spu spu) {
        spuMapper.insert(spu);
    }

    @Override
    public void update(Spu spu) {
        spuMapper.updateById(spu);
    }

    @Override
    public void delete(Long id) {
        spuMapper.deleteById(id);
    }

    @Override
    public void publish(Long id, Integer status) {
        Spu spu = spuMapper.selectById(id);
        if (spu == null) throw new RuntimeException("SPU不存在");
        spu.setStatus(status);
        spuMapper.updateById(spu);
    }
}
```

- [ ] **Step 5：创建 SpuController**

```java
package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.product.entity.Spu;
import com.mall.product.service.SpuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/product/spu")
@RequiredArgsConstructor
public class SpuController {
    private final SpuService spuService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('product:spu:list')")
    public Result page(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size,
                       Long categoryId, Integer status, String keyword) {
        return Result.success(spuService.selectPage(page, size, categoryId, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:list')")
    public Result get(@PathVariable Long id) {
        return Result.success(spuService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:spu:add')")
    public Result add(@Valid @RequestBody Spu spu) {
        spuService.save(spu);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result update(@PathVariable Long id, @Valid @RequestBody Spu spu) {
        spu.setId(id);
        spuService.update(spu);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:delete')")
    public Result delete(@PathVariable Long id) {
        spuService.delete(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('product:spu:publish')")
    public Result publish(@PathVariable Long id, @RequestParam Integer status) {
        spuService.publish(id, status);
        return Result.success(null);
    }
}
```

- [ ] **Step 6：Commit**

```bash
git add mall-product/src/ mall-web/src/main/java/com/mall/web/controller/product/
git commit -m "feat: implement SPU CRUD with publish control"
```

---

### Task 4.3：SKU 管理 + Redis 库存缓存

**Files:**
- Create: `mall-product/src/main/java/com/mall/product/entity/Sku.java`
- Create: `mall-product/src/main/java/com/mall/product/entity/SkuStock.java`
- Create: `mall-product/src/main/java/com/mall/product/mapper/SkuMapper.java`
- Create: `mall-product/src/main/java/com/mall/product/service/SkuService.java`
- Create: `mall-product/src/main/java/com/mall/product/service/impl/SkuServiceImpl.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/product/SkuController.java`
- Create: `mall-common/src/main/java/com/mall/common/config/RedisConfig.java`

- [ ] **Step 1：创建 RedisConfig**

```java
package com.mall.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

- [ ] **Step 2：创建 Sku 实体**

```java
package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_sku")
public class Sku extends BaseEntity {
    private Long spuId;
    private String skuCode;
    private String attrs;          // JSON: [{"k":"颜色","v":"黑色"},{"k":"容量","v":"128GB"}]
    private BigDecimal price;      // USD
    private String currency;
    private BigDecimal costPrice;
    private BigDecimal weight;     // kg
    private String images;
    private Integer status;
}
```

- [ ] **Step 3：创建 SkuService**

```java
package com.mall.product.service;

import com.mall.product.entity.Sku;
import java.math.BigDecimal;
import java.util.List;

public interface SkuService {
    List<Sku> listBySpuId(Long spuId);
    Sku getById(Long id);
    void save(Sku sku);
    void update(Sku sku);
    void delete(Long id);
    void batchUpdatePrice(List<Long> skuIds, BigDecimal price);
    Integer getStock(Long skuId);         // 从 Redis 读
    void updateStock(Long skuId, Integer stock);  // 更新 Redis + DB
}
```

- [ ] **Step 4：创建 SkuServiceImpl（含 Redis 库存缓存）**

```java
package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.product.entity.Sku;
import com.mall.product.mapper.SkuMapper;
import com.mall.product.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SkuServiceImpl implements SkuService {
    private final SkuMapper skuMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STOCK_KEY = "stock:sku:";

    @Override
    public List<Sku> listBySpuId(Long spuId) {
        return skuMapper.selectList(
            Wrappers.<Sku>lambdaQuery().eq(Sku::getSpuId, spuId));
    }

    @Override
    public Sku getById(Long id) {
        return skuMapper.selectById(id);
    }

    @Override
    public void save(Sku sku) {
        skuMapper.insert(sku);
    }

    @Override
    public void update(Sku sku) {
        skuMapper.updateById(sku);
    }

    @Override
    public void delete(Long id) {
        skuMapper.deleteById(id);
        redisTemplate.delete(STOCK_KEY + id);
    }

    @Override
    public void batchUpdatePrice(List<Long> skuIds, BigDecimal price) {
        skuIds.forEach(id -> {
            Sku sku = skuMapper.selectById(id);
            if (sku != null) {
                sku.setPrice(price);
                skuMapper.updateById(sku);
            }
        });
    }

    @Override
    public Integer getStock(Long skuId) {
        Object val = redisTemplate.opsForValue().get(STOCK_KEY + skuId);
        if (val == null) return 0;
        return Integer.parseInt(val.toString());
    }

    @Override
    public void updateStock(Long skuId, Integer stock) {
        redisTemplate.opsForValue().set(STOCK_KEY + skuId, stock);
    }
}
```

- [ ] **Step 5：创建 SkuController**

```java
package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.product.entity.Sku;
import com.mall.product.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/product/sku")
@RequiredArgsConstructor
public class SkuController {
    private final SkuService skuService;

    @GetMapping("/list/{spuId}")
    @PreAuthorize("hasAuthority('product:sku:list')")
    public Result list(@PathVariable Long spuId) {
        return Result.success(skuService.listBySpuId(spuId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result add(@Valid @RequestBody Sku sku) {
        skuService.save(sku);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result update(@PathVariable Long id, @Valid @RequestBody Sku sku) {
        sku.setId(id);
        skuService.update(sku);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:spu:delete')")
    public Result delete(@PathVariable Long id) {
        skuService.delete(id);
        return Result.success(null);
    }

    @PutMapping("/price")
    @PreAuthorize("hasAuthority('product:sku:price')")
    public Result batchPrice(@RequestBody BatchPriceReq req) {
        skuService.batchUpdatePrice(req.getSkuIds(), req.getPrice());
        return Result.success(null);
    }

    @PutMapping("/stock/{skuId}")
    @PreAuthorize("hasAuthority('product:sku:stock')")
    public Result stock(@PathVariable Long skuId, @RequestParam Integer stock) {
        skuService.updateStock(skuId, stock);
        return Result.success(null);
    }

    @GetMapping("/stock/{skuId}")
    @PreAuthorize("hasAuthority('product:sku:list')")
    public Result getStock(@PathVariable Long skuId) {
        return Result.success(skuService.getStock(skuId));
    }

    @lombok.Data
    public static class BatchPriceReq {
        private List<Long> skuIds;
        private BigDecimal price;
    }
}
```

- [ ] **Step 6：Commit**

```bash
git add mall-product/src/ mall-web/src/main/java/com/mall/web/controller/product/ mall-common/src/main/java/com/mall/common/config/RedisConfig.java
git commit -m "feat: implement SKU management with Redis stock cache"
```

---

### Task 4.4：品牌管理

**Files:**
- Create: `mall-product/src/main/java/com/mall/product/entity/Brand.java`
- Create: `mall-product/src/main/java/com/mall/product/mapper/BrandMapper.java`
- Create: `mall-product/src/main/java/com/mall/product/service/BrandService.java`
- Create: `mall-product/src/main/java/com/mall/product/service/impl/BrandServiceImpl.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/product/BrandController.java`

- [ ] **Step 1：创建 Brand 实体**

```java
package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pm_brand")
public class Brand extends BaseEntity {
    private String brandName;
    private String brandLogo;
    private String brandDesc;
    private Integer orderNum;
    private Integer status;
}
```

- [ ] **Step 2：创建 BrandMapper**

```java
package com.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.entity.Brand;

public interface BrandMapper extends BaseMapper<Brand> {
}
```

- [ ] **Step 3：创建 BrandService**

```java
package com.mall.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.product.entity.Brand;

public interface BrandService {
    IPage<Brand> selectPage(Integer page, Integer size, String keyword);
    Brand getById(Long id);
    void save(Brand brand);
    void update(Brand brand);
    void delete(Long id);
}
```

- [ ] **Step 4：创建 BrandServiceImpl**

```java
package com.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.product.entity.Brand;
import com.mall.product.mapper.BrandMapper;
import com.mall.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {
    private final BrandMapper brandMapper;

    @Override
    public IPage<Brand> selectPage(Integer page, Integer size, String keyword) {
        LambdaQueryWrapper<Brand> wrapper = Wrappers.<Brand>lambdaQuery()
            .like(StringUtils.hasText(keyword), Brand::getBrandName, keyword)
            .orderByAsc(Brand::getOrderNum);
        return brandMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Brand getById(Long id) { return brandMapper.selectById(id); }

    @Override
    public void save(Brand brand) { brandMapper.insert(brand); }

    @Override
    public void update(Brand brand) { brandMapper.updateById(brand); }

    @Override
    public void delete(Long id) { brandMapper.deleteById(id); }
}
```

- [ ] **Step 5：创建 BrandController**

```java
package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.product.entity.Brand;
import com.mall.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/product/brand")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result page(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size,
                       String keyword) {
        return Result.success(brandService.selectPage(page, size, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result get(@PathVariable Long id) {
        return Result.success(brandService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result add(@Valid @RequestBody Brand brand) {
        brandService.save(brand);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result update(@PathVariable Long id, @Valid @RequestBody Brand brand) {
        brand.setId(id);
        brandService.update(brand);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:brand:config')")
    public Result delete(@PathVariable Long id) {
        brandService.delete(id);
        return Result.success(null);
    }
}
```

- [ ] **Step 6：Commit**

```bash
git add mall-product/src/ mall-web/src/main/java/com/mall/web/controller/product/
git commit -m "feat: implement brand CRUD"
```

---

## Phase 5：ES 搜索引擎

### Task 5.1：ES 配置与商品索引

**Files:**
- Create: `mall-search/src/main/java/com/mall/search/config/ElasticsearchConfig.java`
- Create: `mall-search/src/main/java/com/mall/search/service/ProductSearchService.java`
- Create: `mall-search/src/main/java/com/mall/search/service/impl/ProductSearchServiceImpl.java`
- Create: `mall-search/src/main/java/com/mall/search/job/ProductIndexJob.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/product/SearchController.java`

- [ ] **Step 1：创建 ES 配置**

```java
package com.mall.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${elasticsearch.hosts:localhost:9200}")
    private String hosts;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
            .connectedTo(hosts)
            .build();
    }
}
```

- [ ] **Step 2：创建搜索服务**

```java
package com.mall.search.service;

import java.util.Map;
import java.util.List;

public interface ProductSearchService {
    void createIndex();
    void indexProduct(Map<String, Object> product);
    void deleteProduct(Long spuId);
    Map<String, Object> search(String keyword, Long categoryId,
                                Double minPrice, Double maxPrice,
                                String sortField, String sortOrder,
                                int page, int size);
    void syncAllProducts();
}
```

- [ ] **Step 3：创建 ES 搜索实现（ProductSearchServiceImpl）**

```java
package com.mall.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.mall.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ElasticsearchClient esClient;
    private static final String INDEX_NAME = "mall_product";

    @Override
    public void createIndex() {
        try {
            boolean exists = esClient.indices().exists(
                r -> r.index(INDEX_NAME)).value();
            if (exists) {
                log.info("Index {} already exists", INDEX_NAME);
                return;
            }
            CreateIndexResponse response = esClient.indices().create(
                r -> r.index(INDEX_NAME)
                    .mappings(m -> m
                        .properties("spuId", p -> p.long_(l -> l))
                        .properties("spuName", p -> p.text(t -> t.analyzer("ik_smart")))
                        .properties("categoryPath", p -> p.keyword(k -> k))
                        .properties("brand", p -> p.keyword(k -> k))
                        .properties("minPrice", p -> p.double_(d -> d))
                        .properties("currency", p -> p.keyword(k -> k))
                        .properties("salesCount", p -> p.long_(l -> l))
                        .properties("rating", p -> p.double_(d -> d))
                        .properties("status", p -> p.byte_(b -> b))
                    )
            );
            log.info("Index created: {}", response.acknowledged());
        } catch (IOException e) {
            log.error("Failed to create index", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void indexProduct(Map<String, Object> product) {
        try {
            esClient.index(r -> r
                .index(INDEX_NAME)
                .id(String.valueOf(product.get("spuId")))
                .document(product));
        } catch (IOException e) {
            log.error("Failed to index product: {}", product.get("spuId"), e);
        }
    }

    @Override
    public void deleteProduct(Long spuId) {
        try {
            esClient.delete(r -> r.index(INDEX_NAME).id(String.valueOf(spuId)));
        } catch (IOException e) {
            log.error("Failed to delete product from index: {}", spuId, e);
        }
    }

    @Override
    public Map<String, Object> search(String keyword, Long categoryId,
                                       Double minPrice, Double maxPrice,
                                       String sortField, String sortOrder,
                                       int page, int size) {
        try {
            int from = (page - 1) * size;
            SortOrder order = "desc".equalsIgnoreCase(sortOrder) ? SortOrder.Desc : SortOrder.Asc;

            SearchResponse<Map> response = esClient.search(s -> s
                    .index(INDEX_NAME)
                    .from(from)
                    .size(size)
                    .query(q -> q
                        .bool(b -> {
                            if (keyword != null && !keyword.isEmpty()) {
                                b.must(m -> m.match(t -> t.field("spuName").query(keyword)));
                            }
                            if (categoryId != null) {
                                b.filter(f -> f.term(t -> t.field("categoryId").value(categoryId)));
                            }
                            if (minPrice != null || maxPrice != null) {
                                b.filter(f -> f.range(r -> {
                                    if (minPrice != null) r.gte(String.valueOf(minPrice));
                                    if (maxPrice != null) r.lte(String.valueOf(maxPrice));
                                    return r.field("minPrice");
                                }));
                            }
                            return b;
                        }))
                    .sort(s0 -> s0.field(f -> {
                        if (sortField != null) {
                            f.field(sortField).order(order);
                        } else {
                            f.field("salesCount").order(SortOrder.Desc);
                        }
                        return f;
                    })),
                Map.class);

            List<Map> records = response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("total", response.hits().total().value());
            result.put("page", page);
            result.put("size", size);
            return result;
        } catch (IOException e) {
            log.error("Search failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void syncAllProducts() {
        // 从 MySQL 全量同步至 ES
        createIndex();
        log.info("Full product sync to ES completed (placeholder)");
    }
}
```

- [ ] **Step 4：创建 SearchController**

```java
package com.mall.web.controller.product;

import com.mall.common.result.Result;
import com.mall.search.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final ProductSearchService searchService;

    @GetMapping("/product")
    public Result search(@RequestParam(required = false) String keyword,
                         @RequestParam(required = false) Long categoryId,
                         @RequestParam(required = false) Double minPrice,
                         @RequestParam(required = false) Double maxPrice,
                         @RequestParam(required = false) String sortField,
                         @RequestParam(required = false) String sortOrder,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "20") int size) {
        return Result.success(searchService.search(keyword, categoryId,
            minPrice, maxPrice, sortField, sortOrder, page, size));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('product:spu:edit')")
    public Result sync() {
        searchService.syncAllProducts();
        return Result.success(null);
    }
}
```

- [ ] **Step 5：Commit**

```bash
git add mall-search/src/ mall-web/src/main/java/com/mall/web/controller/product/
git commit -m "feat: implement Elasticsearch product search"
```

---

## Phase 6：订单模块

### Task 6.1：订单创建与 MQ 异步处理

**Files:**
- Create: `mall-order/src/main/java/com/mall/order/entity/Order.java`
- Create: `mall-order/src/main/java/com/mall/order/entity/OrderItem.java`
- Create: `mall-order/src/main/java/com/mall/order/entity/OrderPay.java`
- Create: `mall-order/src/main/java/com/mall/order/mapper/OrderMapper.java`
- Create: `mall-order/src/main/java/com/mall/order/mapper/OrderItemMapper.java`
- Create: `mall-order/src/main/java/com/mall/order/service/OrderService.java`
- Create: `mall-order/src/main/java/com/mall/order/service/impl/OrderServiceImpl.java`
- Create: `mall-order/src/main/java/com/mall/order/mq/OrderMessageSender.java`
- Create: `mall-order/src/main/java/com/mall/order/mq/OrderMessageListener.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/order/OrderController.java`

- [ ] **Step 1：创建 Order 实体**

```java
package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("om_order")
public class Order extends BaseEntity {
    private String orderNo;
    private Long userId;
    private Long memberId;
    private BigDecimal totalAmount;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal customsDeclare;
    private BigDecimal tariffAmount;
    private BigDecimal tariffRate;
    private BigDecimal shippingFee;
    private String shippingMethod;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private Integer orderStatus;    // 0待支付 1已支付 2已发货 3已签收 4已完成 5已取消
    private Integer payStatus;      // 0未支付 1已支付 2退款中 3已退款
    private Integer logisticsStatus;// 0未发货 1已出关 2运输中 3已入关 4已签收
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
}
```

- [ ] **Step 2：创建 OrderItem 实体**

```java
package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("om_order_item")
public class OrderItem extends BaseEntity {
    private Long orderId;
    private Long skuId;
    private Long spuId;
    private String skuName;
    private String skuAttrs;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private BigDecimal tariffRate;
    private BigDecimal tariffAmount;
}
```

- [ ] **Step 3：创建 OrderService（含 Redis 分布式锁 + MQ 异步）**

```java
package com.mall.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.order.entity.Order;
import java.util.Map;

public interface OrderService {
    String createOrder(Long userId, Long skuId, Integer quantity, Long couponId);
    IPage<Order> selectPage(Integer page, Integer size, Integer orderStatus, String keyword);
    Order getById(Long id);
    Order getByOrderNo(String orderNo);
    void processOrderMessage(Map<String, Object> message);  // MQ 消费者
    void paySuccess(String orderNo);
    void cancelOrder(Long id);
}
```

- [ ] **Step 4：创建 OrderServiceImpl（核心逻辑）**

```java
package com.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mq.OrderMessageSender;
import com.mall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderMessageSender messageSender;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STOCK_KEY = "stock:sku:";
    private static final String LOCK_KEY = "lock:order:create:";

    @Override
    public String createOrder(Long userId, Long skuId, Integer quantity, Long couponId) {
        // 1. Redis 分布式锁 (防重复下单)
        String lockKey = LOCK_KEY + skuId + ":" + userId;
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw new RuntimeException("操作太频繁，请稍后重试");
        }
        try {
            // 2. Redis 预扣库存
            Long stock = redisTemplate.opsForValue()
                .decrement(STOCK_KEY + skuId);
            if (stock == null || stock < 0) {
                redisTemplate.opsForValue().increment(STOCK_KEY + skuId); // 回滚
                throw new RuntimeException("库存不足");
            }

            // 3. 发送 MQ 消息异步处理订单
            Map<String, Object> msg = new HashMap<>();
            msg.put("userId", userId);
            msg.put("skuId", skuId);
            msg.put("quantity", quantity);
            msg.put("couponId", couponId);
            msg.put("orderNo", "ORD-" + System.currentTimeMillis());
            messageSender.sendOrderCreate(msg);

            return (String) msg.get("orderNo");
        } finally {
            // 释放锁
            String val = (String) redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(val)) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    @Override
    @Transactional
    public void processOrderMessage(Map<String, Object> msg) {
        String orderNo = (String) msg.get("orderNo");
        Long userId = Long.valueOf(msg.get("userId").toString());
        Long skuId = Long.valueOf(msg.get("skuId").toString());
        Integer quantity = Integer.valueOf(msg.get("quantity").toString());

        // 创建订单 (简化版，实际应查商品价格)
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(new BigDecimal("99.99").multiply(BigDecimal.valueOf(quantity)));
        order.setCurrency("USD");
        order.setExchangeRate(new BigDecimal("7.24"));
        order.setTariffAmount(new BigDecimal("9.99"));
        order.setShippingFee(new BigDecimal("15.00"));
        order.setPayAmount(order.getTotalAmount().add(order.getTariffAmount()).add(order.getShippingFee()));
        order.setOrderStatus(0);
        order.setPayStatus(0);
        order.setLogisticsStatus(0);
        orderMapper.insert(order);

        // 创建订单明细
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setPrice(new BigDecimal("99.99"));
        item.setTotalPrice(new BigDecimal("99.99").multiply(BigDecimal.valueOf(quantity)));
        orderItemMapper.insert(item);

        log.info("Order created successfully: {}", orderNo);
    }

    @Override
    @Transactional
    public void paySuccess(String orderNo) {
        Order order = orderMapper.selectOne(
            Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
        if (order == null || order.getOrderStatus() != 0) return;

        order.setOrderStatus(1);
        order.setPayStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("Order paid: {}", orderNo);
    }

    @Override
    public IPage<Order> selectPage(Integer page, Integer size,
                                    Integer orderStatus, String keyword) {
        LambdaQueryWrapper<Order> wrapper = Wrappers.<Order>lambdaQuery()
            .eq(orderStatus != null, Order::getOrderStatus, orderStatus)
            .like(StringUtils.hasText(keyword), Order::getOrderNo, keyword)
            .orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Order getById(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        return orderMapper.selectOne(
            Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
    }

    @Override
    public void cancelOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        order.setOrderStatus(5);
        orderMapper.updateById(order);
    }
}
```

- [ ] **Step 5：创建 MQ 消息发送 + 监听**

```java
// OrderMessageSender.java
package com.mall.order.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderMessageSender {
    private final RabbitTemplate rabbitTemplate;

    public void sendOrderCreate(Map<String, Object> message) {
        rabbitTemplate.convertAndSend("order.exchange", "order.create", message);
    }

    public void sendOrderPaid(Map<String, Object> message) {
        rabbitTemplate.convertAndSend("order.exchange", "order.paid", message);
    }
}

// OrderMessageListener.java
package com.mall.order.mq;

import com.mall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageListener {
    private final OrderService orderService;

    @RabbitListener(queues = "order.create.queue")
    public void handleOrderCreate(Map<String, Object> message) {
        log.info("Received order create message: {}", message);
        orderService.processOrderMessage(message);
    }

    @RabbitListener(queues = "order.paid.queue")
    public void handleOrderPaid(Map<String, Object> message) {
        String orderNo = (String) message.get("orderNo");
        orderService.paySuccess(orderNo);
    }
}
```

- [ ] **Step 6：创建 RabbitMQ 配置**

`mall-common/src/main/java/com/mall/common/config/RabbitConfig.java`:
```java
package com.mall.common.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange");
    }

    @Bean
    public Queue orderCreateQueue() {
        return new Queue("order.create.queue");
    }

    @Bean
    public Queue orderPaidQueue() {
        return new Queue("order.paid.queue");
    }

    @Bean
    public Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue())
            .to(orderExchange()).with("order.create");
    }

    @Bean
    public Binding orderPaidBinding() {
        return BindingBuilder.bind(orderPaidQueue())
            .to(orderExchange()).with("order.paid");
    }
}
```

- [ ] **Step 7：创建 OrderController**

```java
package com.mall.web.controller.order;

import com.mall.common.result.Result;
import com.mall.order.service.OrderService;
import com.mall.security.user.LoginUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/create")
    public Result create(@RequestBody CreateReq req, Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        String orderNo = orderService.createOrder(user.getUserId(),
            req.getSkuId(), req.getQuantity(), req.getCouponId());
        return Result.success(orderNo);
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('order:list')")
    public Result page(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size,
                       Integer orderStatus, String keyword) {
        return Result.success(orderService.selectPage(page, size, orderStatus, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:detail')")
    public Result detail(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    @PostMapping("/{id}/pay")
    public Result pay(@PathVariable Long id) {
        Order order = orderService.getById(id);
        orderService.paySuccess(order.getOrderNo());
        return Result.success(null);
    }

    @PostMapping("/{id}/cancel")
    public Result cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success(null);
    }

    @Data
    public static class CreateReq {
        @NotNull private Long skuId;
        @Min(1) private Integer quantity;
        private Long couponId;
    }
}
```

- [ ] **Step 8：Commit**

```bash
git add mall-order/src/ mall-web/src/main/java/com/mall/web/controller/order/ mall-common/src/main/java/com/mall/common/config/RabbitConfig.java
git commit -m "feat: implement order create with Redis lock and MQ async processing"
```

---

### Task 6.2：退款处理 + 物流追踪

**Files:**
- Create: `mall-order/src/main/java/com/mall/order/entity/OrderRefund.java`
- Create: `mall-order/src/main/java/com/mall/order/entity/OrderLogistics.java`
- Create: `mall-order/src/main/java/com/mall/order/mapper/RefundMapper.java`
- Create: `mall-order/src/main/java/com/mall/order/mapper/LogisticsMapper.java`
- Create: `mall-order/src/main/java/com/mall/order/service/RefundService.java`
- Create: `mall-order/src/main/java/com/mall/order/service/impl/RefundServiceImpl.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/order/RefundController.java`

- [ ] **Step 1：创建 OrderRefund 实体**

```java
package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("om_order_refund")
public class OrderRefund extends BaseEntity {
    private Long orderId;
    private String orderNo;
    private Long skuId;
    private Integer quantity;
    private BigDecimal refundAmount;
    private String currency;
    private String refundReason;
    private Integer refundType;      // 0仅退款 1退货退款
    private Integer refundStatus;    // 0待审批 1已通过 2已驳回 3已完成
    private Long applicantId;        // 申请人(客服)
    private Long approverId;         // 审批人(店长)
    private LocalDateTime approveTime;
    private String approveComment;
}
```

- [ ] **Step 2：创建 OrderLogistics 实体**

```java
package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("om_order_logistics")
public class OrderLogistics extends BaseEntity {
    private Long orderId;
    private String trackingNo;
    private String carrier;            // DHL/FedEx/UPS/EMS
    private String customsDeclareNo;   // 报关单号
    private Integer customsStatus;     // 0未报关 1已报关 2清关中 3已清关
    private String trackingEvents;     // JSON: [{"time":"...","location":"...","desc":"..."}]
    private String originCountry;
    private String destCountry;
}
```

- [ ] **Step 3：创建 RefundMapper 和 LogisticsMapper**

```java
// RefundMapper.java
package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.OrderRefund;

public interface RefundMapper extends BaseMapper<OrderRefund> {
}

// LogisticsMapper.java
package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.OrderLogistics;

public interface LogisticsMapper extends BaseMapper<OrderLogistics> {
}
```

- [ ] **Step 4：创建 RefundService**

```java
package com.mall.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.order.entity.OrderRefund;

public interface RefundService {
    IPage<OrderRefund> selectPage(Integer page, Integer size, Integer refundStatus);
    OrderRefund getById(Long id);
    void apply(OrderRefund refund, Long userId);       // 客服发起退款
    void approve(Long id, Long approverId, String comment); // 店长审批
    void reject(Long id, Long approverId, String comment);
}
```

- [ ] **Step 5：创建 RefundServiceImpl**

```java
package com.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.order.entity.OrderRefund;
import com.mall.order.mapper.RefundMapper;
import com.mall.order.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    private final RefundMapper refundMapper;

    @Override
    public IPage<OrderRefund> selectPage(Integer page, Integer size, Integer refundStatus) {
        LambdaQueryWrapper<OrderRefund> wrapper = Wrappers.<OrderRefund>lambdaQuery()
            .eq(refundStatus != null, OrderRefund::getRefundStatus, refundStatus)
            .orderByDesc(OrderRefund::getCreateTime);
        return refundMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public OrderRefund getById(Long id) {
        return refundMapper.selectById(id);
    }

    @Override
    @Transactional
    public void apply(OrderRefund refund, Long userId) {
        refund.setApplicantId(userId);
        refund.setRefundStatus(0); // 待审批
        refundMapper.insert(refund);
    }

    @Override
    @Transactional
    public void approve(Long id, Long approverId, String comment) {
        OrderRefund refund = refundMapper.selectById(id);
        if (refund == null) throw new RuntimeException("退款申请不存在");
        if (refund.getRefundStatus() != 0) throw new RuntimeException("该申请已处理");
        refund.setRefundStatus(1);
        refund.setApproverId(approverId);
        refund.setApproveTime(LocalDateTime.now());
        refund.setApproveComment(comment);
        refundMapper.updateById(refund);
    }

    @Override
    @Transactional
    public void reject(Long id, Long approverId, String comment) {
        OrderRefund refund = refundMapper.selectById(id);
        if (refund == null) throw new RuntimeException("退款申请不存在");
        refund.setRefundStatus(2);
        refund.setApproverId(approverId);
        refund.setApproveTime(LocalDateTime.now());
        refund.setApproveComment(comment);
        refundMapper.updateById(refund);
    }
}
```

- [ ] **Step 6：创建 RefundController**

```java
package com.mall.web.controller.order;

import com.mall.common.result.Result;
import com.mall.order.entity.OrderRefund;
import com.mall.order.service.RefundService;
import com.mall.security.user.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/order/refund")
@RequiredArgsConstructor
public class RefundController {
    private final RefundService refundService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('order:refund:process')")
    public Result page(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size,
                       Integer refundStatus) {
        return Result.success(refundService.selectPage(page, size, refundStatus));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:refund:process')")
    public Result get(@PathVariable Long id) {
        return Result.success(refundService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('order:refund:process')")
    public Result apply(@Valid @RequestBody OrderRefund refund, Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        refundService.apply(refund, user.getUserId());
        return Result.success(null);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result approve(@PathVariable Long id, @RequestParam String comment,
                          Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        refundService.approve(id, user.getUserId(), comment);
        return Result.success(null);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('order:refund:approve')")
    public Result reject(@PathVariable Long id, @RequestParam String comment,
                         Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        refundService.reject(id, user.getUserId(), comment);
        return Result.success(null);
    }
}
```

- [ ] **Step 7：Commit**

```bash
git add mall-order/src/ mall-web/src/main/java/com/mall/web/controller/order/
git commit -m "feat: implement refund process and logistics tracking"
```

---

## Phase 7：会员模块

### Task 7.1：会员管理

**Files:**
- Create: `mall-member/src/main/java/com/mall/member/entity/Member.java`
- Create: `mall-member/src/main/java/com/mall/member/entity/MemberAddr.java`
- Create: `mall-member/src/main/java/com/mall/member/mapper/MemberMapper.java`
- Create: `mall-member/src/main/java/com/mall/member/service/MemberService.java`
- Create: `mall-member/src/main/java/com/mall/member/service/impl/MemberServiceImpl.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/member/MemberController.java`

- [ ] **Step 1：创建 Member 实体**

```java
package com.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mm_member")
public class Member extends BaseEntity {
    private String email;
    private String phone;
    private String password;
    private String nickName;
    private Integer gender;
    private String avatar;
    private Integer level;         // 0普通 1Gold 2Platinum
    private Integer points;        // 积分
    private BigDecimal totalAmount; // 累计消费
    private Integer status;
}
```

- [ ] **Step 2：创建 MemberService**

```java
package com.mall.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.member.entity.Member;
import java.math.BigDecimal;

public interface MemberService {
    IPage<Member> selectPage(Integer page, Integer size, String keyword, Integer level);
    Member getById(Long id);
    void update(Member member);
    void adjustPoints(Long id, int points, String reason);
    BigDecimal getTotalAmount(Long id);
}
```

- [ ] **Step 3：创建 MemberServiceImpl**

```java
package com.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.member.entity.Member;
import com.mall.member.entity.MemberPointsLog;
import com.mall.member.mapper.MemberMapper;
import com.mall.member.mapper.MemberPointsLogMapper;
import com.mall.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberMapper memberMapper;
    private final MemberPointsLogMapper pointsLogMapper;

    @Override
    public IPage<Member> selectPage(Integer page, Integer size, String keyword, Integer level) {
        LambdaQueryWrapper<Member> wrapper = Wrappers.<Member>lambdaQuery()
            .like(StringUtils.hasText(keyword), Member::getNickName, keyword)
            .or(StringUtils.hasText(keyword))
            .like(StringUtils.hasText(keyword), Member::getEmail, keyword)
            .eq(level != null, Member::getLevel, level)
            .orderByDesc(Member::getCreateTime);
        return memberMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Member getById(Long id) {
        return memberMapper.selectById(id);
    }

    @Override
    public void update(Member member) {
        memberMapper.updateById(member);
    }

    @Override
    @Transactional
    public void adjustPoints(Long id, int points, String reason) {
        Member member = memberMapper.selectById(id);
        if (member == null) throw new RuntimeException("会员不存在");
        member.setPoints(member.getPoints() + points);
        memberMapper.updateById(member);

        MemberPointsLog log = new MemberPointsLog();
        log.setMemberId(id);
        log.setPoints(points);
        log.setReason(reason);
        pointsLogMapper.insert(log);
    }

    @Override
    public BigDecimal getTotalAmount(Long id) {
        Member member = memberMapper.selectById(id);
        return member != null ? member.getTotalAmount() : BigDecimal.ZERO;
    }
}
```

- [ ] **Step 4：创建 MemberController**

```java
package com.mall.web.controller.member;

import com.mall.common.result.Result;
import com.mall.member.entity.Member;
import com.mall.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('member:list')")
    public Result page(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size,
                       String keyword, Integer level) {
        return Result.success(memberService.selectPage(page, size, keyword, level));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('member:detail')")
    public Result detail(@PathVariable Long id) {
        return Result.success(memberService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('member:detail')")
    public Result update(@PathVariable Long id, @RequestBody Member member) {
        member.setId(id);
        memberService.update(member);
        return Result.success(null);
    }

    @PostMapping("/{id}/points")
    @PreAuthorize("hasAuthority('member:points:adjust')")
    public Result adjustPoints(@PathVariable Long id, @RequestParam int points,
                                @RequestParam String reason) {
        memberService.adjustPoints(id, points, reason);
        return Result.success(null);
    }
}
```

- [ ] **Step 5：Commit**

```bash
git add mall-member/src/ mall-web/src/main/java/com/mall/web/controller/member/
git commit -m "feat: implement member management with points system"
```

---

## Phase 8：营销模块

### Task 8.1：优惠券管理

**Files:**
- Create: `mall-marketing/src/main/java/com/mall/marketing/entity/Coupon.java`
- Create: `mall-marketing/src/main/java/com/mall/marketing/entity/CouponIssue.java`
- Create: `mall-marketing/src/main/java/com/mall/marketing/mapper/CouponMapper.java`
- Create: `mall-marketing/src/main/java/com/mall/marketing/service/CouponService.java`
- Create: `mall-marketing/src/main/java/com/mall/marketing/service/impl/CouponServiceImpl.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/marketing/CouponController.java`

- [ ] **Step 1：创建 Coupon 实体**

```java
package com.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mk_coupon")
public class Coupon extends BaseEntity {
    private String couponName;
    private String couponType;    // FULL_REDUCTION / DISCOUNT / SHIPPING
    private BigDecimal threshold; // 满减门槛
    private BigDecimal discount;  // 减免金额/折扣率
    private String currency;
    private Integer maxIssue;     // 发行总量
    private Integer issuedCount;  // 已发行
    private Integer perLimit;     // 每人限领
    private LocalDateTime validStart;
    private LocalDateTime validEnd;
    private String scope;         // ALL / CATEGORY / SKU
    private String scopeIds;      // 适用范围ID（逗号分隔）
    private Integer status;       // 0草稿 1待审核 2已发布 3已结束
}
```

- [ ] **Step 2：创建 CouponMapper**

```java
package com.mall.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.marketing.entity.Coupon;

public interface CouponMapper extends BaseMapper<Coupon> {
}
```

- [ ] **Step 3：创建 CouponService**

```java
package com.mall.marketing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.marketing.entity.Coupon;

public interface CouponService {
    IPage<Coupon> selectPage(Integer page, Integer size, Integer status, String keyword);
    Coupon getById(Long id);
    void save(Coupon coupon);
    void update(Coupon coupon);
    void delete(Long id);
    void submitAudit(Long id);    // 提交审核（运营）
    void audit(Long id, Integer status, String comment); // 审核（店长）
}
```

- [ ] **Step 4：创建 CouponServiceImpl**

```java
package com.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.marketing.entity.Coupon;
import com.mall.marketing.mapper.CouponMapper;
import com.mall.marketing.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {
    private final CouponMapper couponMapper;

    @Override
    public IPage<Coupon> selectPage(Integer page, Integer size, Integer status, String keyword) {
        LambdaQueryWrapper<Coupon> wrapper = Wrappers.<Coupon>lambdaQuery()
            .eq(status != null, Coupon::getStatus, status)
            .like(StringUtils.hasText(keyword), Coupon::getCouponName, keyword)
            .orderByDesc(Coupon::getCreateTime);
        return couponMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Coupon getById(Long id) {
        return couponMapper.selectById(id);
    }

    @Override
    @Transactional
    public void save(Coupon coupon) {
        coupon.setStatus(0); // 草稿
        coupon.setIssuedCount(0);
        couponMapper.insert(coupon);
    }

    @Override
    @Transactional
    public void update(Coupon coupon) {
        couponMapper.updateById(coupon);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        couponMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void submitAudit(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) throw new RuntimeException("优惠券不存在");
        coupon.setStatus(1); // 待审核
        couponMapper.updateById(coupon);
    }

    @Override
    @Transactional
    public void audit(Long id, Integer status, String comment) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) throw new RuntimeException("优惠券不存在");
        if (coupon.getStatus() != 1) throw new RuntimeException("该优惠券不是待审核状态");
        coupon.setStatus(status); // 2已发布 3已结束
        couponMapper.updateById(coupon);
    }
}
```

- [ ] **Step 5：创建 CouponController**

```java
package com.mall.web.controller.marketing;

import com.mall.common.result.Result;
import com.mall.marketing.entity.Coupon;
import com.mall.marketing.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/marketing/coupon")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('marketing:coupon:list')")
    public Result page(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size,
                       Integer status, String keyword) {
        return Result.success(couponService.selectPage(page, size, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:coupon:list')")
    public Result get(@PathVariable Long id) {
        return Result.success(couponService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('marketing:coupon:add')")
    public Result add(@RequestBody Coupon coupon) {
        couponService.save(coupon);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:coupon:add')")
    public Result update(@PathVariable Long id, @RequestBody Coupon coupon) {
        coupon.setId(id);
        couponService.update(coupon);
        return Result.success(null);
    }

    @PostMapping("/{id}/submit-audit")
    @PreAuthorize("hasAuthority('marketing:coupon:add')")
    public Result submitAudit(@PathVariable Long id) {
        couponService.submitAudit(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('marketing:coupon:audit')")
    public Result audit(@PathVariable Long id, @RequestParam Integer status,
                        @RequestParam(required = false) String comment) {
        couponService.audit(id, status, comment);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('marketing:coupon:add')")
    public Result delete(@PathVariable Long id) {
        couponService.delete(id);
        return Result.success(null);
    }
}
```

- [ ] **Step 6：Commit**

```bash
git add mall-marketing/src/ mall-web/src/main/java/com/mall/web/controller/marketing/
git commit -m "feat: implement coupon management with audit flow"
```

---

### Task 8.2：活动与秒杀

**Files:**
- Create: `mall-marketing/src/main/java/com/mall/marketing/entity/Activity.java`
- Create: `mall-marketing/src/main/java/com/mall/marketing/entity/ActivitySku.java`
- Create: `mall-marketing/src/main/java/com/mall/marketing/mapper/ActivityMapper.java`
- Create: `mall-marketing/src/main/java/com/mall/marketing/service/ActivityService.java`
- Create: `mall-marketing/src/main/java/com/mall/marketing/service/SeckillService.java`
- Create: `mall-marketing/src/main/java/com/mall/marketing/service/impl/ActivityServiceImpl.java`
- Create: `mall-marketing/src/main/java/com/mall/marketing/service/impl/SeckillServiceImpl.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/marketing/ActivityController.java`

- [ ] **Step 1：创建秒杀服务（Redis 预热 + 限流）**

```java
package com.mall.marketing.service.impl;

import com.mall.marketing.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void prepareSeckill(Long activityId, Long skuId, Integer totalStock) {
        // 秒杀库存预热到 Redis
        redisTemplate.opsForValue().set("seckill:stock:" + skuId, totalStock);
    }

    @Override
    public boolean trySeckill(Long userId, Long skuId) {
        // 原子扣减秒杀库存
        Long stock = redisTemplate.opsForValue()
            .decrement("seckill:stock:" + skuId);
        if (stock == null || stock < 0) {
            redisTemplate.opsForValue().increment("seckill:stock:" + skuId);
            return false;
        }
        return true;
    }
}
```

```bash
git add mall-marketing/src/ mall-web/src/main/java/com/mall/web/controller/marketing/
git commit -m "feat: implement activity and seckill management"
```

---

## Phase 9：财务模块

### Task 9.1：对账单 + EasyExcel 导出

**Files:**
- Create: `mall-finance/src/main/java/com/mall/finance/entity/Statement.java`
- Create: `mall-finance/src/main/java/com/mall/finance/entity/StatementItem.java`
- Create: `mall-finance/src/main/java/com/mall/finance/entity/TaxConfig.java`
- Create: `mall-finance/src/main/java/com/mall/finance/mapper/StatementMapper.java`
- Create: `mall-finance/src/main/java/com/mall/finance/mapper/TaxConfigMapper.java`
- Create: `mall-finance/src/main/java/com/mall/finance/service/StatementService.java`
- Create: `mall-finance/src/main/java/com/mall/finance/service/impl/StatementServiceImpl.java`
- Create: `mall-finance/src/main/java/com/mall/finance/service/TaxConfigService.java`
- Create: `mall-finance/src/main/java/com/mall/finance/service/impl/TaxConfigServiceImpl.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/finance/StatementController.java`
- Create: `mall-web/src/main/java/com/mall/web/controller/finance/TaxConfigController.java`

- [ ] **Step 1：创建 Statement 实体 + 对账服务（含 EasyExcel 导出）**

```java
// TaxConfig.java
package com.mall.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fn_tax_config")
public class TaxConfig extends BaseEntity {
    private Long categoryId;
    private String originCountry;
    private String destCountry;
    private BigDecimal taxRate;
    private String taxType;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
}
```

- [ ] **Step 2：创建对账单导出 DTO**

```java
package com.mall.finance.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StatementExportVO {
    @ExcelProperty("订单号")
    private String orderNo;

    @ExcelProperty("交易金额(USD)")
    private BigDecimal totalAmount;

    @ExcelProperty("关税(USD)")
    private BigDecimal tariffAmount;

    @ExcelProperty("运费(USD)")
    private BigDecimal shippingFee;

    @ExcelProperty("退款金额(USD)")
    private BigDecimal refundAmount;

    @ExcelProperty("实付(USD)")
    private BigDecimal payAmount;

    @ExcelProperty("下单时间")
    private LocalDateTime createTime;
}
```

- [ ] **Step 3：创建 StatementController（含 Excel 导出）**

```java
package com.mall.web.controller.finance;

import com.alibaba.excel.EasyExcel;
import com.mall.common.result.Result;
import com.mall.finance.entity.Statement;
import com.mall.finance.entity.StatementExportVO;
import com.mall.finance.service.StatementService;
import com.mall.finance.service.TaxConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class StatementController {
    private final StatementService statementService;
    private final TaxConfigService taxConfigService;

    @GetMapping("/statement/page")
    @PreAuthorize("hasAuthority('finance:statement:list')")
    public Result page(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(statementService.selectPage(page, size));
    }

    @GetMapping("/statement/{id}")
    @PreAuthorize("hasAuthority('finance:statement:list')")
    public Result detail(@PathVariable Long id) {
        return Result.success(statementService.getDetail(id));
    }

    @GetMapping("/statement/{id}/export")
    @PreAuthorize("hasAuthority('finance:statement:export')")
    public void export(@PathVariable Long id, HttpServletResponse response) throws IOException {
        List<StatementExportVO> data = statementService.getExportData(id);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
            "attachment;filename=" + URLEncoder.encode("对账单.xlsx", "UTF-8"));
        EasyExcel.write(response.getOutputStream(), StatementExportVO.class).sheet("对账单").doWrite(data);
    }

    @PutMapping("/statement/{id}/confirm")
    @PreAuthorize("hasAuthority('finance:statement:confirm')")
    public Result confirm(@PathVariable Long id) {
        statementService.confirm(id);
        return Result.success(null);
    }
}
```

- [ ] **Step 4：Commit**

```bash
git add mall-finance/src/ mall-web/src/main/java/com/mall/web/controller/finance/
git commit -m "feat: implement financial statements, tax config and Excel export"
```

---

## Phase 10：前端路由 + 核心页面

### Task 10.1：前端权限路由与 Axios 封装

- [ ] **Step 1：创建 Axios 封装（request.js）**

`mall-web/src/utils/request.js`:
```javascript
import axios from 'axios'
import { message } from 'ant-design-vue'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    message.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
```

- [ ] **Step 2：创建 Pinia 用户 Store**

`mall-web/src/store/user.js`:
```javascript
import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,
    permissions: []
  }),
  actions: {
    async login(username, password) {
      const res = await request.post('/auth/login', { username, password })
      this.token = res.data.token
      this.permissions = res.data.permissions
      localStorage.setItem('token', this.token)
      return res
    },
    async getUserInfo() {
      const res = await request.get('/auth/userinfo')
      this.userInfo = res.data
      this.permissions = res.data.permissions
      return res.data
    },
    hasPermission(perm) {
      return this.permissions.includes(perm)
    },
    logout() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      localStorage.removeItem('token')
    }
  }
})
```

- [ ] **Step 3：创建动态路由 + 权限路由守卫**

`mall-web/src/router/index.js`:
```javascript
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  { path: '/login', component: () => import('@/views/login/index.vue') },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard',
        meta: { title: '首页', icon: 'DashboardOutlined' },
        component: () => import('@/views/dashboard/index.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

// 根据权限动态添加路由
const asyncRoutes = [
  {
    path: '/product', meta: { title: '商品管理', icon: 'ShopOutlined' },
    children: [
      { path: 'category', component: () => import('@/views/product/category/index.vue'),
        meta: { title: '分类管理', perms: ['product:category:config'] } },
      { path: 'spu', component: () => import('@/views/product/spu/index.vue'),
        meta: { title: 'SPU列表', perms: ['product:spu:list'] } },
      { path: 'sku/:spuId', component: () => import('@/views/product/sku/index.vue'),
        meta: { title: 'SKU管理', perms: ['product:sku:list'], hidden: true } }
    ]
  },
  {
    path: '/order', meta: { title: '订单管理', icon: 'OrderedListOutlined' },
    children: [
      { path: 'list', component: () => import('@/views/order/list/index.vue'),
        meta: { title: '订单列表', perms: ['order:list'] } },
      { path: 'refund', component: () => import('@/views/order/refund/index.vue'),
        meta: { title: '退款处理', perms: ['order:refund:process'] } }
    ]
  },
  {
    path: '/member', meta: { title: '会员管理', icon: 'UserOutlined' },
    children: [
      { path: 'list', component: () => import('@/views/member/list/index.vue'),
        meta: { title: '会员列表', perms: ['member:list'] } }
    ]
  },
  {
    path: '/marketing', meta: { title: '营销管理', icon: 'GiftOutlined' },
    children: [
      { path: 'coupon', component: () => import('@/views/marketing/coupon/index.vue'),
        meta: { title: '优惠券管理', perms: ['marketing:coupon:list'] } },
      { path: 'activity', component: () => import('@/views/marketing/activity/index.vue'),
        meta: { title: '活动管理', perms: ['marketing:activity:config'] } }
    ]
  },
  {
    path: '/finance', meta: { title: '财务管理', icon: 'DollarOutlined' },
    children: [
      { path: 'statement', component: () => import('@/views/finance/statement/index.vue'),
        meta: { title: '对账单', perms: ['finance:statement:list'] } },
      { path: 'tax', component: () => import('@/views/finance/tax/index.vue'),
        meta: { title: '关税配置', perms: ['finance:tax:config'] } }
    ]
  },
  {
    path: '/system', meta: { title: '系统管理', icon: 'SettingOutlined' },
    children: [
      { path: 'user', component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', perms: ['system:user:list'] } },
      { path: 'role', component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', perms: ['system:role:config'] } }
    ]
  }
]

export function addAsyncRoutes(permissions) {
  function filterRoutes(routes) {
    return routes.filter(route => {
      if (route.meta?.perms) {
        return route.meta.perms.some(p => permissions.includes(p))
      }
      if (route.children) {
        route.children = filterRoutes(route.children)
        return route.children.length > 0
      }
      return true
    }).map(route => {
      if (route.children && !route.path) {
        route.path = ''
      }
      return route
    })
  }

  const filtered = filterRoutes(asyncRoutes)
  filtered.forEach(group => {
    router.addRoute(group)
  })
}

router.beforeEach(async (to, from, next) => {
  if (to.path === '/login') return next()
  const token = localStorage.getItem('token')
  if (!token) return next('/login')

  const userStore = useUserStore()
  if (!userStore.permissions.length) {
    await userStore.getUserInfo()
    addAsyncRoutes(userStore.permissions)
    return next({ ...to, replace: true })
  }
  next()
})

export default router
```

- [ ] **Step 4：创建权限指令**

`mall-web/src/utils/permission.js`:
```javascript
import { useUserStore } from '@/store/user'

export const permission = {
  mounted(el, binding) {
    const store = useUserStore()
    const value = binding.value
    if (value && !store.hasPermission(value)) {
      el.parentNode?.removeChild(el)
    }
  }
}
```

在 `main.js` 中注册: `app.directive('permission', permission)`

- [ ] **Step 5：创建 AdminLayout（Ant Design Vue）**

`mall-web/src/layouts/AdminLayout.vue`:
```vue
<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" :trigger="null" collapsible>
      <div class="logo">{{ collapsed ? 'MALL' : '跨境 Mall' }}</div>
      <a-menu v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys"
              mode="inline" theme="dark" @click="handleMenuClick">
        <template v-for="item in visibleRoutes" :key="item">
          <a-sub-menu v-if="item.children?.length" :key="item.path">
            <template #title>
              <component :is="iconMap[item.meta?.icon]" />
              <span>{{ item.meta?.title }}</span>
            </template>
            <a-menu-item v-for="child in item.children" :key="child.path">
              <span>{{ child.meta?.title }}</span>
            </a-menu-item>
          </a-sub-menu>
          <a-menu-item v-else :key="item.path">
            <component :is="iconMap[item.meta?.icon]" />
            <span>{{ item.meta?.title }}</span>
          </a-menu-item>
        </template>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header style="background:#fff;padding:0 24px;display:flex;justify-content:space-between;align-items:center;">
        <MenuUnfoldOutlined v-if="collapsed" class="trigger" @click="collapsed=!collapsed" />
        <MenuFoldOutlined v-else class="trigger" @click="collapsed=!collapsed" />
        <a-dropdown>
          <a class="ant-dropdown-link" @click.prevent>
            {{ userStore.userInfo?.realName || '用户' }} <DownOutlined />
          </a>
          <template #overlay>
            <a-menu @click="handleLogout">
              <a-menu-item key="logout">退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-layout-header>
      <a-layout-content style="margin:24px;padding:24px;background:#fff;min-height:280px;">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  MenuUnfoldOutlined, MenuFoldOutlined, DownOutlined,
  DashboardOutlined, ShopOutlined, OrderedListOutlined,
  UserOutlined, GiftOutlined, DollarOutlined, SettingOutlined
} from '@ant-design/icons-vue'

const iconMap = {
  DashboardOutlined, ShopOutlined, OrderedListOutlined,
  UserOutlined, GiftOutlined, DollarOutlined, SettingOutlined
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const collapsed = ref(false)
const selectedKeys = ref([route.path])
const openKeys = ref([])

const visibleRoutes = computed(() =>
  router.getRoutes().filter(r => r.meta?.title && !r.meta?.hidden
    && r.path.startsWith('/') && !r.path.includes(':'))
)

const handleMenuClick = ({ key }) => router.push(key)
const handleLogout = ({ key }) => {
  if (key === 'logout') { userStore.logout(); router.push('/login') }
}
</script>

<style scoped>
.logo { height:32px;margin:16px;color:#fff;font-size:18px;font-weight:bold;text-align:center;line-height:32px; }
.trigger { font-size:18px;cursor:pointer; }
</style>
```

- [ ] **Step 6：创建登录页（Ant Design Vue）**

`mall-web/src/views/login/index.vue`:
```vue
<template>
  <div class="login-container">
    <div class="login-card">
      <h2>跨境 Mall</h2>
      <p>B2C 跨境电商后台管理系统</p>
      <a-form :model="form" :rules="rules" @finish="handleLogin">
        <a-form-item name="username">
          <a-input v-model:value="form.username" placeholder="用户名" size="large">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item name="password">
          <a-input-password v-model:value="form.password" placeholder="密码" size="large">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="loading" block size="large">登 录</a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }]
}
const handleLogin = async () => {
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    message.success('登录成功')
    router.push('/')
  } catch (e) {
    message.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container { height:100vh;display:flex;align-items:center;justify-content:center;
  background:linear-gradient(135deg,#667eea 0%,#764ba2 100%); }
.login-card { width:400px;padding:40px;background:#fff;border-radius:8px;
  box-shadow:0 8px 24px rgba(0,0,0,0.15);text-align:center; }
.login-card h2 { margin-bottom:4px;font-size:24px; }
.login-card p { margin-bottom:32px;color:#999; }
</style>
```

```bash
git add mall-web/src/router/ mall-web/src/store/ mall-web/src/utils/ mall-web/src/views/ mall-web/src/layouts/
git commit -m "feat: implement frontend auth routing, permission directive, login page with Ant Design Vue"
```

---

## Phase 11：权限菜单初始化 SQL + 集成测试

### Task 11.1：预置权限菜单

- [ ] **Step 1：向 data.sql 追加菜单权限数据**

```sql
USE mall_system;

-- 菜单数据
INSERT IGNORE INTO sys_menu VALUES
-- 商品管理
(100, '商品管理', 0, 1, '/product', NULL, NULL, 'ShopOutlined', 'M', 1, 1, NOW(), NOW()),
(101, '分类管理', 100, 1, '/product/category', 'product/category/index', 'product:category:config', NULL, 'C', 1, 1, NOW(), NOW()),
(102, 'SPU管理', 100, 2, '/product/spu', 'product/spu/index', 'product:spu:list', NULL, 'C', 1, 1, NOW(), NOW()),
(103, '新增SPU', 102, 1, NULL, NULL, 'product:spu:add', NULL, 'F', 1, 1, NOW(), NOW()),
(104, '编辑SPU', 102, 2, NULL, NULL, 'product:spu:edit', NULL, 'F', 1, 1, NOW(), NOW()),
(105, '删除SPU', 102, 3, NULL, NULL, 'product:spu:delete', NULL, 'F', 1, 1, NOW(), NOW()),
(106, '上下架', 102, 4, NULL, NULL, 'product:spu:publish', NULL, 'F', 1, 1, NOW(), NOW()),
(107, 'SKU列表', 102, 5, NULL, NULL, 'product:sku:list', NULL, 'F', 1, 1, NOW(), NOW()),
(108, 'SKU调价', 107, 1, NULL, NULL, 'product:sku:price', NULL, 'F', 1, 1, NOW(), NOW()),
(109, 'SKU库存', 107, 2, NULL, NULL, 'product:sku:stock', NULL, 'F', 1, 1, NOW(), NOW()),
-- 订单管理
(200, '订单管理', 0, 2, '/order', NULL, NULL, 'OrderedListOutlined', 'M', 1, 1, NOW(), NOW()),
(201, '订单列表', 200, 1, '/order/list', 'order/list/index', 'order:list', NULL, 'C', 1, 1, NOW(), NOW()),
(202, '订单详情', 200, 2, NULL, NULL, 'order:detail', NULL, 'F', 1, 1, NOW(), NOW()),
(203, '退款处理', 200, 3, '/order/refund', 'order/refund/index', 'order:refund:process', NULL, 'C', 1, 1, NOW(), NOW()),
(204, '退款审批', 203, 1, NULL, NULL, 'order:refund:approve', NULL, 'F', 1, 1, NOW(), NOW()),
(205, '物流编辑', 200, 4, NULL, NULL, 'order:logistics:edit', NULL, 'F', 1, 1, NOW(), NOW()),
-- 会员管理
(300, '会员管理', 0, 3, '/member', NULL, NULL, 'UserOutlined', 'M', 1, 1, NOW(), NOW()),
(301, '会员列表', 300, 1, '/member/list', 'member/list/index', 'member:list', NULL, 'C', 1, 1, NOW(), NOW()),
(302, '会员详情', 300, 2, NULL, NULL, 'member:detail', NULL, 'F', 1, 1, NOW(), NOW()),
(303, '积分调整', 300, 3, NULL, NULL, 'member:points:adjust', NULL, 'F', 1, 1, NOW(), NOW()),
-- 营销管理
(400, '营销管理', 0, 4, '/marketing', NULL, NULL, 'GiftOutlined', 'M', 1, 1, NOW(), NOW()),
(401, '优惠券管理', 400, 1, '/marketing/coupon', 'marketing/coupon/index', 'marketing:coupon:list', NULL, 'C', 1, 1, NOW(), NOW()),
(402, '新增优惠券', 401, 1, NULL, NULL, 'marketing:coupon:add', NULL, 'F', 1, 1, NOW(), NOW()),
(403, '审核优惠券', 401, 2, NULL, NULL, 'marketing:coupon:audit', NULL, 'F', 1, 1, NOW(), NOW()),
(404, '活动管理', 400, 2, '/marketing/activity', 'marketing/activity/index', 'marketing:activity:config', NULL, 'C', 1, 1, NOW(), NOW()),
-- 财务管理
(500, '财务管理', 0, 5, '/finance', NULL, NULL, 'DollarOutlined', 'M', 1, 1, NOW(), NOW()),
(501, '对账单', 500, 1, '/finance/statement', 'finance/statement/index', 'finance:statement:list', NULL, 'C', 1, 1, NOW(), NOW()),
(502, '导出对账单', 501, 1, NULL, NULL, 'finance:statement:export', NULL, 'F', 1, 1, NOW(), NOW()),
(503, '确认对账', 501, 2, NULL, NULL, 'finance:statement:confirm', NULL, 'F', 1, 1, NOW(), NOW()),
(504, '关税配置', 500, 2, '/finance/tax', 'finance/tax/index', 'finance:tax:config', NULL, 'C', 1, 1, NOW(), NOW()),
-- 系统管理
(600, '系统管理', 0, 6, '/system', NULL, NULL, 'SettingOutlined', 'M', 1, 1, NOW(), NOW()),
(601, '用户管理', 600, 1, '/system/user', 'system/user/index', 'system:user:list', NULL, 'C', 1, 1, NOW(), NOW()),
(602, '新增用户', 601, 1, NULL, NULL, 'system:user:add', NULL, 'F', 1, 1, NOW(), NOW()),
(603, '编辑用户', 601, 2, NULL, NULL, 'system:user:edit', NULL, 'F', 1, 1, NOW(), NOW()),
(604, '角色管理', 600, 2, '/system/role', 'system/role/index', 'system:role:config', NULL, 'C', 1, 1, NOW(), NOW()),
(605, '菜单配置', 600, 3, NULL, NULL, 'system:menu:config', NULL, 'F', 1, 1, NOW(), NOW());

-- 店长 (role_id=1) — 全部权限
INSERT IGNORE INTO sys_role_menu SELECT NULL, 1, id FROM sys_menu WHERE id IN (100,101,102,103,104,105,106,107,108,109,200,201,202,203,204,205,300,301,302,303,400,401,402,403,404,500,501,502,503,504,600,601,602,603,604,605);

-- 运营专员 (role_id=2) — 商品 + 营销
INSERT IGNORE INTO sys_role_menu SELECT NULL, 2, id FROM sys_menu WHERE id IN (100,101,102,103,104,106,107,109,400,401,402,404);

-- 客服专员 (role_id=3) — 订单 + 会员
INSERT IGNORE INTO sys_role_menu SELECT NULL, 3, id FROM sys_menu WHERE id IN (200,201,202,203,205,300,301,302);

-- 财务专员 (role_id=4) — 财务
INSERT IGNORE INTO sys_role_menu SELECT NULL, 4, id FROM sys_menu WHERE id IN (500,501,502,504);
```

- [ ] **Step 2：Commit**

```bash
git add mall-web/src/main/resources/
git commit -m "feat: add permission menu data and role-menu assignments"
```

---

### Task 11.2：Docker Compose 部署

- [ ] **Step 1：创建 docker-compose.yml**

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: mall_system
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mall-web/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/schema.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"

  elasticsearch:
    image: elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"

  backend:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - rabbitmq
      - elasticsearch
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/mall_system
      SPRING_REDIS_HOST: redis
      SPRING_RABBITMQ_HOST: rabbitmq
      ELASTICSEARCH_HOSTS: elasticsearch:9200

  frontend:
    build:
      context: ./mall-web
      dockerfile: Dockerfile
    ports:
      - "3000:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

- [ ] **Step 2：创建后端 Dockerfile**

```dockerfile
FROM maven:3.8-openjdk-8 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:8-jre-slim
WORKDIR /app
COPY --from=build /app/mall-web/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 3：创建前端 Dockerfile**

```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

- [ ] **Step 4：Commit 最终版本**

```bash
git add docker-compose.yml Dockerfile mall-web/Dockerfile
git commit -m "chore: add Docker deployment configuration"
git tag v1.0.0 -m "B2C E-Commerce v1.0.0 - initial release"
```

---

## 实施顺序

| 顺序 | Phase | 内容 | 预估 | 依赖 |
|------|-------|------|------|------|
| 1 | Phase 1 | 项目脚手架 | 0.5天 | 无 |
| 2 | Phase 2 | 基础设施层 | 0.5天 | Phase 1 |
| 3 | Phase 3 | RBAC 权限体系 | 1天 | Phase 2 |
| 4 | Phase 4 | 商品模块 | 2天 | Phase 3 |
| 5 | Phase 5 | ES 搜索引擎 | 1天 | Phase 3 |
| 6 | Phase 6 | 订单模块 | 2天 | Phase 3 |
| 7 | Phase 7 | 会员模块 | 0.5天 | Phase 3 |
| 8 | Phase 8 | 营销模块 | 1.5天 | Phase 4,6 |
| 9 | Phase 9 | 财务模块 | 1天 | Phase 6 |
| 10 | Phase 10 | 前端路由+页面 | 2天 | Phases 4-9 |
| 11 | Phase 11 | Docker+集成 | 0.5天 | 全部 |

**总预估：~12.5 人天（约 3 周，单人开发）**
