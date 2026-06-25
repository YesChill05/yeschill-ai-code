from playwright.sync_api import sync_playwright
import os

DIR = os.path.join(os.path.dirname(__file__), "docs", "screenshots")
os.makedirs(DIR, exist_ok=True)
BASE = "http://localhost:5173"


def shot(page, name, full=True):
    path = os.path.join(DIR, name)
    page.screenshot(path=path, full_page=full)
    print(f"  -> {name} ({os.path.getsize(path) / 1024:.0f}KB)")


def smart_full_shot(page, name, padding=40):
    """先测量内容高度再滚回顶部裁剪，避免大片白边"""
    bottom = page.evaluate("""() => {
        let maxBottom = 0;
        const walk = (el) => {
            if (el.nodeType !== 1) return;
            const r = el.getBoundingClientRect();
            const b = r.bottom + window.scrollY;
            if (b > maxBottom) maxBottom = b;
            for (const child of el.children) walk(child);
        };
        walk(document.body);
        return Math.ceil(maxBottom);
    }""")
    page.evaluate("window.scrollTo(0, 0)")
    page.wait_for_timeout(500)
    path = os.path.join(DIR, name)
    page.screenshot(path=path, clip={"x": 0, "y": 0, "width": 1440, "height": bottom + padding})
    print(f"  -> {name} height={bottom}px ({os.path.getsize(path) / 1024:.0f}KB)")


def login(page):
    """登录 yeschill 管理员账号"""
    page.goto(f"{BASE}/user/login", wait_until="networkidle", timeout=30000)
    page.wait_for_timeout(2000)
    page.locator('input[placeholder="请输入账号"]').fill("yeschill")
    page.locator('input[placeholder="请输入密码"]').fill("12345678")
    page.locator('button[type="submit"]').click()
    page.wait_for_url("**/", timeout=15000)
    page.wait_for_timeout(3000)


with sync_playwright() as p:
    browser = p.chromium.launch(channel="chrome", headless=True)
    ctx = browser.new_context(viewport={"width": 1440, "height": 900}, locale="zh-CN")

    # ── 1. 登录页 ──
    print("[1/11] Login page")
    page = ctx.new_page()
    page.goto(f"{BASE}/user/login", wait_until="networkidle", timeout=30000)
    page.wait_for_selector("h2", timeout=10000)
    page.wait_for_timeout(2000)
    shot(page, "01-login-page.png")
    page.close()

    # ── 2. 注册页 ──
    print("[2/11] Register page")
    page = ctx.new_page()
    page.goto(f"{BASE}/user/register", wait_until="networkidle", timeout=30000)
    page.wait_for_timeout(2000)
    shot(page, "01-register-page.png")
    page.close()

    # ── 3. 登录 ──
    print("[3/11] Login as yeschill")
    page = ctx.new_page()
    login(page)
    page.close()

    # ── 4. 首页 Hero（已登录） ──
    print("[4/11] Homepage hero")
    page = ctx.new_page()
    page.goto(f"{BASE}/", wait_until="networkidle", timeout=30000)
    page.wait_for_selector("h1", timeout=10000)
    page.wait_for_timeout(2000)
    page.set_viewport_size({"width": 1440, "height": 750})
    shot(page, "02-homepage-hero.png", full=False)
    page.close()

    # ── 5. 工作流模式创建 ──
    print("[5/11] Create + workflow mode")
    page = ctx.new_page()
    page.goto(f"{BASE}/", wait_until="networkidle", timeout=30000)
    page.wait_for_selector("textarea", timeout=10000)
    page.wait_for_timeout(2000)
    page.locator("textarea").fill("做一个响应式企业官网，包含产品展示、团队介绍和联系我们")
    page.wait_for_timeout(300)
    wf = page.locator("text=工作流模式").first
    if wf.is_visible(timeout=2000):
        wf.click()
        page.wait_for_timeout(500)
    page.set_viewport_size({"width": 1440, "height": 750})
    shot(page, "03-homepage-create-workflow.png", full=False)
    page.close()

    # ── 6. 我的应用 + 精选（smart crop 去白边） ──
    print("[6/11] My apps + featured")
    page = ctx.new_page()
    page.goto(f"{BASE}/", wait_until="networkidle", timeout=30000)
    page.wait_for_timeout(3000)
    page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
    page.wait_for_timeout(2000)
    smart_full_shot(page, "04-homepage-my-apps.png")
    page.close()

    # ── 7. 聊天页 ──
    print("[7/11] Chat page")
    page = ctx.new_page()
    page.goto(f"{BASE}/", wait_until="networkidle", timeout=30000)
    page.wait_for_timeout(3000)
    try:
        card = page.locator(".ant-card").first
        if card.is_visible(timeout=3000):
            card.click()
            page.wait_for_load_state("networkidle", timeout=15000)
            page.wait_for_timeout(2000)
    except Exception as e:
        print(f"  No card: {e}")
    shot(page, "06-chat-page.png")
    page.close()

    # ── 8. 个人中心 ──
    print("[8/11] User center")
    page = ctx.new_page()
    page.goto(f"{BASE}/user/center", wait_until="networkidle", timeout=30000)
    page.wait_for_timeout(3000)
    shot(page, "07-user-center.png")
    page.close()

    # ── 9. 管理后台 - 应用管理 ──
    print("[9/11] Admin - App manage")
    page = ctx.new_page()
    page.goto(f"{BASE}/admin/appManage", wait_until="networkidle", timeout=30000)
    page.wait_for_timeout(3000)
    shot(page, "08-admin-app-manage.png")
    page.close()

    # ── 10. 管理后台 - 用户管理 ──
    print("[10/11] Admin - User manage")
    page = ctx.new_page()
    page.goto(f"{BASE}/admin/userManage", wait_until="networkidle", timeout=30000)
    page.wait_for_timeout(3000)
    shot(page, "09-admin-user-manage.png")
    page.close()

    # ── 11. 管理后台 - 对话管理 ──
    print("[11/11] Admin - Chat manage")
    page = ctx.new_page()
    page.goto(f"{BASE}/admin/chatManage", wait_until="networkidle", timeout=30000)
    page.wait_for_timeout(3000)
    shot(page, "10-admin-chat-manage.png")
    page.close()

    # ── 12. API 文档 ──
    print("[12/11] API docs")
    page = ctx.new_page()
    page.goto("http://localhost:1113/api/doc.html", wait_until="networkidle", timeout=30000)
    page.wait_for_timeout(3000)
    shot(page, "05-api-docs.png")
    page.close()

    browser.close()
    print("\nDone!")
