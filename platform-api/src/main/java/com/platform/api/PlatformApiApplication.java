package com.platform.api;

import com.platform.api.config.AppProperties;
import com.platform.api.config.CaptchaProperties;
import com.platform.api.config.JwtProperties;
import com.platform.api.config.OAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        AppProperties.class,
        CaptchaProperties.class,
        JwtProperties.class,
        OAuthProperties.class,
})
public class PlatformApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }
}
