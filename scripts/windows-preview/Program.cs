// Copyright (c) 2026 ViviMe project contributors. SPDX-License-Identifier: GPL-3.0-only
using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Net;
using System.Security.Cryptography;
using System.Threading;
using System.Windows.Forms;

internal static class Program
{
    private const string Title = "维维美桌宠 · 核心技术预览";
    private const string RuntimeDirectory = "jdk-17.0.21+9-jre";
    private const string RuntimeZip = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jre_x64_windows_hotspot_17.0.21_9.zip";
    private const string RuntimeSha256 = "fe4656bd78fb09812dc38c49da777f79cc33e7ee435a254be82f9e7f40d9d117";

    [STAThread]
    private static int Main()
    {
        Application.EnableVisualStyles();
        try
        {
            if (!Environment.Is64BitOperatingSystem)
                throw new PlatformNotSupportedException("此下载包需要 64 位 Windows。");

            string app = AppDomain.CurrentDomain.BaseDirectory;
            string libraries = Path.Combine(app, "lib");
            if (!File.Exists(Path.Combine(libraries, "desktop-0.1.0-core-preview.jar")))
                throw new FileNotFoundException("找不到程序文件。请解压整个 ZIP，再双击启动器。");

            string storage = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "ViviMeCorePreview", "java");
            Directory.CreateDirectory(storage);
            string java = Path.Combine(storage, RuntimeDirectory, "bin", "javaw.exe");
            // Serialize the installation across concurrent launches in this account/session.
            using (var installationLock = new FileStream(Path.Combine(storage, "install.lock"), FileMode.OpenOrCreate,
                       FileAccess.ReadWrite, FileShare.None))
            {
                if (!File.Exists(java))
                {
                    DialogResult answer = MessageBox.Show(
                        "首次运行需要从 Eclipse Adoptium 的官方 GitHub 下载约 44 MB 的 Java 17。运行时将校验并保存在当前 Windows 用户目录。是否下载？",
                        Title, MessageBoxButtons.YesNo, MessageBoxIcon.Information);
                    if (answer != DialogResult.Yes)
                        return 0;
                    InstallRuntime(storage);
                }
            }

            Process.Start(new ProcessStartInfo(java, "-cp \"" + Path.Combine(libraries, "*") +
                "\" cn.harryh.vivime.demo.DesktopDemo") { UseShellExecute = false, WorkingDirectory = app });
            return 0;
        }
        catch (Exception error)
        {
            MessageBox.Show("无法启动预览。请检查网络、磁盘空间和解压后的文件。\r\n\r\n" + error.Message,
                Title, MessageBoxButtons.OK, MessageBoxIcon.Error);
            return 1;
        }
    }

    private static void InstallRuntime(string storage)
    {
        string staging = Path.Combine(storage, Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(staging);
        try
        {
            string archive = Path.Combine(staging, "runtime.zip");
            ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;
            using (var downloader = new WebClient())
                downloader.DownloadFile(RuntimeZip, archive);

            using (var file = File.OpenRead(archive))
            using (var digest = SHA256.Create())
            {
                string hash = BitConverter.ToString(digest.ComputeHash(file)).Replace("-", "");
                if (!string.Equals(hash, RuntimeSha256, StringComparison.OrdinalIgnoreCase))
                    throw new InvalidDataException("Java 下载文件的 SHA-256 校验失败。");
            }

            ZipFile.ExtractToDirectory(archive, staging);
            string runtime = Path.Combine(staging, RuntimeDirectory);
            if (!File.Exists(Path.Combine(runtime, "bin", "javaw.exe")))
                throw new InvalidDataException("Java 存档中缺少启动文件。");
            Directory.Move(runtime, Path.Combine(storage, RuntimeDirectory));
        }
        finally
        {
            Directory.Delete(staging, true);
        }
    }
}
