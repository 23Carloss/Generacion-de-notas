using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Management;
using System.Net;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace GameMaintenanceLauncher
{
    internal static class Program
    {
        [STAThread]
        private static void Main(string[] args)
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);

            string projectRoot = ResolveProjectRoot(args);
            string selfTestFile = GetArgumentValue(args, "--self-test-file");
            if (HasArgument(args, "--self-test"))
            {
                int result = LauncherController.RunSelfTest(projectRoot, selfTestFile);
                Environment.Exit(result);
                return;
            }

            bool created;
            string mutexName = "Local\\GameMaintenanceLauncher-" + StableHash(projectRoot.ToLowerInvariant());
            using (Mutex mutex = new Mutex(true, mutexName, out created))
            {
                if (!created)
                {
                    MessageBox.Show(
                        "El lanzador de Game Maintenance ya está abierto.",
                        "Game Maintenance",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Information);
                    return;
                }

                using (LauncherForm form = new LauncherForm(projectRoot))
                {
                    Application.Run(form);
                }
            }
        }

        private static string ResolveProjectRoot(string[] args)
        {
            string configured = GetArgumentValue(args, "--project-root");
            if (!String.IsNullOrWhiteSpace(configured))
                return Path.GetFullPath(configured.Trim('"'));
            return Path.GetFullPath(AppDomain.CurrentDomain.BaseDirectory);
        }

        private static bool HasArgument(string[] args, string name)
        {
            foreach (string arg in args)
                if (String.Equals(arg, name, StringComparison.OrdinalIgnoreCase))
                    return true;
            return false;
        }

        private static string GetArgumentValue(string[] args, string name)
        {
            for (int i = 0; i < args.Length; i++)
            {
                if (String.Equals(args[i], name, StringComparison.OrdinalIgnoreCase) && i + 1 < args.Length)
                    return args[i + 1];
                string prefix = name + "=";
                if (args[i].StartsWith(prefix, StringComparison.OrdinalIgnoreCase))
                    return args[i].Substring(prefix.Length);
            }
            return null;
        }

        private static string StableHash(string value)
        {
            unchecked
            {
                uint hash = 2166136261;
                foreach (char c in value)
                {
                    hash ^= c;
                    hash *= 16777619;
                }
                return hash.ToString("x8");
            }
        }
    }

    internal sealed class LauncherForm : Form
    {
        private readonly Label statusLabel;
        private readonly TextBox logBox;
        private readonly Button closeButton;
        private readonly LauncherController controller;
        private int closeStarted;

        public LauncherForm(string projectRoot)
        {
            Text = "Game Maintenance";
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(650, 380);
            MinimumSize = new Size(570, 330);
            Icon = SystemIcons.Application;

            Label title = new Label();
            title.Text = "Game Maintenance";
            title.Font = new Font("Segoe UI", 18F, FontStyle.Bold);
            title.AutoSize = true;
            title.Location = new Point(22, 18);

            statusLabel = new Label();
            statusLabel.Text = "Preparando la aplicación...";
            statusLabel.Font = new Font("Segoe UI", 10F, FontStyle.Regular);
            statusLabel.AutoEllipsis = true;
            statusLabel.Location = new Point(26, 62);
            statusLabel.Size = new Size(595, 24);
            statusLabel.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;

            logBox = new TextBox();
            logBox.Multiline = true;
            logBox.ReadOnly = true;
            logBox.ScrollBars = ScrollBars.Vertical;
            logBox.Font = new Font("Consolas", 9F);
            logBox.Location = new Point(26, 96);
            logBox.Size = new Size(595, 224);
            logBox.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            logBox.BackColor = Color.White;

            closeButton = new Button();
            closeButton.Text = "Cerrar aplicación";
            closeButton.Location = new Point(481, 332);
            closeButton.Size = new Size(140, 30);
            closeButton.Anchor = AnchorStyles.Bottom | AnchorStyles.Right;
            closeButton.Click += delegate { Close(); };

            Controls.Add(title);
            Controls.Add(statusLabel);
            Controls.Add(logBox);
            Controls.Add(closeButton);

            controller = new LauncherController(projectRoot, SetStatus, AppendLog, BrowserClosed);
            Shown += delegate { controller.Start(); };
            FormClosing += OnFormClosing;
        }

        private void SetStatus(string text)
        {
            if (IsDisposed) return;
            if (InvokeRequired)
            {
                BeginInvoke(new Action<string>(SetStatus), text);
                return;
            }
            statusLabel.Text = text;
        }

        private void AppendLog(string text)
        {
            if (IsDisposed) return;
            if (InvokeRequired)
            {
                BeginInvoke(new Action<string>(AppendLog), text);
                return;
            }
            logBox.AppendText(text + Environment.NewLine);
        }

        private void BrowserClosed()
        {
            if (IsDisposed) return;
            if (InvokeRequired)
            {
                BeginInvoke(new Action(BrowserClosed));
                return;
            }
            if (Interlocked.Exchange(ref closeStarted, 1) == 0)
                Close();
        }

        private void OnFormClosing(object sender, FormClosingEventArgs e)
        {
            if (Interlocked.Exchange(ref closeStarted, 1) == 0)
            {
                closeButton.Enabled = false;
                controller.Stop();
            }
            else
            {
                controller.Stop();
            }
        }
    }

    internal sealed class LauncherController : IDisposable
    {
        private const int BackendPort = 8080;
        private const int FrontendPort = 5173;
        // Debe coincidir exactamente con APP_ALLOWED_ORIGINS del backend.
        // localhost y 127.0.0.1 son orígenes distintos para CORS.
        private const string ApplicationUrl = "http://localhost:5173";

        private readonly string projectRoot;
        private readonly string backendRoot;
        private readonly string frontendRoot;
        private readonly Action<string> setStatus;
        private readonly Action<string> appendUiLog;
        private readonly Action browserClosed;
        private readonly object sync = new object();
        private readonly object logSync = new object();
        private readonly List<OwnedProcess> ownedProcesses = new List<OwnedProcess>();
        private readonly JobObject ownedJob = new JobObject();
        private volatile bool stopRequested;
        private int cleanupStarted;
        private StreamWriter logWriter;
        private string statePath;
        private string browserProfile;

        public LauncherController(
            string root,
            Action<string> statusCallback,
            Action<string> logCallback,
            Action browserClosedCallback)
        {
            projectRoot = Path.GetFullPath(root);
            backendRoot = Path.Combine(projectRoot, "Game_Maintenance");
            frontendRoot = Path.Combine(projectRoot, "game-maintenance-frontend");
            setStatus = statusCallback;
            appendUiLog = logCallback;
            browserClosed = browserClosedCallback;
        }

        public void Start()
        {
            Thread worker = new Thread(Run);
            worker.Name = "GameMaintenanceLauncherWorker";
            worker.IsBackground = true;
            worker.Start();
        }

        private void Run()
        {
            try
            {
                InitializeLogging();
                Log("Lanzador iniciado. Raíz: " + projectRoot);
                ValidateProject();

                bool backendReady = ProbeBackend();
                bool frontendReady = ProbeFrontend();

                if (!backendReady && IsPortOpen(BackendPort))
                    throw new InvalidOperationException("El puerto 8080 está ocupado por otro servicio que no parece ser Game Maintenance.");
                if (!frontendReady && IsPortOpen(FrontendPort))
                    throw new InvalidOperationException("El puerto 5173 está ocupado por otro servicio que no parece ser el frontend de Game Maintenance.");

                if (backendReady)
                    Log("Backend preexistente detectado en el puerto 8080; no será cerrado por este lanzador.");
                else
                    StartBackend();

                if (frontendReady)
                    Log("Frontend preexistente detectado en el puerto 5173; no será cerrado por este lanzador.");
                else
                    StartFrontend();

                WaitForApplications();
                if (stopRequested) return;

                setStatus("Aplicación lista. Al cerrar el navegador se detendrán los procesos iniciados aquí.");
                Process browser = StartBrowser();
                Log("Navegador dedicado iniciado (PID " + browser.Id + ").");
                WriteState();

                while (!stopRequested && BrowserWindowIsRunning(browserProfile))
                    Thread.Sleep(750);

                if (!stopRequested)
                {
                    Log("Se cerró la ventana del navegador. Iniciando cierre controlado.");
                    setStatus("Cerrando los servicios iniciados por el lanzador...");
                    Stop();
                    browserClosed();
                }
            }
            catch (Exception ex)
            {
                Log("ERROR: " + ex.Message);
                setStatus("No se pudo iniciar: " + ex.Message);
                MessageBox.Show(
                    ex.Message + Environment.NewLine + Environment.NewLine + "Consulta el registro para más detalles.",
                    "No se pudo iniciar Game Maintenance",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error);
                Stop();
            }
        }

        private void ValidateProject()
        {
            if (!Directory.Exists(backendRoot))
                throw new DirectoryNotFoundException("No se encontró el backend en " + backendRoot);
            if (!Directory.Exists(frontendRoot))
                throw new DirectoryNotFoundException("No se encontró el frontend en " + frontendRoot);

            string mainClass = Path.Combine(backendRoot, "Persistencia", "target", "classes", "ServerMain", "ServerMain.class");
            string dependencyDirectory = Path.Combine(backendRoot, "Persistencia", "target", "dependency");
            string viteScript = Path.Combine(frontendRoot, "node_modules", "vite", "bin", "vite.js");
            if (!File.Exists(mainClass) || !Directory.Exists(dependencyDirectory))
                throw new FileNotFoundException("El backend no está compilado. Ejecuta launcher\\build-launcher.ps1 para compilarlo.");
            if (!File.Exists(viteScript))
                throw new FileNotFoundException("Faltan las dependencias del frontend. Ejecuta npm install en game-maintenance-frontend.");
            FindJava25();
            FindNode();
            FindBrowser();
        }

        private void StartBackend()
        {
            setStatus("Iniciando backend...");
            string java = FindJava25();
            string classes = Path.Combine(backendRoot, "Persistencia", "target", "classes");
            string models = Path.Combine(backendRoot, "Models", "target", "classes");
            string business = Path.Combine(backendRoot, "Negocio", "target", "classes");
            string dependencies = Path.Combine(backendRoot, "Persistencia", "target", "dependency", "*");
            string classpath = classes + ";" + models + ";" + business + ";" + dependencies;

            ProcessStartInfo info = ServiceStartInfo(java, "-cp " + Quote(classpath) + " ServerMain.ServerMain", backendRoot);
            StartOwnedProcess("backend", info);
        }

        private void StartFrontend()
        {
            setStatus("Iniciando frontend...");
            string node = FindNode();
            string viteScript = Path.Combine(frontendRoot, "node_modules", "vite", "bin", "vite.js");
            string arguments = Quote(viteScript) + " --host 127.0.0.1 --port 5173 --strictPort";
            ProcessStartInfo info = ServiceStartInfo(node, arguments, frontendRoot);
            StartOwnedProcess("frontend", info);
        }

        private ProcessStartInfo ServiceStartInfo(string fileName, string arguments, string workingDirectory)
        {
            ProcessStartInfo info = new ProcessStartInfo();
            info.FileName = fileName;
            info.Arguments = arguments;
            info.WorkingDirectory = workingDirectory;
            info.UseShellExecute = false;
            info.CreateNoWindow = true;
            info.RedirectStandardOutput = true;
            info.RedirectStandardError = true;
            info.StandardOutputEncoding = Encoding.UTF8;
            info.StandardErrorEncoding = Encoding.UTF8;
            return info;
        }

        private Process StartOwnedProcess(string role, ProcessStartInfo info)
        {
            Process process = new Process();
            process.StartInfo = info;
            process.EnableRaisingEvents = true;
            process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs e)
            {
                if (e.Data != null) Log("[" + role + "] " + e.Data);
            };
            process.ErrorDataReceived += delegate(object sender, DataReceivedEventArgs e)
            {
                if (e.Data != null) Log("[" + role + "] " + e.Data);
            };

            if (!process.Start())
                throw new InvalidOperationException("No fue posible iniciar " + role + ".");

            try
            {
                ownedJob.Add(process);
            }
            catch
            {
                try { process.Kill(); } catch { }
                throw;
            }

            process.BeginOutputReadLine();
            process.BeginErrorReadLine();
            lock (sync)
                ownedProcesses.Add(new OwnedProcess(role, process));
            Log(Char.ToUpperInvariant(role[0]) + role.Substring(1) + " iniciado por el lanzador (PID " + process.Id + ").");
            WriteState();
            return process;
        }

        private void WaitForApplications()
        {
            setStatus("Esperando a que backend y frontend estén listos...");
            DateTime deadline = DateTime.UtcNow.AddSeconds(120);
            bool backendReady = false;
            bool frontendReady = false;

            while (!stopRequested && DateTime.UtcNow < deadline)
            {
                backendReady = ProbeBackend();
                frontendReady = ProbeFrontend();
                if (backendReady && frontendReady)
                {
                    Log("Backend y frontend están listos.");
                    return;
                }

                ThrowIfOwnedProcessExited("backend", backendReady);
                ThrowIfOwnedProcessExited("frontend", frontendReady);
                Thread.Sleep(1000);
            }

            if (stopRequested) return;
            string missing = !backendReady && !frontendReady ? "backend y frontend" : (!backendReady ? "backend" : "frontend");
            throw new TimeoutException("Tiempo agotado esperando " + missing + ". Revisa el registro del lanzador.");
        }

        private void ThrowIfOwnedProcessExited(string role, bool ready)
        {
            if (ready) return;
            lock (sync)
            {
                foreach (OwnedProcess owned in ownedProcesses)
                {
                    if (owned.Role == role && owned.Process.HasExited)
                        throw new InvalidOperationException("El " + role + " terminó antes de quedar listo (código " + owned.Process.ExitCode + ").");
                }
            }
        }

        private Process StartBrowser()
        {
            setStatus("Abriendo la aplicación en el navegador...");
            string browser = FindBrowser();
            string localData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            string profilesRoot = Path.Combine(localData, "GameMaintenanceLauncher", "browser-runs");
            Directory.CreateDirectory(profilesRoot);
            browserProfile = Path.Combine(profilesRoot, DateTime.Now.ToString("yyyyMMdd-HHmmss") + "-" + Guid.NewGuid().ToString("N"));
            Directory.CreateDirectory(browserProfile);

            ProcessStartInfo info = new ProcessStartInfo();
            info.FileName = browser;
            info.Arguments = "--app=" + ApplicationUrl +
                             " --user-data-dir=" + Quote(browserProfile) +
                             " --no-first-run --no-default-browser-check --disable-background-mode --disable-extensions";
            info.UseShellExecute = false;
            info.WorkingDirectory = projectRoot;

            Process process = Process.Start(info);
            if (process == null)
                throw new InvalidOperationException("No se pudo abrir el navegador.");
            try
            {
                ownedJob.Add(process);
            }
            catch
            {
                try { process.Kill(); } catch { }
                throw;
            }

            lock (sync)
                ownedProcesses.Add(new OwnedProcess("navegador", process));

            Thread.Sleep(1500);
            Process root = FindBrowserRoot(browserProfile);
            return root ?? process;
        }

        private static bool BrowserWindowIsRunning(string profile)
        {
            return FindBrowserRoot(profile) != null;
        }

        private static Process FindBrowserRoot(string profile)
        {
            if (String.IsNullOrEmpty(profile)) return null;
            string normalized = profile.TrimEnd('\\');
            try
            {
                using (ManagementObjectSearcher searcher = new ManagementObjectSearcher(
                    "SELECT ProcessId, CommandLine FROM Win32_Process WHERE Name='chrome.exe' OR Name='opera.exe' OR Name='launcher.exe'"))
                using (ManagementObjectCollection results = searcher.Get())
                {
                    foreach (ManagementObject item in results)
                    {
                        string commandLine = Convert.ToString(item["CommandLine"]);
                        if (commandLine.IndexOf(normalized, StringComparison.OrdinalIgnoreCase) >= 0 &&
                            commandLine.IndexOf("--type=", StringComparison.OrdinalIgnoreCase) < 0)
                        {
                            int pid = Convert.ToInt32((UInt32)item["ProcessId"]);
                            try { return Process.GetProcessById(pid); } catch { }
                        }
                    }
                }
            }
            catch { }
            return null;
        }

        private bool ProbeBackend()
        {
            HttpWebResponse response = null;
            try
            {
                HttpWebRequest request = (HttpWebRequest)WebRequest.Create(
                    "http://127.0.0.1:8080/GameMaintenance/api/auth/launcher-probe");
                request.Method = "GET";
                request.Timeout = 1200;
                request.ReadWriteTimeout = 1200;
                request.AllowAutoRedirect = false;
                try
                {
                    response = (HttpWebResponse)request.GetResponse();
                }
                catch (WebException ex)
                {
                    response = ex.Response as HttpWebResponse;
                }
                if (response == null) return false;
                string server = response.Headers[HttpResponseHeader.Server] ?? "";
                string security = response.Headers["X-Content-Type-Options"] ?? "";
                return server.IndexOf("Jetty", StringComparison.OrdinalIgnoreCase) >= 0 &&
                       String.Equals(security, "nosniff", StringComparison.OrdinalIgnoreCase);
            }
            catch { return false; }
            finally { if (response != null) response.Close(); }
        }

        private bool ProbeFrontend()
        {
            HttpWebResponse response = null;
            try
            {
                HttpWebRequest request = (HttpWebRequest)WebRequest.Create(ApplicationUrl + "/");
                request.Method = "GET";
                request.Timeout = 1200;
                request.ReadWriteTimeout = 1200;
                request.AllowAutoRedirect = false;
                response = (HttpWebResponse)request.GetResponse();
                using (StreamReader reader = new StreamReader(response.GetResponseStream()))
                {
                    string body = reader.ReadToEnd();
                    return body.IndexOf("Game Maintenance", StringComparison.OrdinalIgnoreCase) >= 0 &&
                           body.IndexOf("Panel de Servicio", StringComparison.OrdinalIgnoreCase) >= 0;
                }
            }
            catch { return false; }
            finally { if (response != null) response.Close(); }
        }

        private static bool IsPortOpen(int port)
        {
            TcpClient client = new TcpClient();
            try
            {
                IAsyncResult result = client.BeginConnect(IPAddress.Loopback, port, null, null);
                bool connected = result.AsyncWaitHandle.WaitOne(500);
                if (!connected) return false;
                client.EndConnect(result);
                return client.Connected;
            }
            catch { return false; }
            finally { client.Close(); }
        }

        private string FindJava25()
        {
            List<string> candidates = new List<string>();
            string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!String.IsNullOrWhiteSpace(javaHome))
                candidates.Add(Path.Combine(javaHome, "bin", "java.exe"));

            AddJavaCandidates(candidates, Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "Java"));
            AddJavaCandidates(candidates, Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "Eclipse Adoptium"));
            AddPathCandidates(candidates, "java.exe");

            HashSet<string> seen = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            foreach (string candidate in candidates)
            {
                if (!seen.Add(candidate) || !File.Exists(candidate)) continue;
                if (JavaMajorVersion(candidate) >= 25) return candidate;
            }
            throw new FileNotFoundException("Se requiere JDK 25 para ejecutar el backend y no se encontró una instalación compatible.");
        }

        private static void AddJavaCandidates(List<string> candidates, string directory)
        {
            try
            {
                if (!Directory.Exists(directory)) return;
                foreach (string child in Directory.GetDirectories(directory, "*", SearchOption.TopDirectoryOnly))
                    candidates.Add(Path.Combine(child, "bin", "java.exe"));
            }
            catch { }
        }

        private static int JavaMajorVersion(string java)
        {
            try
            {
                ProcessStartInfo info = new ProcessStartInfo(java, "-version");
                info.UseShellExecute = false;
                info.CreateNoWindow = true;
                info.RedirectStandardError = true;
                Process process = Process.Start(info);
                string output = process.StandardError.ReadToEnd();
                process.WaitForExit(3000);
                int firstQuote = output.IndexOf('"');
                int secondQuote = firstQuote < 0 ? -1 : output.IndexOf('"', firstQuote + 1);
                if (firstQuote >= 0 && secondQuote > firstQuote)
                {
                    string version = output.Substring(firstQuote + 1, secondQuote - firstQuote - 1);
                    string first = version.Split('.')[0];
                    int major;
                    if (Int32.TryParse(first, out major)) return major;
                }
            }
            catch { }
            return 0;
        }

        private string FindNode()
        {
            List<string> candidates = new List<string>();
            candidates.Add(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "nodejs", "node.exe"));
            candidates.Add(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86), "nodejs", "node.exe"));
            AddPathCandidates(candidates, "node.exe");
            foreach (string candidate in candidates)
                if (File.Exists(candidate)) return candidate;
            throw new FileNotFoundException("Node.js no está instalado o no está disponible en PATH.");
        }

        private string FindBrowser()
        {
            string programFiles = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
            string programFilesX86 = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86);
            string local = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            // Chrome es la primera opción. Opera GX se usa como alternativa.
            // Edge se omite deliberadamente por preferencia del usuario.
            string[] candidates = new string[]
            {
                Path.Combine(programFiles, "Google", "Chrome", "Application", "chrome.exe"),
                Path.Combine(programFilesX86, "Google", "Chrome", "Application", "chrome.exe"),
                Path.Combine(local, "Google", "Chrome", "Application", "chrome.exe"),
                Path.Combine(local, "Programs", "Opera GX", "opera.exe"),
                Path.Combine(local, "Programs", "Opera GX", "launcher.exe")
            };
            foreach (string candidate in candidates)
                if (File.Exists(candidate)) return candidate;
            throw new FileNotFoundException("Se necesita Google Chrome u Opera GX para abrir una ventana rastreable.");
        }

        private static void AddPathCandidates(List<string> candidates, string executable)
        {
            string path = Environment.GetEnvironmentVariable("PATH") ?? "";
            foreach (string part in path.Split(Path.PathSeparator))
            {
                string clean = part.Trim().Trim('"');
                if (!String.IsNullOrEmpty(clean)) candidates.Add(Path.Combine(clean, executable));
            }
        }

        private void InitializeLogging()
        {
            string local = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            string baseDirectory = Path.Combine(local, "GameMaintenanceLauncher");
            string logDirectory = Path.Combine(baseDirectory, "logs");
            Directory.CreateDirectory(logDirectory);
            string logPath = Path.Combine(logDirectory, "launcher-" + DateTime.Now.ToString("yyyyMMdd-HHmmss") + ".log");
            statePath = Path.Combine(baseDirectory, "last-run.txt");
            logWriter = new StreamWriter(logPath, true, new UTF8Encoding(false));
            logWriter.AutoFlush = true;
            Log("Registro: " + logPath);
        }

        private void Log(string message)
        {
            string line = DateTime.Now.ToString("HH:mm:ss") + "  " + message;
            try
            {
                lock (logSync)
                {
                    if (logWriter != null)
                    {
                        logWriter.WriteLine(line);
                        logWriter.Flush();
                    }
                }
            }
            catch { }
            try { appendUiLog(line); } catch { }
        }

        private void WriteState()
        {
            if (String.IsNullOrEmpty(statePath)) return;
            try
            {
                StringBuilder state = new StringBuilder();
                state.AppendLine("started=" + DateTime.Now.ToString("o"));
                state.AppendLine("launcherPid=" + Process.GetCurrentProcess().Id);
                lock (sync)
                {
                    foreach (OwnedProcess owned in ownedProcesses)
                    {
                        state.AppendLine(owned.Role + "Pid=" + owned.Process.Id);
                        state.AppendLine(owned.Role + "Owned=true");
                    }
                }
                if (!String.IsNullOrEmpty(browserProfile))
                    state.AppendLine("browserProfile=" + browserProfile);
                File.WriteAllText(statePath, state.ToString(), new UTF8Encoding(false));
            }
            catch { }
        }

        public void Stop()
        {
            stopRequested = true;
            if (Interlocked.Exchange(ref cleanupStarted, 1) != 0) return;

            setStatus("Cerrando los procesos iniciados por este lanzador...");
            List<OwnedProcess> snapshot;
            lock (sync)
                snapshot = new List<OwnedProcess>(ownedProcesses);

            for (int i = snapshot.Count - 1; i >= 0; i--)
            {
                OwnedProcess owned = snapshot[i];
                try
                {
                    if (!owned.Process.HasExited)
                    {
                        Log("Deteniendo " + owned.Role + " (PID " + owned.Process.Id + ").");
                        owned.Process.Kill();
                        owned.Process.WaitForExit(3000);
                    }
                }
                catch (Exception ex)
                {
                    Log("No se pudo detener " + owned.Role + " directamente: " + ex.Message);
                }
            }

            ownedJob.Dispose();
            TryDeleteBrowserProfile();
            Log("Cierre finalizado. Los procesos preexistentes no fueron modificados.");
            try
            {
                lock (logSync)
                {
                    if (logWriter != null)
                    {
                        logWriter.Dispose();
                        logWriter = null;
                    }
                }
            }
            catch { }
        }

        private void TryDeleteBrowserProfile()
        {
            if (String.IsNullOrEmpty(browserProfile) || !Directory.Exists(browserProfile)) return;
            for (int attempt = 0; attempt < 4; attempt++)
            {
                try
                {
                    Directory.Delete(browserProfile, true);
                    return;
                }
                catch { Thread.Sleep(250); }
            }
        }

        public void Dispose()
        {
            Stop();
        }

        public static int RunSelfTest(string projectRoot, string outputFile)
        {
            List<string> results = new List<string>();
            int failures = 0;
            Action<bool, string> check = delegate(bool condition, string message)
            {
                results.Add((condition ? "OK   " : "FAIL ") + message);
                if (!condition) failures++;
            };

            string root = Path.GetFullPath(projectRoot);
            string backend = Path.Combine(root, "Game_Maintenance");
            string frontend = Path.Combine(root, "game-maintenance-frontend");
            check(Directory.Exists(backend), "Carpeta del backend");
            check(Directory.Exists(frontend), "Carpeta del frontend");
            check(File.Exists(Path.Combine(backend, "Persistencia", "target", "classes", "ServerMain", "ServerMain.class")), "Backend compilado");
            check(Directory.Exists(Path.Combine(backend, "Persistencia", "target", "dependency")), "Dependencias del backend");
            check(File.Exists(Path.Combine(frontend, "node_modules", "vite", "bin", "vite.js")), "Dependencias de Vite");

            LauncherController controller = new LauncherController(root, delegate { }, delegate { }, delegate { });
            try { controller.FindJava25(); check(true, "JDK 25"); } catch (Exception ex) { check(false, "JDK 25: " + ex.Message); }
            try { controller.FindNode(); check(true, "Node.js"); } catch (Exception ex) { check(false, "Node.js: " + ex.Message); }
            try { controller.FindBrowser(); check(true, "Google Chrome u Opera GX"); } catch (Exception ex) { check(false, "Navegador: " + ex.Message); }
            controller.ownedJob.Dispose();

            results.Add(failures == 0 ? "RESULTADO: LISTO" : "RESULTADO: " + failures + " ERROR(ES)");
            string report = String.Join(Environment.NewLine, results.ToArray());
            if (!String.IsNullOrWhiteSpace(outputFile))
                File.WriteAllText(outputFile, report, new UTF8Encoding(false));
            else
                MessageBox.Show(report, "Diagnóstico del lanzador", MessageBoxButtons.OK,
                    failures == 0 ? MessageBoxIcon.Information : MessageBoxIcon.Error);
            return failures == 0 ? 0 : 1;
        }

        private static string Quote(string value)
        {
            return "\"" + value.Replace("\"", "\\\"") + "\"";
        }
    }

    internal sealed class OwnedProcess
    {
        public readonly string Role;
        public readonly Process Process;

        public OwnedProcess(string role, Process process)
        {
            Role = role;
            Process = process;
        }
    }

    internal sealed class JobObject : IDisposable
    {
        private IntPtr handle;

        public JobObject()
        {
            handle = NativeMethods.CreateJobObject(IntPtr.Zero, null);
            if (handle == IntPtr.Zero)
                throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error(), "No se pudo crear el grupo de procesos.");

            NativeMethods.JOBOBJECT_EXTENDED_LIMIT_INFORMATION info = new NativeMethods.JOBOBJECT_EXTENDED_LIMIT_INFORMATION();
            info.BasicLimitInformation.LimitFlags = NativeMethods.JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
            int length = Marshal.SizeOf(typeof(NativeMethods.JOBOBJECT_EXTENDED_LIMIT_INFORMATION));
            IntPtr pointer = Marshal.AllocHGlobal(length);
            try
            {
                Marshal.StructureToPtr(info, pointer, false);
                if (!NativeMethods.SetInformationJobObject(handle, 9, pointer, (UInt32)length))
                    throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error(), "No se pudo configurar el grupo de procesos.");
            }
            finally
            {
                Marshal.FreeHGlobal(pointer);
            }
        }

        public void Add(Process process)
        {
            if (handle == IntPtr.Zero)
                throw new ObjectDisposedException("JobObject");
            if (!NativeMethods.AssignProcessToJobObject(handle, process.Handle))
                throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error(), "No se pudo proteger el proceso iniciado.");
        }

        public void Dispose()
        {
            IntPtr current = Interlocked.Exchange(ref handle, IntPtr.Zero);
            if (current != IntPtr.Zero)
                NativeMethods.CloseHandle(current);
        }
    }

    internal static class NativeMethods
    {
        internal const UInt32 JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x00002000;

        [StructLayout(LayoutKind.Sequential)]
        internal struct JOBOBJECT_BASIC_LIMIT_INFORMATION
        {
            public Int64 PerProcessUserTimeLimit;
            public Int64 PerJobUserTimeLimit;
            public UInt32 LimitFlags;
            public UIntPtr MinimumWorkingSetSize;
            public UIntPtr MaximumWorkingSetSize;
            public UInt32 ActiveProcessLimit;
            public UIntPtr Affinity;
            public UInt32 PriorityClass;
            public UInt32 SchedulingClass;
        }

        [StructLayout(LayoutKind.Sequential)]
        internal struct IO_COUNTERS
        {
            public UInt64 ReadOperationCount;
            public UInt64 WriteOperationCount;
            public UInt64 OtherOperationCount;
            public UInt64 ReadTransferCount;
            public UInt64 WriteTransferCount;
            public UInt64 OtherTransferCount;
        }

        [StructLayout(LayoutKind.Sequential)]
        internal struct JOBOBJECT_EXTENDED_LIMIT_INFORMATION
        {
            public JOBOBJECT_BASIC_LIMIT_INFORMATION BasicLimitInformation;
            public IO_COUNTERS IoInfo;
            public UIntPtr ProcessMemoryLimit;
            public UIntPtr JobMemoryLimit;
            public UIntPtr PeakProcessMemoryUsed;
            public UIntPtr PeakJobMemoryUsed;
        }

        [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
        internal static extern IntPtr CreateJobObject(IntPtr securityAttributes, string name);

        [DllImport("kernel32.dll", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        internal static extern bool SetInformationJobObject(IntPtr job, int informationClass, IntPtr information, UInt32 length);

        [DllImport("kernel32.dll", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        internal static extern bool AssignProcessToJobObject(IntPtr job, IntPtr process);

        [DllImport("kernel32.dll", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        internal static extern bool CloseHandle(IntPtr handle);
    }
}
