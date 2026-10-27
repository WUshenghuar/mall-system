package com.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI对话实体类
 * 继承自BaseEntity，包含会话相关的基本信息
 */
/**************************** CodeGeeX Inline Diff ****************************/
/**
 * AI对话记录实体类，对应数据库表 {@code ai_conversation}。
 *
 * <p>本类用于持久化存储AI对话的详细信息，包括会话上下文关联、请求追踪、
 * 用户标识、消息角色与内容、模型调用指标以及用户反馈等。继承自 {@code BaseEntity} 获取基础公共字段。</p>
 *
 * <h3>核心功能/属性概览：</h3>
 * <ul>
 *   <li>会话与请求追踪：通过 sessionId 关联完整对话上下文，requestId 追踪单次请求</li>
 *   <li>角色区分：支持 user(用户)、assistant(AI助手)、agent(代理) 等角色标识</li>
 *   <li>性能与计费指标：记录 tokensUsed(消耗Token数) 和 latencyMs(响应延迟毫秒数)</li>
 *   <li>用户反馈：通过 feedback 字段记录用户对AI回复的评价</li>
 * </ul>
 *
 * <h3>构造函数参数：</h3>
 * <p>本类依赖 Lombok {@code @Data} 注解自动生成无参构造器和全参构造器。
 * 核心参数含义如下：</p>
 * <ul>
 *   <li>{@code sessionId} - 会话ID，用于标识一次完整的对话上下文</li>
 *   <li>{@code requestId} - 请求ID，用于标识单次请求（便于链路追踪）</li>
 *   <li>{@code userId} - 用户ID，标识对话发起者</li>
 *   <li>{@code role} - 消息角色（如：user / assistant / agent）</li>
 *   <li>{@code content} - 消息文本内容</li>
 *   <li>{@code tokensUsed} - 本次请求消耗的Token数量</li>
 *   <li>{@code latencyMs} - 本次请求的响应延迟时间（毫秒）</li>
 *   <li>{@code model} - 调用的AI模型标识</li>
 *   <li>{@code feedback} - 用户反馈评分或标识</li>
 * </ul>
 *
 * <h3>使用限制与副作用：</h3>
 * <ul>
 *   <li>本类使用了 Lombok 的 {@code @Data} 和 {@code @EqualsAndHashCode(callSuper = true)}，
 *       因此 {@code equals} 和 {@code hashCode} 方法会包含父类 {@code BaseEntity} 的字段，
 *       在将实体放入 {@code HashSet} 或作为 {@code HashMap} 的键时，请确保父类字段也符合预期逻辑。</li>
 *   <li>由于 Lombok 会生成全参构造器，若父类 {@code BaseEntity} 的构造函数非无参，
 *       可能会导致编译错误，需额外配合 {@code @Builder} 或手写构造器处理。</li>
 * </ul>
 *
 * <h3>代码示例：</h3>
 * <pre>{@code
 * AiConversation conversation = new AiConversation();
 * conversation.setSessionId("sess-123456");
 * conversation.setRequestId("req-7890");
 * conversation.setUserId(1001L);
 * conversation.setRole("user");
 * conversation.setContent("请帮我生成一段Java代码");
 * conversation.setModel("gpt-4");
 * }</pre>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_conversation")
public class AiConversation extends BaseEntity {
    private String sessionId;    // 会话ID，用于标识一次完整的对话
    private String requestId;   // 请求ID，用于标识单次请求
    private Long userId;         // 用户ID，标识对话发起者
    /** user / assistant / agent */
    private String role;
    private String content;
    private Integer tokensUsed;
    private Integer latencyMs;
    private String model;
    private Integer feedback;
}
/******************** dfd77962-32e7-4412-9db2-46865447144c ********************/
