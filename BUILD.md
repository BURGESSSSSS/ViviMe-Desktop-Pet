# Windows 构建与运行

## 环境

需要 Windows 图形桌面、JDK 17 和 Git。运行资源检查还需要 Python 3.9 或更新版本。安装 N 个 JDK 时，用 `JAVA_HOME` 选中 JDK 17。应用构建从 Maven Central 获取依赖，Gradle Wrapper 从官方分发端点获取 Gradle 8.14.3，首次构建需要网络。运行示例不需要远程素材或服务。

```powershell
java -version
.\gradlew.bat --no-daemon :core:test :desktop:build :desktop:installDist
.\gradlew.bat --no-daemon :desktop:run
```

也可运行生成的 Windows 脚本：

```powershell
.\desktop\build\install\ViviMe-Desktop-Pet\bin\ViviMe-Desktop-Pet.bat
```

脚本使用本机 JDK。示例的数据默认写入 `%LOCALAPPDATA%\ViviMeDemo\placement.json`，与个人完整版使用不同目录。Gradle 的 `installDist` 只生成本地目录，当前没有 Windows 安装器。复验外部依赖范围和许可请运行 `:desktop:verifyRuntimeBoundary`。

## 检查

```powershell
python scripts/audit_release.py --index --objects --self-test
python scripts/audit_release.py --distribution desktop/build/install/ViviMe-Desktop-Pet
```

暂存前可启用本仓库提供的 Git 钩子。该配置只作用于当前仓库。

```powershell
git config --local core.hooksPath .githooks
```

在可见 Windows 桌面上运行冒烟检查。它会创建应用的两个真实窗口，验证落地、顶沿行走、隐藏与恢复，以及程序绘制的六次命中，随后输出截图和 JSON。关闭其它可能挡住示例窗口的测试程序可以得到清晰截图。

```powershell
$evidence = Join-Path ([IO.Path]::GetTempPath()) ('vivime-demo-' + [guid]::NewGuid().ToString('N'))
.\desktop\build\install\ViviMe-Desktop-Pet\bin\ViviMe-Desktop-Pet.bat "--smoke-dir=$evidence"
```

第一次构建完成后可重复使用 `--offline`，前提是沿用下载过相同版本依赖的 Gradle 缓存。
