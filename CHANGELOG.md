# 更新日志

## [2.0.4] - 2026-10-31

### Fixed
- **主题改用 REST 契约后「获取信息」按钮消失**：增强脚本原本只按 `/links/apply/submit`
  识别申请表单，而 PluginLinks 2.5 的访客申请走 `api.link.halo.run` 的 POST 接口，
  表单 `action` 不再固定。现在额外按字段特征（`url` + `displayName` + `logo`）兜底识别，
  两种契约的主题都能注入按钮。本插件仍然只读 `site-info`，不提交申请、不复制官方表单。

## [2.0.3] - 2026-10-31

### Fixed
- **非 UTF-8 站点标题乱码**：`site-info` 先用默认字符集把响应体解码成 String 再交给 Jsoup，
  `<meta charset>` 已无从补救，GBK/GB2312 站点抓回来的标题必然是乱码。改为按响应
  `Content-Type` 的字符集解析字节，缺失时交给 Jsoup 依据页面 meta 自动识别。
- **配置读取失败时反而放开**：`enableLinkPreview` 读取异常或缺失时会走到开启分支，
  这个开关本身就是保护，现改为读不到即按关闭处理（fail closed）。
- **响应超时后 NPE**：`block(10s)` 超时返回 `null` 会在后续解引用处抛 NPE，落进通用 502 分支；
  现在显式判空并返回统一的 502，同时给整条抓取链路加 12 秒总超时。
- **限流器表清零策略**：临界状态下每个新客户端都会触发一次万级全表排序，且发生在事件循环上；
  改为一次性淘汰到半水位，并把取 IP / 建限流器整体移到 `boundedElastic`。
- **每次抓取都新建 WebClient**：连接池与编解码器无法复用，高频调用下资源抖动；公共配置抽为静态
  builder，每次请求只 clone 后替换 connector。
- **控制台入口观察器开销**：`MutationObserver` 监听整个 body 子树且回调直接执行 DOM 查询，
  改为 rAF 合并同一帧内的多次变动。
- **增强按钮漏注入**：表单 `action` 写成相对路径时选择器匹配不到；改为按 `URL.pathname` 判断，
  并对异步渲染出来的表单加观察者兜底。

### Verification
- `./gradlew cleanTest test`：3 个测试类、15 项测试通过，0 失败 / 0 错误 / 0 跳过。
- Widget 与 UI 构建通过（Gradle `build` 链路内的 `vite build`）。
- 版本同步：`gradle.properties`、`build.gradle`、`plugin.yaml`、Widget 包版本均为 2.0.3。

## [2.0.2] - 2026-10-31

### Security
- **限流 key 可伪造**：`X-Forwarded-For` 原本取最左值，而这个值由客户端自带、可写成任意 IP，等于把
  `site-info` 的限流关掉。改为取最右一跳（可信代理亲眼看到的对端地址）。
- **事件循环被 DNS 阻塞**：`firstForwardedIp` 会先放行形似十六进制的任意字符串，再交给
  `InetAddress.getByName` 解析，`X-Forwarded-For: beef` 因此进入域名解析；该调用同步运行在 Netty
  事件循环上，几十个并发就能拖住全站接口。现在改为纯字面量解析，完全不做 DNS 查询。
- **端口未限制**：URL 校验原本不限端口，匿名接口可用于探测 6379(Redis)、25(SMTP)、9200(ES) 等
  非 HTTP 服务是否存活。现在只放行 80 与 443。
- **SSRF 探测 oracle**：400 响应不再回显异常原文（内网地址被拒 / 主机无法解析 / 端口不允许），
  具体原因只写服务端日志。

### Verification
- Java 单测 15 项通过（`cleanTest test`：`AnonymousEndpointRateLimitTest`、`IpAddressUtilsTest`、`SafeUrlValidatorTest`，0 失败 / 0 错误 / 0 跳过）；新增用例覆盖「取最右一跳」「非字面量不做解析」「IPv4 越界回落」「非 Web 端口拒绝」。
- Widget 构建通过（`vite build --config vite.config.lib.ts`：`link-apply-enhance.iife.js` 4.85 kB）。
- Gradle 校验任务全部通过：`verifyVersionConsistency`（Gradle / `plugin.yaml` / `gradle.properties` / Widget 包版本一致）、`verifyPublicationMetadata`、`verifyEnhancementBoundary`、`verifyWidgetRuntimeAsset`。
- 制品抽查：`link-submit-next-2.0.2.jar` 由当前源码构建。

## [2.0.1] - 2026-10-30

### Changed
- 「获取信息」按钮改为网址输入框右侧的小地球图标（`mdi:web-refresh`，与官方友链后台编辑链接弹窗一致），带「获取网站信息」提示与请求中转圈状态，不再占用表单下方单独一行。
- `site-info` 解析顺序对齐官方后台 `link.detail`：`<title>` / `meta[name=description]` / `link[rel~=icon]` 优先，`og:*` 仅在标准标签缺失时兜底；Logo 不再优先取 `og:image`。

### Verification
- Widget 单测 14 项通过（`vitest --run`，`src/site-info.test.ts`）；Widget `eslint "src/**/*.ts" --max-warnings=0` 零告警。
- Java 单测 10 项通过（`cleanTest test`：`AnonymousEndpointRateLimitTest`、`IpAddressUtilsTest`、`SafeUrlValidatorTest`，0 失败 / 0 错误 / 0 跳过）。
- Console UI `eslint . --max-warnings=0` 零告警。
- Gradle 校验任务全部通过：`verifyVersionConsistency`（Gradle / `plugin.yaml` / `gradle.properties` / Widget 包版本一致）、`verifyPublicationMetadata`（Halo `>= 2.27.0`、`PluginLinks` 2.x 依赖区间）、`verifyEnhancementBoundary`（不含 `LinkSubmit` 模型、`linksubmits`、`link-applications` 等旧边界）、`verifyWidgetRuntimeAsset`（运行时脚本契约）。
- 完整安装包已构建并抽查：`link-submit-next-2.0.1.jar` 含 `plugin.yaml`（`version: 2.0.1`、`requires: ">=2.27.0"`）、`static/link-apply-enhance.iife.js`、`ui/ui-plugin.json`。

## [2.0.0] - 2026-10-29

### Removed
- 删除自建申请弹窗（Lit `LinkSubmitWidget` 组件、`var.css` 主题变量）以及插件内的验证码调用与申请提交逻辑：表单、提交、验证码、匿名限流、审核与通知统一由官方 `PluginLinks` 和主题负责。
- 删除旧版 `LinkSubmit` / `CronLinkSubmit` 扩展模型、对应 Console 列表页与 CSV 工具、OpenAPI 生成客户端。
- 删除前台浮动“提交友链”按钮和 `GET configuration` 匿名接口；设置项从 3 个开关减为 2 个（移除 `displayTheSubmitButton`）。

### Added
- 新增官方表单增强脚本 `link-apply-enhance.iife.js`：识别 `form[action*="links/apply/submit"]`，在网址字段旁注入“获取信息”按钮，调用 `site-info` 后只填充空白或上一次自动填入的 `displayName` / `logo` / `description`，并给出 `role="status"` 的无障碍反馈。
- 注入脚本带 `data-link-preview-enabled`，后台关闭“自动获取网站信息”时不再出现按钮，接口返回 403。

### Changed
- `AnonymousEndpoint` 仅保留 `GET site-info`，安全边界（公网地址校验、逐跳重定向复核、DNS 固定解析、响应体与超时限制、按来源 IP 限流与 RFC 7807 错误）保持不变。
- 页脚注入器更名为 `LinkApplyEnhanceFooterProcessor`，只输出一个脚本标签。
- 增强边界与运行时资源校验任务改为断言新契约：不得出现 `LinkSubmit` 数据模型、提交写入 API、`link-applications`/验证码调用或旧弹窗资源。

### Compatibility
- 仍要求 Halo `>= 2.27.0` 与官方 `PluginLinks >= 2.3.0 & < 3.0.0`。
- 破坏性变更：升级后前台申请入口必须由主题按官方接口渲染申请表单（NUCMA 主题已适配），不再存在 `LinkSubmitWidget.open()`。

## [1.2.9] - 2026-10-12

### Changed
- 网站信息抓取使用 Halo 2.26 的安全 HTTP 客户端与响应体大小过滤器，同时保留 DNS 固定解析和逐跳公网地址校验。

### Verification
- Java 测试通过；完整 JAR 打包待执行。

## [1.2.8] - 2026-09-22

### Changed
- 加强网站信息抓取的公网地址校验，逐个重定向重新解析并拒绝保留地址段、IPv4-mapped IPv6 和本机主机名。
- 保留官方 PluginLinks 申请边界与原有安全限制。

### Verification
- 通过 Java 测试、Console lint/type checks、Widget 构建、测试和 lint。

## [1.2.6] - 2026-08-20

### Changed
- 插件安装后默认启用；仍要求 Halo 官方 `PluginLinks >= 2.3.0`。
- 保持申请、审核、通知、限流、清理和正式友链数据由官方链接插件负责，本插件仅提供前台增强与旧记录只读兼容。
- 前端 Console 与 Widget 增加不修改源码的 lint 质量门禁，并忽略 OpenAPI 生成代码的格式噪声。
- 同步 Gradle、插件 manifest、Widget 包版本，并校验 `gradle.properties` 版本一致性。

### Verification
- 通过 Java 测试、Console 类型检查、Console lint、Widget 构建、Widget 测试、Widget lint、增强边界、发布元数据和运行时资源契约。
- 完整安装包为 `link-submit-next-1.2.6.jar`。

## [1.2.5] - 2026-08-20

### Changed
- 改为 Halo 官方 `PluginLinks` 的友链申请增强插件，删除新的自有申请、审核和通知写入流程。
- 保留旧版申请记录只读兼容，并禁止复用上游应用商店 ID。
