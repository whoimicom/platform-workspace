package com.platform.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Spring Data Web 序列化配置。
 *
 * <p>Spring Data 4.x 直接序列化 {@code PageImpl} 会产生不稳定 JSON 结构，
 * 官方推荐通过 {@link EnableSpringDataWebSupport} 指定序列化模式。
 *
 * <p>采用 {@code PageSerializationMode.VIA_DTO}：
 * <ul>
 *   <li>序列化输出稳定 DTO 结构（content + page 嵌套对象）</li>
 *   <li>彻底消除 {@code SpringDataJackson3Configuration.PageModule.WarningLoggingModifier} 的 WARNING</li>
 *   <li>不引入 spring-hateoas 依赖（VIA_HATEOAS 才需要）</li>
 * </ul>
 *
 * <p>输出结构：
 * <pre>
 * {
 *   "content": [...],
 *   "page": {
 *     "number": 0,
 *     "size": 10,
 *     "totalElements": 100,
 *     "totalPages": 10
 *   }
 * }
 * </pre>
 */
@Configuration
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class SpringDataWebConfig {
}
