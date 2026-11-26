# Windows 免安装预览

适用于 Windows 10 或 11（x64，需系统 .NET Framework 4.8）。下载 `ViviMe-Desktop-Pet-v0.1.0-core-preview-windows-x64.zip`，完整解压后双击 `ViviMeCorePreview.exe`。

首次启动需要联网。确认提示后，启动器会从 Eclipse Adoptium 官方 GitHub 下载约 44 MB 的 Eclipse Temurin Java 17。下载文件经 SHA-256 核对后解压到 `%LOCALAPPDATA%\ViviMeCorePreview\java`，供本预览重复使用。运行时由用户直接从供应商获取。启动器无需管理员权限，也不修改系统 Java 或开机设置。[Temurin 17.0.21+9 官方发布页](https://github.com/adoptium/temurin17-binaries/releases/tag/jdk-17.0.21%2B9)

窗口内可以拖动原创几何角色、沿示例窗口顶沿移动，以及观看以几何图形呈现的六次命中。演示使用 `%LOCALAPPDATA%\ViviMeDemo` 保存位置。解压目录、下载的运行时和演示存档可以各自删除。

此预览只提供核心技术演示。启动器未签名，Windows 可能显示信誉提示。每次发布所附源码归档包含 Java 示例及 `scripts/windows-preview/` 中的启动器源码和打包脚本，项目许可证及依赖声明见包内的 `LICENSE`、`NOTICE.md` 和 `THIRD_PARTY_LICENSES.md`。

从同一版本的源码构建 ZIP（在 x64 Windows 上使用 JDK 17 和系统 .NET Framework）。

```powershell
.\gradlew.bat --no-daemon :core:test :desktop:installDist
.\scripts\windows-preview\Build-Preview.ps1 -OutputDirectory .\desktop\build\windows-preview
```
