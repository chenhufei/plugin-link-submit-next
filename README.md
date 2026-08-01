# Plugin Link Submit Next

> 基于 `chengzhongxue/link-submit` 二次开发的 Halo 友链提交插件升级版。

## ✨ 核心优化与特性

- **跨域(CORS)请求解决**：内置 `/site-info` 后端代理接口。当用户提交网站链接时，由后端代为请求目标网站获取标题、描述等 Meta 信息，彻底解决前端直接 Fetch 导致的跨域拦截问题。
- **UI 交互升级**：优化了提交表单的交互体验，使其更符合现代主题的风格。
- **后台管理**：支持管理员在 Halo 后台中审批和管理访客提交的友情链接。

## 📦 安装与使用

1. 下载最新版本的 `.jar` 插件包。
2. 在 Halo 后台的 **插件管理** 页面上传并启用该插件。
3. 在友链页面（或指定页面）使用短代码/组件渲染友链提交表单。

## 🛠️ 开发指南

```bash
# 编译并构建插件包
./gradlew build

# 插件打包产物位于
# build/libs/plugin-link-submit-next-x.x.x.jar
```
