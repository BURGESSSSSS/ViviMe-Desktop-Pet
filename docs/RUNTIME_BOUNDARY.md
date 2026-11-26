# 运行时与素材边界

核心使用 JDK 17 的数据类型及 Fastjson2，示例的窗口和几何特效由 Swing 实现。原有物理算法和落地状态机运行在实际桌面窗口上，目前示例只把自己的普通窗口作为支撑面。个人版 Win32 全桌面碰撞、透明输入转发、系统托盘集成和安装器保留在受限工程中。轨迹、动作、菜单绘制和索敌代码可复用，具体个人版渲染层仍需单独处理。

完整个人版依赖 `com.esotericsoftware.spine:spine-libgdx:3.8.99.1`，缓存 POM 表示 Spine Runtime License v2.5。历史 3.8 分支的许可证和目前的 Runtime/Editor 条款都对集成与分发另设条件。本候选的 Gradle 图没有该依赖或可运行的适配模块。[历史 3.8 许可证](https://github.com/EsotericSoftware/spine-runtimes/blob/3.8/LICENSE) · [当前 Runtime 条款](https://esotericsoftware.com/spine-runtimes-license) · [当前 Editor 条款](https://esotericsoftware.com/spine-editor-license)

厂商提到可选模块是一种可讨论的架构。GPL 方面仍需考虑实际链接、进程通信、组合分发及是否需要由相关版权人授权例外。未来可评估只使用独立协议、进程和用户单独提供素材的适配器，但在权利方或专业人士确认之前，不能依赖模块开关、安装时下载或改包名取得分发许可。[Esoteric Software 官方支持](https://esotericsoftware.com/support) · [GNU GPL FAQ](https://www.gnu.org/licenses/gpl-faq.html#GPLIncompatibleLibs)
