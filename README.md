# 维维美桌宠

维维美桌宠是基于 Harry Huang 与 [Ark-Pets](https://github.com/isHarryh/Ark-Pets/tree/v3.13.1) 贡献者作品的 Windows 桌宠技术项目。本项目为非官方同人技术项目，不代表游戏开发商或发行商。

候选工程将窗口支撑、角色物理、位置存档、动作调度、合成音效以及命中时的目标锁定代码从原有渲染环境中拆出。示例使用 Java 17 和 Swing 绘制原创几何角色，可以拖动、落到示例窗口顶沿、沿顶沿行走、隐藏或显示，也可以播放六次程序绘制的命中示意。示例使用独立数据目录。

个人完整版本继续使用原来的启动入口与本地资源。角色模型、实际六表情爆炸画面、原生准星贴图、Spine 渲染器及个人版安装器均有单独的授权边界。候选工程的构建和运行只使用授权明确的依赖与占位图形。详见 [素材政策](ASSET_POLICY.md)。

## Windows 快速开始

准备 Windows 10 或 11 的图形桌面和 JDK 17，并让 `JAVA_HOME` 指向 JDK。Windows 实机验证见 [构建说明](BUILD.md)。第一次构建需要从 Gradle 和 Maven Central 获取构建依赖。

```powershell
.\gradlew.bat --no-daemon :core:test :desktop:build
.\gradlew.bat --no-daemon :desktop:run
```

打开示例后，拖动浮动角色，或点击「落到窗口顶沿」再点击「沿窗口走两步」。索敌示意使用可见的几何环与命中状态，节奏为示例所用。需要额外授权的资源由 [边界说明](docs/RUNTIME_BOUNDARY.md) 记录。

## 代码与来源

| 路径 | 内容 |
| --- | --- |
| `core` | 上游物理及顶沿遮挡算法、维维美落地与返岗状态、位置存档、动作策略、目标锁定、Swing 战斗菜单与程序化音效、无素材测试 |
| `desktop` | Windows 上运行的 Swing 示例、独立顶沿支撑、原创角色、启动与桌面冒烟检查 |
| `scripts` | 提交对象、整个 Git 对象库及本地构建产物检查 |

维维美新增了办公动作策略、窗口顶沿修正、短音合成、定序攻击和准星命中状态。此候选只合入可在独立示例中完整构建和回归的代码范围。保留 Harry Huang 与各源文件的原作者声明。项目源代码使用 [GPL-3.0](LICENSE)，第三方组件见 [许可证清单](THIRD_PARTY_LICENSES.md)。
