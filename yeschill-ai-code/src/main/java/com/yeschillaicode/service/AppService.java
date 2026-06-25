package com.yeschillaicode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yeschillaicode.model.dto.app.AppAddRequest;
import com.yeschillaicode.model.dto.app.AppQueryRequest;
import com.yeschillaicode.model.entity.App;
import com.yeschillaicode.model.entity.User;
import com.yeschillaicode.model.vo.app.AppVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 */
public interface AppService extends IService<App> {

/**
 * 根据App实体对象获取对应的AppVO（View Object）对象
 * AppVO通常用于视图层展示，可能包含与App实体不同格式或结构的数据
 *
 * @param app App实体对象，包含应用的基本信息
 * @return AppVO 视图对象，用于前端展示或数据传输
 */
     AppVO getAppVO(App app) ;

    String deployApp(Long appId, User loginUser);

    void generateAppScreenshotAsync(Long appId, String appUrl);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);


     List<AppVO> getAppVOList(List<App> appList);
     Flux<String> chatToGenCode(Long appid, String message, User loginUser);

    /**
     * 工作流模式生成代码
     */
    SseEmitter chatToGenCodeWithWorkflow(Long appId, String message, User loginUser);

    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 查询应用部署状态
     */
    String getDeployStatus(Long appId);
}
