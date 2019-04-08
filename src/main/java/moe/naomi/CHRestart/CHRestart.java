package moe.naomi.CHRestart;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.logging.Logger;

public class CHRestart extends JavaPlugin {

    private Logger log = Logger.getLogger("Minecraft");

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!isConfigured()) {
            getLogger().warning("CHRestart configuration looks to be unmodified. Please configure the plugin before use.");
        }

        // Setup block for Unirest, to avoid warnings about invalid cookie expiry values
        RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
        HttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();
        Unirest.setHttpClient(httpclient);


        log.info(getDescription().getName() + " version " + getDescription().getVersion() + " is now enabled.");
    }

    @Override
    public void onDisable() {
        try {
            Unirest.shutdown();
        }
        catch (IOException ex) {
            log.severe("CHRestart encountered an error while disabling!");
            ex.printStackTrace();
        }

        log.info(getDescription().getName() + " is now disabled.");
    }

    // Lazy check to see if the configuration has been set or not. If it hasn't, we'll want to notify such.
    private boolean isConfigured() {
        String key = getConfig().getString("API.Key", null);
        String user = getConfig().getString("API.User", null);
        int server = getConfig().getInt("API.ServerID", 200);
        return (key != null && !key.equals("AbCdEfGhIjKlMnOpQrStUvWxYz012345") && user != null && !user.equals("user@cubedhost.com") && server != 200);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "reboot":
                if (args.length > 0) {
                    sender.sendMessage(ChatColor.RED + "This command does not take arguments. Please re-run without them.");
                    break;
                }
                if (!isConfigured()) {
                    sender.sendMessage(ChatColor.RED + "This plugin does not appear to be configured. Please edit config.yml then run \"/chrestart reload\".");
                    break;
                }

                sender.sendMessage("Attempting to restart server...");

                try {
                    HttpResponse<JsonNode> postResponse = Unirest.post("https://prisma.cubedhost.com/api/server/{serverid}/restart")
                            .routeParam("serverid", Integer.toString(getConfig().getInt("API.ServerID")))
                            .header("Content-Type", "application/json")
                            .header("X-Api-Key", getConfig().getString("API.Key"))
                            .header("X-Api-User", getConfig().getString("API.User"))
                            .asJson();

                    if (postResponse.getBody().getObject().getBoolean("success")) {
                        sender.sendMessage(ChatColor.GREEN + "Restart command sent! Server restarting...");
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Restart command rejected. Server will not restart.");
                    }
                }
                catch (UnirestException ex) {
                    sender.sendMessage(ChatColor.RED + "Restart request failed to send. Please check the Console for further information.");
                    log.severe("CHRestarted failed to issue restart command!");
                    ex.printStackTrace();
                    break;
                }

                break; // return true;

            case "chrestart":
                switch (args.length) {
                    case 0:
                        sender.sendMessage(getDescription().getName() + " version " + getDescription().getVersion());
                        sender.sendMessage("Plugin " + (isConfigured() ? "appears" : "does not appear") + " to be configured.");
                        sender.sendMessage("To reload configuration, run \"/" + cmd.getLabel() + " reload\".");
                        break;
                    case 1:
                        if (!args[0].equalsIgnoreCase("reload")) {
                            sender.sendMessage("Invalid arguments. Please re-check your command.");
                            break;
                        }
                        if (!sender.hasPermission("chrestart.command.reload")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to perform this action.");
                            break;
                        }

                        reloadConfig();
                        sender.sendMessage("Configuration reloaded.");
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Too many arguments. Please re-check your command.");
                }

                break; // return true;

            default:
                return false;
        }

        return true;
    }

}
