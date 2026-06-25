package com.yeschillaicode;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@EnableCaching
@MapperScan("com.yeschillaicode.mapper")


public class YeschillAiCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(YeschillAiCodeApplication.class, args);
    }

}
