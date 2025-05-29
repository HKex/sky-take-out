package com.sky.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Data
@AllArgsConstructor
@Slf4j
public class TCCOSUtil {

    private String baseUrl;
    private String accessKey;
    private String secretKey;
    private String regionName;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建COSCredentials实例。
        COSCredentials credentials = new BasicCOSCredentials(accessKey, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(regionName));
        COSClient cosClient = new COSClient(credentials, clientConfig);

        try {
            // 创建PutObject请求。
            InputStream inputStream = new ByteArrayInputStream(bytes);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream,null );
            cosClient.putObject(putObjectRequest);
        } catch (Exception e) {
            System.out.println("TCCOSUtil:文件上传失败："+ e.getMessage());
        }
        finally {
            cosClient.shutdown();
        }

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
//        StringBuilder stringBuilder = new StringBuilder("https://");
//        stringBuilder
//                .append(bucketName)
//                .append(".")
//                .append(endpoint)
//                .append("/")
//                .append(objectName);
//
//        log.info("文件上传到:{}", stringBuilder.toString());

        return String.valueOf(cosClient.getObjectUrl(bucketName, objectName));
    }
}
