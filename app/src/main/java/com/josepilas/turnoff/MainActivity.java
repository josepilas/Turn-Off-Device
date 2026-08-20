package com.josepilas.turnoff;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Turn Off Device
 *
 * A single-Activity, framework-only root utility. When the user taps the
 * launcher icon (on a phone, tablet, or Android TV), the Activity starts
 * invisibly, verifies functional root access, and (if root is available)
 * performs a device power-off using a chain of safe fallback commands
 * executed through {@code su}.
 *
 * If root is not available / not granted, the only visible UI is shown:
 * a warning dialog explaining that root is required.
 *
 * Security notes:
 *  - No user input is ever concatenated into shell commands. Every command
 *    is a fixed, compile-time constant.
 *  - The app uses no internet, stores no data, declares no permissions,
 *    and runs no background services.
 */
public class MainActivity extends Activity {

    /**
     * Ordered list of safe power-off commands. Each is executed as
     * {@code su -c "COMMAND"}. The next entry is only attempted when the
     * previous one throws, cannot start, returns a non-zero exit code, or
     * times out. {@code reboot} without a power-off argument is intentionally
     * absent so the device is never rebooted instead of powered off.
     */
    private static final String[] SHUTDOWN_COMMANDS = new String[]{
            "svc power shutdown",     // AOSP framework service (most ROMs)
            "reboot -p",              // poweroff via reboot (Linux/Android)
            "reboot --poweroff",      // long-form poweroff flag
            "poweroff",               // classic Linux poweroff
            "halt -p",                // halt and power off
            "shutdown -h now",        // BSD/shutdown style
            "busybox poweroff",       // busybox userspace
            "busybox halt -p",        // busybox halt -p
            "toybox reboot -p",       // toybox (Android 6+)
            "toolbox reboot -p"       // legacy Android toolbox
    };

    /** Timeout for the {@code su -c id} root verification, in milliseconds. */
    private static final int ROOT_CHECK_TIMEOUT_MS = 8000;

    /** Timeout for each shutdown command, in milliseconds. */
    private static final int SHUTDOWN_TIMEOUT_MS = 5000;

    /** Guard so the flow only runs once even if onCreate is called again. */
    private volatile boolean started = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // No setContentView(): there is intentionally no layout.
        // Disable all default activity transition animations.
        overridePendingTransition(0, 0);

        if (started) {
            return;
        }
        started = true;

        // Run root detection + shutdown off the UI thread.
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                performShutdownFlow();
            }
        }, "RootShutdownWorker");
        worker.setDaemon(true);
        worker.start();
    }

    /** Top-level flow: verify root, then either shut down or show the dialog. */
    private void performShutdownFlow() {
        final boolean rootOk = hasFunctionalRoot();
        if (rootOk) {
            attemptShutdown();
            // If we get here, a shutdown was issued (or every fallback was
            // attempted). Finish quietly with no UI.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    silentFinish();
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showRootRequiredDialog();
                }
            });
        }
    }

    /**
     * Functional root check.
     *
     * Step 1: fast pre-check that the {@code su} binary exists somewhere on
     * the device (PATH or well-known root locations). If it does not exist,
     * root is certainly unavailable.
     *
     * Step 2: execute {@code su -c id} and confirm the output indicates
     * UID 0 (root). This proves that root is not only present but actually
     * granted to this app (e.g. the user accepted the Magisk prompt).
     */
    private boolean hasFunctionalRoot() {
        if (!suBinaryExists()) {
            return false;
        }
        try {
            ProcessResult r = runRootCommand("id", ROOT_CHECK_TIMEOUT_MS);
            if (r == null) {
                return false;
            }
            if (r.exitCode != 0) {
                return false;
            }
            String out = ((r.stdout == null ? "" : r.stdout))
                    + (r.stderr == null ? "" : r.stderr);
            // Look for "uid=0" (and optionally "(root)").
            return out.contains("uid=0");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks for the {@code su} executable in the system PATH and in the
     * common root-install locations used by Magisk, SuperSU, etc.
     */
    private boolean suBinaryExists() {
        // Well-known absolute locations.
        String[] known = new String[]{
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/system/sbin/su",
                "/vendor/bin/su",
                "/su/bin/su",
                "/magisk/.core/bin/su",
                "/debug_ramdisk/su"
        };
        for (String p : known) {
            if (new File(p).exists()) {
                return true;
            }
        }
        // PATH-based lookup (mirrors `which su`).
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] dirs = pathEnv.split(":");
            for (String dir : dirs) {
                if (dir.length() == 0) {
                    continue;
                }
                File f = new File(dir, "su");
                if (f.exists()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Runs the ordered shutdown commands through {@code su -c}. Stops at the
     * first command that returns exit code 0 (device should be powering off).
     * Missing/incompatible commands simply fail and the next fallback runs.
     */
    private void attemptShutdown() {
        for (String command : SHUTDOWN_COMMANDS) {
            ProcessResult r = null;
            try {
                r = runRootCommand(command, SHUTDOWN_TIMEOUT_MS);
            } catch (Exception e) {
                r = null;
            }
            if (r != null && r.exitCode == 0) {
                // Command accepted by the system; shutdown in progress.
                return;
            }
            // Otherwise: exception, non-zero exit, or timeout -> try next.
        }
    }

    /**
     * Executes {@code su -c "<command>"} with a hard timeout, draining both
     * stdout and stderr so the child cannot block on a full pipe buffer.
     *
     * The command argument is passed as a single argv element to the
     * {@code su} process (via {@link ProcessBuilder}), which is exactly
     * equivalent to the shell form {@code su -c "COMMAND"} but avoids any
     * shell parsing/injection because no shell is involved on our side.
     *
     * @return a {@link ProcessResult}, or {@code null} if the process could
     *         not be started at all.
     */
    private ProcessResult runRootCommand(String command, int timeoutMs) {
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("su", "-c", command);
            pb.redirectErrorStream(false);
            process = pb.start();
            final Process p = process;

            // Drain stdout and stderr concurrently.
            AtomicReference<String> outRef = new AtomicReference<>("");
            AtomicReference<String> errRef = new AtomicReference<>("");
            Thread outReader = drainStream(p.getInputStream(), outRef);
            Thread errReader = drainStream(p.getErrorStream(), errRef);

            // Watchdog: enforce a timeout. Uses destroy() (available on all
            // API levels) rather than destroyForcibly()/isAlive() (API 26+).
            final boolean[] timedOut = {false};
            final boolean[] done = {false};
            Thread watchdog = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(timeoutMs);
                    } catch (InterruptedException e) {
                        return; // process finished first
                    }
                    if (!done[0]) {
                        timedOut[0] = true;
                        try {
                            p.destroy();
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }, "SuWatchdog");
            watchdog.setDaemon(true);
            watchdog.start();

            int exitCode;
            try {
                exitCode = p.waitFor();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                exitCode = -1;
            }
            done[0] = true;
            watchdog.interrupt();

            // Best-effort: also close the child's stdin (unused here, but
            // keeps file descriptors tidy).
            try {
                OutputStream os = p.getOutputStream();
                if (os != null) {
                    os.close();
                }
            } catch (IOException ignored) {
            }

            try {
                outReader.join(1500);
            } catch (InterruptedException ignored) {
            }
            try {
                errReader.join(1500);
            } catch (InterruptedException ignored) {
            }

            ProcessResult result = new ProcessResult();
            result.exitCode = timedOut[0] ? -1 : exitCode;
            result.stdout = outRef.get();
            result.stderr = errRef.get();
            return result;
        } catch (IOException e) {
            // su binary missing or cannot be spawned.
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /** Reads a stream fully into the given reference holder. */
    private Thread drainStream(InputStream is, final AtomicReference<String> ref) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                } catch (IOException ignored) {
                    // Stream closed / process died; whatever was read is enough.
                } finally {
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
                ref.set(sb.toString());
            }
        }, "SuStreamDrain");
        t.setDaemon(true);
        t.start();
        return t;
    }

    /** The only visible UI: shown exclusively when root is unavailable. */
    private void showRootRequiredDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.root_required_title);
        builder.setMessage(R.string.root_required_message);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                silentFinish();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                silentFinish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    /** Finishes the activity with no transition animation. */
    private void silentFinish() {
        try {
            finishAffinity();
        } catch (Throwable ignored) {
            finish();
        }
        overridePendingTransition(0, 0);
    }

    /** Simple holder for a process result. */
    private static final class ProcessResult {
        int exitCode;
        String stdout;
        String stderr;
    }
}
