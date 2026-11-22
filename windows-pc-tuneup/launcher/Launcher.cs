// PC TuneUp launcher.
// A tiny Windows program with PCTuneUp.ps1 built in as a resource. When started it copies the
// script to %LOCALAPPDATA%\PCTuneUp\app and runs it with the Windows PowerShell 5.1 that comes
// with Windows. No network access. Reports and undo data are kept in a "PCTuneUp-Data" folder
// next to this .exe, so it stays portable.
using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Windows.Forms;

[assembly: AssemblyTitle("PC TuneUp")]
[assembly: AssemblyProduct("PC TuneUp")]
[assembly: AssemblyDescription("Offline fan & performance doctor for Windows")]
[assembly: AssemblyVersion("1.0.0.0")]
[assembly: AssemblyFileVersion("1.0.0.0")]

static class Program
{
    [STAThread]
    static int Main(string[] args)
    {
        try
        {
            // Folder this .exe lives in (no trailing backslash - it would escape the closing quote).
            string exeDir = AppDomain.CurrentDomain.BaseDirectory.TrimEnd('\\', '/');

            string appDir = Path.Combine(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "PCTuneUp"), "app");
            Directory.CreateDirectory(appDir);
            string script = Path.Combine(appDir, "PCTuneUp.ps1");

            using (Stream src = Assembly.GetExecutingAssembly().GetManifestResourceStream("PCTuneUp.ps1"))
            {
                if (src == null) throw new InvalidOperationException("The built-in script is missing from this .exe.");
                using (FileStream dst = new FileStream(script, FileMode.Create, FileAccess.Write, FileShare.Read))
                {
                    src.CopyTo(dst);
                }
            }

            string powershell = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), @"WindowsPowerShell\v1.0\powershell.exe");
            if (!File.Exists(powershell)) powershell = "powershell.exe";

            string extra = "";
            foreach (string a in args) { if (a.Equals("/limited", StringComparison.OrdinalIgnoreCase) || a.Equals("-limited", StringComparison.OrdinalIgnoreCase)) extra = " -Limited"; }

            ProcessStartInfo psi = new ProcessStartInfo(powershell,
                "-NoProfile -ExecutionPolicy Bypass -STA -WindowStyle Hidden -File \"" + script + "\" -DataRoot \"" + exeDir + "\"" + extra);
            psi.UseShellExecute = false;
            psi.CreateNoWindow = true;
            psi.WorkingDirectory = exeDir;
            Process.Start(psi);
            return 0;
        }
        catch (Exception ex)
        {
            MessageBox.Show("PC TuneUp could not start:\n\n" + ex.Message, "PC TuneUp", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return 1;
        }
    }
}
