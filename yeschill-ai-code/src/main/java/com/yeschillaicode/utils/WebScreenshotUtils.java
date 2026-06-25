package com.yeschillaicode.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.yeschillaicode.exception.BusinessException;
import com.yeschillaicode.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 截图工具类
 * 使用 ThreadLocal + 固定线程池方案：
 * - 每个线程绑定一个独立的 ChromeDriver，互不干扰
 * - 线程池固定 2 个线程，最多同时截 2 张图
 * - 60 秒超时兜底，截图失败不阻塞主流程
 */
@Slf4j
@Component
public class WebScreenshotUtils {

    private static final ExecutorService SCREENSHOT_POOL = Executors.newFixedThreadPool(1, r -> {
        Thread t = new Thread(r, "screenshot-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    });

    private static final ThreadLocal<WebDriver> DRIVER_HOLDER = ThreadLocal.withInitial(() -> {
        WebDriverManager.chromedriver().useMirror().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1600,900");
        options.addArguments("--disable-extensions");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        return driver;
    });

    private static WebDriver getHealthyDriver() {
        WebDriver driver = DRIVER_HOLDER.get();
        try {
            driver.getWindowHandles(); // 快速健康检查
            return driver;
        } catch (WebDriverException e) {
            log.warn("WebDriver 失效，重建中");
            try { driver.quit(); } catch (Exception ignored) {}
            DRIVER_HOLDER.remove();
            WebDriver newDriver = DRIVER_HOLDER.get(); // 触发 initialValue 重建
            return newDriver;
        }
    }

    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("网页URL不能为空");
            return null;
        }

        Future<String> future = SCREENSHOT_POOL.submit(() -> {
            WebDriver driver = getHealthyDriver();

            String rootPath = System.getProperty("user.dir") + File.separator
                    + "tmp" + File.separator + "screenshots"
                    + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);

            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + ".png";

            driver.get(webUrl);
            waitForPageLoad(driver);

            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            saveImage(screenshotBytes, imageSavePath);

            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + "_compressed.jpg";
            compressImage(imageSavePath, compressedImagePath);
            FileUtil.del(imageSavePath);

            return compressedImagePath;
        });

        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("截图超时 (60s): {}", webUrl);
            future.cancel(true);
            return null;
        } catch (Exception e) {
            log.error("截图失败: {}", webUrl, e);
            return null;
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("正在关闭截图线程池和 ChromeDriver...");
        SCREENSHOT_POOL.shutdownNow();
        // 清理 ThreadLocal 中绑定的 ChromeDriver
        try {
            WebDriver driver = DRIVER_HOLDER.get();
            if (driver != null) {
                driver.quit();
            }
        } catch (Exception ignored) {}
        DRIVER_HOLDER.remove();
        // 杀掉残留的 Chrome 进程
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                Runtime.getRuntime().exec(new String[]{"pkill", "-f", "chromedriver"});
                Runtime.getRuntime().exec(new String[]{"pkill", "-f", "chrome.*headless"});
            } else if (os.contains("windows")) {
                Runtime.getRuntime().exec(new String[]{"taskkill", "/F", "/IM", "chromedriver.exe"});
                Runtime.getRuntime().exec(new String[]{"taskkill", "/F", "/IM", "chrome.exe"});
            }
        } catch (Exception e) {
            log.error("清理 Chrome 进程失败", e);
        }
    }

    private static void waitForPageLoad(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
                            .equals("complete"));
            Thread.sleep(2000);
        } catch (Exception e) {
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }

    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("保存图片失败: {}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    private static void compressImage(String originalImagePath, String compressedImagePath) {
        try {
            ImgUtil.compress(
                    FileUtil.file(originalImagePath),
                    FileUtil.file(compressedImagePath),
                    0.3f);
        } catch (Exception e) {
            log.error("压缩图片失败: {} -> {}", originalImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
        }
    }
}
