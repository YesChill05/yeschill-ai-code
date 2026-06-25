package com.yeschillaicode.langgraph4j.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yeschillaicode.langgraph4j.model.ImageResource;
import com.yeschillaicode.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片搜索工具（Pixabay API，根据关键词搜索内容图片）
 */
@Slf4j
@Component
public class ImageSearchTool {

    private static final String PIXABAY_API_URL = "https://pixabay.com/api/";

    @Value("${pixabay.api-key:}")
    private String pixabayApiKey;

    @Tool("搜索内容相关的图片，用于网站内容展示")
    public List<ImageResource> searchContentImages(@P("搜索关键词") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        int searchCount = 12;
        try (HttpResponse response = HttpRequest.get(PIXABAY_API_URL)
                .form("key", pixabayApiKey)
                .form("q", query)
                .form("image_type", "photo")
                .form("per_page", searchCount)
                .form("page", 1)
                .timeout(10000)
                .execute()) {
            if (response.isOk()) {
                JSONObject result = JSONUtil.parseObj(response.body());
                JSONArray hits = result.getJSONArray("hits");
                for (int i = 0; i < hits.size(); i++) {
                    JSONObject hit = hits.getJSONObject(i);
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.CONTENT)
                            .description(hit.getStr("tags", query))
                            .url(hit.getStr("webformatURL"))
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Pixabay API 调用失败: {}", e.getMessage(), e);
        }
        return imageList;
    }
}
