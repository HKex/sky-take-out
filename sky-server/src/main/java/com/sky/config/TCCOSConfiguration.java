package com.sky.config;

import com.sky.properties.TCCOSProperties;
import com.sky.utils.TCCOSUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class TCCOSConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TCCOSUtil tccosUtil(TCCOSProperties tccosProperties) {
        log.info("开始创建腾讯云文件上传客户端：{}", tccosProperties);
        return new TCCOSUtil(
                tccosProperties.getBaseUrl(),
                tccosProperties.getAccessKey(),
                tccosProperties.getSecretKey(),
                tccosProperties.getRegionName(),
                tccosProperties.getBucketName());
    }
}
