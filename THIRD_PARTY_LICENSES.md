# 第三方组件

| 组件 | 版本 | 用途与许可 |
| --- | --- | --- |
| Ark-Pets | 3.13.1 | 采用的源代码为 GPL-3.0。保留版权声明与完整许可证。[上游](https://github.com/isHarryh/Ark-Pets/tree/v3.13.1) |
| Gradle Wrapper | 8.14.3 | 仓库随附的唯一第三方 JAR 和启动脚本。Apache-2.0。Wrapper JAR 与分发 ZIP 均按 Gradle 官方 SHA-256 核对。[上游代码](https://github.com/gradle/gradle/tree/v8.14.3) · [Gradle 校验值](https://gradle.org/release-checksums/#8.14.3) |
| Fastjson2 | 2.0.60 | 位置与动作配置的 JSON 解析，示例的唯一外部运行库，Apache-2.0。其 JAR 内嵌的 ASM 片段保留三条款 BSD 版权及许可告知。构建从 Maven Central 解析。[上游](https://github.com/alibaba/fastjson2/tree/2.0.60) · [本地 Apache-2.0](licenses/Apache-2.0.txt) · [告知](licenses/FASTJSON2-NOTICE.txt) |
| JUnit Jupiter / JUnit Platform | 5.11.4 / 1.11.4 | 测试采用 API、参数化测试、引擎、平台 Commons、Engine 和 Launcher。EPL-2.0，仅测试 classpath。[上游](https://github.com/junit-team/junit5/tree/r5.11.4) |
| OpenTest4J | 1.3.0 | JUnit 传递测试依赖，Apache-2.0。[上游](https://github.com/ota4j-team/opentest4j/tree/r1.3.0) |
| API Guardian | 1.1.2 | JUnit 传递测试依赖，Apache-2.0。[上游](https://github.com/apiguardian-team/apiguardian/tree/r1.1.2) |
| JDK 17 | 17 | 示例使用本机安装的 `java.desktop` 等标准模块。当前源代码和 Gradle 分发目录均未嵌入任何 JDK。 |

个人版原有的 libGDX、LWJGL、ANGLE、JavaFX、JNA、Sentry 及 Spine 等无需参与该候选的运行。若将来重新接入其它依赖，先核对实际解析的组件、归档内容和许可，再修改锁定的运行范围。
