package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.tccos")
@Data
public class TCCOSProperties {
    private String baseUrl;
    private String accessKey;
    private String secretKey;
    private String regionName;
    private String bucketName;

}
