package com.platform.api.gen;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 数据库字段类型 → Java 类型映射。
 *
 * <p>映射关系放在 classpath 配置文件中，不硬编码、不落库：
 * <ul>
 *     <li>{@code classpath:gen/MYSQL-MAPPING.yml}</li>
 *     <li>{@code classpath:gen/PGSQL-MAPPING.yml}</li>
 * </ul>
 * 按当前数据源的产品名选择映射文件；未命中的类型统一按 {@code String} 生成。
 */
@Component
public class DbTypeMappings {

    private static final String MYSQL_MAPPING_FILE = "classpath:gen/MYSQL-MAPPING.yml";

    private static final String PGSQL_MAPPING_FILE = "classpath:gen/PGSQL-MAPPING.yml";

    private final Map<String, String> mysqlMapping;

    private final Map<String, String> pgsqlMapping;

    public DbTypeMappings() {
        this(load(MYSQL_MAPPING_FILE), load(PGSQL_MAPPING_FILE));
    }

    DbTypeMappings(Map<String, String> mysqlMapping, Map<String, String> pgsqlMapping) {
        this.mysqlMapping = mysqlMapping;
        this.pgsqlMapping = pgsqlMapping;
    }

    /**
     * 根据数据库产品名解析映射配置。
     */
    public Mapping resolve(String databaseProductName) {
        String product = databaseProductName == null ? "" : databaseProductName.toLowerCase(Locale.ROOT);
        if (product.contains("postgres")) {
            return new Mapping(databaseProductName, pgsqlMapping, true);
        }
        return new Mapping(databaseProductName, mysqlMapping, false);
    }

    private static Map<String, String> load(String location) {
        Resource resource = new DefaultResourceLoader().getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("类型映射文件不存在: " + location);
        }
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(location, resource);
            Map<String, String> mapping = new LinkedHashMap<>();
            for (PropertySource<?> source : sources) {
                if (source instanceof MapPropertySource mapSource) {
                    mapSource.getSource().forEach((key, value) -> {
                        if (key != null && value != null) {
                            mapping.put(key.trim().toLowerCase(Locale.ROOT), String.valueOf(value).trim());
                        }
                    });
                }
            }
            if (mapping.isEmpty()) {
                throw new IllegalStateException("类型映射文件为空: " + location);
            }
            return Collections.unmodifiableMap(mapping);
        } catch (IOException e) {
            throw new IllegalStateException("读取类型映射文件失败: " + location, e);
        }
    }

    /**
     * 单个数据源的映射上下文。
     */
    public record Mapping(String productName, Map<String, String> types, boolean postgres) {

        /**
         * 去掉长度/修饰后的基础类型；MySQL 还要去掉 unsigned 等空格后缀。
         */
        public String baseType(String columnType) {
            if (columnType == null || columnType.isBlank()) {
                return "";
            }
            String type = columnType.toLowerCase(Locale.ROOT).trim();
            int parenthesis = type.indexOf('(');
            if (parenthesis >= 0) {
                type = type.substring(0, parenthesis).trim();
            }
            if (!postgres) {
                int space = type.indexOf(' ');
                if (space >= 0) {
                    type = type.substring(0, space).trim();
                }
            }
            return type;
        }

        /**
         * 未命中的类型按 String 兜底。
         */
        public String javaType(String columnType) {
            return types.getOrDefault(baseType(columnType), "String");
        }
    }
}
