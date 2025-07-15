package org.strassburger.lifestealz.commands.MainCommand.subcommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.strassburger.lifestealz.LifeStealZ;
import org.strassburger.lifestealz.commands.SubCommand;
import org.strassburger.lifestealz.util.MessageUtils;
import org.strassburger.lifestealz.util.SchedulerUtils;
import org.strassburger.lifestealz.util.commands.CommandUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DebugSubCommand implements SubCommand {
    private final LifeStealZ plugin;

    public DebugSubCommand(LifeStealZ plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!hasPermission(sender)) {
            CommandUtils.throwPermissionError(sender);
            return false;
        }

        sender.sendMessage(MessageUtils.getAndFormatMsg(
                false,
                "generatingDebugReport",
                "&7Generating debug report..."
        ));

        // Run asynchronously
        SchedulerUtils.runTaskAsynchronously(plugin, () -> {
            try {
                String debugDump = generateDebugDump();
                String pasteUrl = uploadToMclogs(debugDump);

                if (pasteUrl != null) {
                    // Clean up any escaped slashes
                    pasteUrl = pasteUrl.replace("\\/", "/");

                    // Create a formatted message with a clickable link
                    Component message = MessageUtils.getAndFormatMsg(
                                    false,
                                    "debugReportUploaded",
                                    "&aDebug report uploaded: "
                            )
                            .append(
                                    MessageUtils.formatMsg("&7" + pasteUrl)
                                            .clickEvent(ClickEvent.openUrl(pasteUrl))
                                            .hoverEvent(HoverEvent.showText(MessageUtils.getAndFormatMsg(
                                                    false,
                                                    "clickToOpenDebugReport",
                                                    "&eClick to open the debug report"
                                            )))
                            );

                    sender.sendMessage(message);
                } else {
                    sender.sendMessage(MessageUtils.getAndFormatMsg(
                            false,
                            "failedToUploadDebugReport",
                            "&cFailed to upload debug report. Please try again later."
                    ));
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error generating debug report", e);
                sender.sendMessage(MessageUtils.getAndFormatMsg(
                        false,
                        "errorWhileGeneratingDebugReport",
                        "&cAn error occurred while generating the debug report."
                ));
            }
        });
        return true;
    }

    @Override
    public String getUsage() {
        return "/lifestealz debug";
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.isOp() || sender.equals(Bukkit.getConsoleSender());
    }

    /**
     * Generates the debug dump for the LifeStealZ plugin.
     *
     * @return The formatted debug dump string.
     */
    private String generateDebugDump() {
        StringBuilder debug = new StringBuilder();
        long epochTime = System.currentTimeMillis();
        String formattedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(epochTime));

        // Basic plugin info
        String pluginVersion = plugin.getDescription().getVersion();
        String pluginHash = generatePluginHash();
        String serverVersion = Bukkit.getVersion();
        String serverSoftware = Bukkit.getName();
        String javaVersion = System.getProperty("java.version");
        String osInfo = System.getProperty("os.name") + " " + System.getProperty("os.version");

        // Header
        debug.append("---- LifeStealZ Debug Dump ----\n")
                .append("// This is an automatically generated debug report for the LifeStealZ Plugin.\n")
                .append("// This report DOES NOT include any Personally Identifiable Information.\n")
                .append("// This debug report is intended to be provided to a support agent.\n\n")
                .append("// For More Information visit: https://modrinth.com/plugin/lifestealz\n\n")
                .append("Time: ").append(formattedTime).append(" (Epoch: ").append(epochTime).append(")\n\n");

        // Plugin info
        debug.append("-- Plugin Details --\n")
                .append("Plugin Version: ").append(pluginVersion).append("\n")
                .append("Plugin Hash: ").append(pluginHash).append("\n\n")
                .append("Minecraft Version: ").append(serverVersion).append("\n")
                .append("Server Software: ").append(serverSoftware).append("\n")
                .append("Java Version: ").append(javaVersion).append("\n")
                .append("OS: ").append(osInfo).append("\n\n");


        debug.append("-- Installed Plugins --\n")
                .append(getInstalledPlugins()).append("\n\n");

        debug.append("-- Lifecycle Logs --\n").
                append(getPluginLogs()).append("\n\n");
        // Configuration files
        debug.append("-- Configuration Files --\n");

        // Main config
        debug.append("# config.yml\n```yaml\n").
                append(plugin.getConfig().saveToString()).append("\n```\n\n");

        // Storage config
        debug.append("# storage.yml\n```yaml\n").
                append(plugin.getConfigManager().getStorageConfig().saveToString()).append("\n```\n\n");

        // Items config
        debug.append("# items.yml\n```yaml\n").
                append(plugin.getConfigManager().getCustomItemConfig().saveToString()).append("\n```\n\n");


        return debug.toString();
    }

    /**
     * Extracts plugin-related logs from the server log file
     *
     * @return A string containing all plugin-related log entries
     */
    private String getPluginLogs() {
        StringBuilder logContent = new StringBuilder();

        try {
            File logFile = new File("logs/latest.log");
            if (!logFile.exists()) {
                // Try to find the log file in a different location for some server types
                File serverDir = new File(".");
                Optional<File> latestLog = Arrays.stream(serverDir.listFiles())
                        .filter(f -> f.isFile() && f.getName().endsWith(".log"))
                        .max(Comparator.comparingLong(File::lastModified));

                if (latestLog.isPresent()) {
                    logFile = latestLog.get();
                } else {
                    return "Could not find server log file.";
                }
            }

            // Pattern to match log entries with the plugin name
            String pluginName = plugin.getDescription().getName();
            Pattern logPattern = Pattern.compile(".*\\[(.*" + pluginName + ".*)\\].*");

            // Read the log file and extract plugin-related entries
            int linesAdded = 0;
            int maxLines = 500; // Limit the number of log lines to prevent oversized reports

            List<String> logLines = Files.readAllLines(logFile.toPath());
            // Read in reverse to get most recent logs first
            for (int i = logLines.size() - 1; i >= 0 && linesAdded < maxLines; i--) {
                String line = logLines.get(i);
                Matcher matcher = logPattern.matcher(line);
                if (matcher.matches() || line.contains(pluginName)) {
                    logContent.append(line).append("\n");
                    linesAdded++;
                }

                // Also include any error or exception lines
                if (line.contains("ERROR") || line.contains("Exception") || line.contains("Error:")) {
                    // Check if this error is related to our plugin by looking at surrounding lines
                    boolean isRelated = false;
                    for (int j = Math.max(0, i - 5); j < Math.min(logLines.size(), i + 5); j++) {
                        if (logLines.get(j).contains(pluginName)) {
                            isRelated = true;
                            break;
                        }
                    }

                    if (isRelated) {
                        logContent.insert(3, line + "\n");
                        linesAdded++;
                    }
                }
            }

            // Add note if we hit the line limit
            if (linesAdded >= maxLines) {
                logContent.insert(3, "// Note: Log output limited to " + maxLines + " lines (most recent first)\n");
            }

        } catch (IOException e) {
            logContent.append("Failed to read log file: ").append(e.getMessage());
        }

        return logContent.toString();
    }

    /**
     * Generates an SHA-256 hash of the plugin's binary.
     *
     * @return The hash as a hexadecimal string.
     */
    private String generatePluginHash() {
        try {
            File pluginFile = plugin.getPluginFile();
            if (pluginFile == null || !pluginFile.exists()) {
                return "UNKNOWN";
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(pluginFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                hexString.append(String.format("%02x", hashByte));
            }
            return hexString.toString();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to generate plugin hash", e);
            return "ERROR";
        }
    }

    /**
     * Retrieves a list of installed plugins on the server.
     *
     * @return A formatted string containing all installed plugins.
     */
    private String getInstalledPlugins() {
        StringBuilder pluginInfo = new StringBuilder();
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            pluginInfo.append("- ").append(p.getName())
                    .append(" v").append(p.getDescription().getVersion())
                    .append(" (Enabled: ").append(p.isEnabled()).append(")")
                    .append("\n");
        }
        return pluginInfo.toString();
    }

    /**
     * Uploads the debug dump to mclo.gs and returns the paste URL.
     *
     * @param content The debug log content.
     * @return The mclo.gs URL or null if upload failed.
     */
    private String uploadToMclogs(String content) {
        try {
            URL url = new URL("https://api.mclo.gs/1/log");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("User-Agent", "LifeStealZ/" + plugin.getDescription().getVersion());
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Encode content for HTTP request
            String encodedContent = "content=" + URLEncoder.encode(content, StandardCharsets.UTF_8);

            // Write data to request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = encodedContent.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine);
                    }

                    // Parse JSON response
                    return parseMclogsResponse(response.toString());
                }
            } else {
                plugin.getLogger().warning("Failed to upload to mclo.gs. Response code: " + responseCode);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to upload debug report", e);
        }
        return null;
    }

    /**
     * Extracts the URL from the mclo.gs JSON response.
     *
     * @param jsonResponse The raw JSON response.
     * @return The URL of the uploaded log or null if failed.
     */
    private String parseMclogsResponse(String jsonResponse) {
        if (jsonResponse.contains("\"success\":true") && jsonResponse.contains("\"url\":\"")) {
            int urlStart = jsonResponse.indexOf("\"url\":\"") + 7;
            int urlEnd = jsonResponse.indexOf("\"", urlStart);
            return jsonResponse.substring(urlStart, urlEnd);
        }
        plugin.getLogger().warning("Invalid response from mclo.gs: " + jsonResponse);
        return null;
    }
}