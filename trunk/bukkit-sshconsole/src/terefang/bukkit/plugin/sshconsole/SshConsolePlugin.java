package terefang.bukkit.plugin.sshconsole;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.io.mina.MinaServiceFactory;
import org.apache.sshd.server.*;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationOptions;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class SshConsolePlugin 
extends JavaPlugin
implements PasswordAuthenticator, CommandFactory, Factory
{

    public static final String PLUGIN_TAG = "[SshConsole]";
    public static final String PLUGIN_NAME = "SshConsole";
    SshServer sshd = null;
    private Configuration config = null;

    public SshConsolePlugin()
    {
        sshd = null;
    }

    public static void main(String args[])
    throws Throwable
    {
        SshConsolePlugin plugin = new SshConsolePlugin();
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(25522);
        sshd.setKeyPairProvider(((org.apache.sshd.common.KeyPairProvider) (new SimpleGeneratorHostKeyProvider("hostkey.ser"))));
        sshd.setPasswordAuthenticator(((PasswordAuthenticator) (plugin)));
        sshd.setShellFactory(((Factory) (plugin)));
        sshd.setCommandFactory(((CommandFactory) (plugin)));
        sshd.start();
        do
        {
            Thread.sleep(10000L);
        } while(true);
    }

    public Command create()
    {
        return createCommand("shell");
    }

    public Command createCommand(String cline)
    {
        SshCommandHandler sch = new SshCommandHandler(this, cline);
        try
        {
            Bukkit.getServer().getLogger().addHandler(((java.util.logging.Handler) (sch)));
        }
        catch(Exception exception) { }
        return ((Command) (sch));
    }

    public boolean authenticate(String username, String password, ServerSession ssession)
    {
        if(getConfig().getBoolean("public-admin", false))
        {
            return true;
        }
        if(getConfig().isString((new StringBuilder("user.")).append(username).toString()))
        {
            return getConfig().getString((new StringBuilder("user.")).append(username).toString(), "#*+!$%&").equalsIgnoreCase(password);
        } else
        {
            return false;
        }
    }

    public void log(Level l, String msg, Throwable t)
    {
        getLogger().log(l, (new StringBuilder("[SshConsole] ")).append(msg).toString(), t);
    }

    public void log(Level l, String msg)
    {
        getLogger().log(l, (new StringBuilder("[SshConsole] ")).append(msg).toString());
    }

    public void info(String msg, Throwable t)
    {
        log(Level.INFO, msg, t);
    }

    public void info(String msg)
    {
        log(Level.INFO, msg);
    }

    public void warn(String msg, Throwable t)
    {
        log(Level.WARNING, msg, t);
    }

    public void warn(String msg)
    {
        log(Level.WARNING, msg);
    }

    public void error(String msg, Throwable t)
    {
        log(Level.SEVERE, msg, t);
    }

    public void error(String msg)
    {
        log(Level.SEVERE, msg);
    }

    public void onEnable()
    {
        getDataFolder().mkdirs();
        loadConfiguration();
        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(getConfig().getInt("port"));
        if(!getConfig().getBoolean("force-nio2"))
        {
            sshd.setIoServiceFactory(((org.apache.sshd.common.io.IoServiceFactory) (new MinaServiceFactory())));
        }
        sshd.setKeyPairProvider(((org.apache.sshd.common.KeyPairProvider) (new SimpleGeneratorHostKeyProvider((new File(getDataFolder(), "hostkey.ser")).getAbsolutePath()))));
        sshd.setPasswordAuthenticator(((PasswordAuthenticator) (this)));
        sshd.setShellFactory(((Factory) (this)));
        try
        {
            sshd.start();
        }
        catch(IOException e)
        {
            error("starting ssh-listener", ((Throwable) (e)));
        }
    }

    private void initConfiguration()
    {
        if(config == null)
        {
            config = ((Configuration) (getConfig()));
        }
        config.options().copyDefaults(true);
        getConfig().set("force-nio2", ((Object) (Boolean.valueOf(getConfig().getBoolean("force-nio2", false)))));
        getConfig().set("public-admin", ((Object) (Boolean.valueOf(getConfig().getBoolean("public-admin", false)))));
        getConfig().set("port", ((Object) (Integer.valueOf(getConfig().getInt("port", 25522)))));
        saveConfig();
    }

    private void loadConfiguration()
    {
        initConfiguration();
    }

    public void onDisable()
    {
        try
        {
            sshd.stop();
        }
        catch(InterruptedException e)
        {
            error("stopping ssh-listener", ((Throwable) (e)));
        }
        reloadConfig();
        saveConfig();
    }

}
