package com.yeschillaicode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yeschillaicode.ai.AiCodeGenTypeRoutingService;
import com.yeschillaicode.ai.AiCodeGenTypeRoutingServiceFactory;
import com.yeschillaicode.constant.AppConstant;
import com.yeschillaicode.core.AiCodeGeneratorFacade;
import com.yeschillaicode.core.bulider.VueProjectBuilder;
import com.yeschillaicode.core.handler.StreamHandlerExecutor;
import com.yeschillaicode.exception.BusinessException;
import com.yeschillaicode.exception.ErrorCode;
import com.yeschillaicode.exception.ThrowUtils;
import com.yeschillaicode.langgraph4j.CodeGenWorkflow;
import com.yeschillaicode.mapper.AppMapper;
import com.yeschillaicode.model.dto.app.AppAddRequest;
import com.yeschillaicode.model.dto.app.AppQueryRequest;
import com.yeschillaicode.model.entity.App;
import com.yeschillaicode.model.entity.User;
import com.yeschillaicode.model.enums.ChatHistoryMessageTypeEnum;
import com.yeschillaicode.model.enums.CodeGenTypeEnum;
import com.yeschillaicode.model.vo.User.UserVO;
import com.yeschillaicode.model.vo.app.AppVO;
import com.yeschillaicode.service.AppService;
import com.yeschillaicode.service.ChatHistoryService;
import com.yeschillaicode.service.ScreenshotService;
import com.yeschillaicode.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
@Slf4j
@Service
@EnableScheduling
public class AppServiceImpl  extends ServiceImpl<AppMapper, App>  implements AppService {
    @Resource
    private UserService userService;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
  @Resource
    private ChatHistoryService chatHistoryService;
  @Resource
  private StreamHandlerExecutor streamHandlerExecutor;
  @Resource
  private VueProjectBuilder vueProjectBuilder;
    @Resource
    private ScreenshotService screenshotService;
    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;
    @Resource
    private CodeGenWorkflow codeGenWorkflow;
    @Value("${code.deploy-host:http://localhost}")
    private String codeDeployHost;

    @Override
    public Flux<String> chatToGenCode(Long appid, String message, User loginUser) {
        //1.参数校验
        ThrowUtils.throwIf(appid == null||appid<=0, ErrorCode.PARAMS_ERROR, "应用id错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        //2.查询应用信息
        App app = this.getById(appid);
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        //3.权限校验，校验应用是不是该用户的
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限");
        //4.获取应用的代码类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码类型错误");
                }
        //5 在调用ai前，先把用户的消息插入到数据库
        chatHistoryService.addChatMessage(appid, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        //调用ai生成代码(流式)
        Flux<String> contentStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appid);
        //收集ai响应的内容，并且在完成后保存记录到对话历史
      return    streamHandlerExecutor.doExecute(contentStream, chatHistoryService, appid, loginUser, codeGenTypeEnum);

    }

    /**
     * 工作流模式生成代码（SSE步骤事件）
     */
    public SseEmitter chatToGenCodeWithWorkflow(Long appId, String message, User loginUser) {
        //1.参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用id错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        //2.查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        //3.权限校验
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限");
        //4.获取应用的代码类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码类型错误");
        }
        //5.保存用户消息
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        //6.判断是否首次生成
        boolean isFirst = isFirstGeneration(appId);
        //7.执行工作流，完成后保存聊天历史
        return codeGenWorkflow.executeWorkflowWithSse(appId, message, codeGenTypeEnum, isFirst, () -> {
            try {
                chatHistoryService.addChatMessage(appId, "工作流模式代码生成完成", ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
            } catch (Exception e) {
                log.error("保存工作流聊天历史失败: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 判断是否为首次生成（无聊天历史）
     */
    private boolean isFirstGeneration(Long appId) {
        try {
            long count = chatHistoryService.countByAppId(appId);
            return count == 0;
        } catch (Exception e) {
            log.error("查询聊天历史数量失败: {}", e.getMessage(), e);
            return true;
        }
    }



    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型（多例模式）
        AiCodeGenTypeRoutingService routingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        CodeGenTypeEnum selectedCodeGenType = routingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 保存生成模式，默认快速模式
        String genMode = StrUtil.isBlank(appAddRequest.getGenMode()) ? "normal" : appAddRequest.getGenMode();
        app.setGenMode(genMode);
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }


    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. 确定部署目录路径
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;

        // 8. Vue 项目特殊处理：改为异步构建，不阻塞部署响应
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目标记为排队中，构建由消费者异步执行
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setDeployKey(deployKey);
            updateApp.setDeployedTime(LocalDateTime.now());
            updateApp.setDeployStatus("QUEUED");
            this.updateById(updateApp);

            // 先复制源码到部署目录（构建完成后会覆盖为 dist）
            try {
                FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
            }

            String appDeployUrl = String.format("%s/%s/", codeDeployHost, deployKey);
            return appDeployUrl;
        }
       // 9. 复制文件到部署目录

        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 10. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 11. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", codeDeployHost, deployKey);
        // 12. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;

    }



    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            try {
                // 调用截图服务生成截图并上传
                String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
                if (StrUtil.isNotBlank(screenshotUrl)) {
                    // 更新应用封面字段
                    App updateApp = new App();
                    updateApp.setId(appId);
                    updateApp.setCover(screenshotUrl);
                    this.updateById(updateApp);
                }
            } catch (Exception e) {
                log.error("异步截图失败, appId={}, url={}", appId, appUrl, e);
            }
        });
    }



    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean removeById(Serializable id) {
        if(id == null){
            return false;
        }
        //这个方法传的id是序列化id，需要转化类型
        long appId = Long.parseLong(id.toString());
        // 检查应用是否存在
        if(appId<=0){
            return false;
        }
        App app = this.getById(appId);
        if (app == null) {
            return false;
        }
        //关联删除历史会话记录
        try{
            chatHistoryService.deleteByappid(appId);
        }catch (Exception e){
           log.error("删除应用关联的历史会话记录失败:{}",e.getMessage());
        }
        return super.removeById(id);
    }

    /**
     * 定时消费构建队列：扫描 QUEUED 状态的应用，串行执行 npm 构建
     */
    @Scheduled(fixedDelay = 5000)
    public void consumeBuildQueue() {
        QueryWrapper query = QueryWrapper.create()
                .eq("deployStatus", "QUEUED")
                .orderBy("deployedTime", true)
                .limit(1);
        App app = this.getOne(query);
        if (app == null) return;

        app.setDeployStatus("BUILDING");
        this.updateById(app);

        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator
                + app.getCodeGenType() + "_" + app.getId();

        log.info("开始异步构建 Vue 项目: {}", sourceDirPath);
        boolean success = vueProjectBuilder.buildProject(sourceDirPath);
        log.info("Vue 项目构建{}: {}", success ? "成功" : "失败", sourceDirPath);

        if (success) {
            File distDir = new File(sourceDirPath, "dist");
            if (distDir.exists()) {
                String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR
                        + File.separator + app.getDeployKey();
                FileUtil.copyContent(distDir, new File(deployDirPath), true);
            }
            // 构建完成后截图
            String appDeployUrl = String.format("%s/%s/", codeDeployHost, app.getDeployKey());
            generateAppScreenshotAsync(app.getId(), appDeployUrl);
        }

        App update = new App();
        update.setId(app.getId());
        update.setDeployStatus(success ? "DONE" : "FAILED");
        this.updateById(update);
    }

    @Override
    public String getDeployStatus(Long appId) {
        App app = this.getById(appId);
        return app != null && app.getDeployStatus() != null
                ? app.getDeployStatus() : "NONE";
    }

}





