package terefang.bukkit.plugin.sshconsole;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
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

import org.apache.mina.util.Base64;

import org.jasypt.digest.StandardStringDigester;

public class SshConsolePlugin 
extends JavaPlugin
implements PasswordAuthenticator, CommandFactory, Factory, PublickeyAuthenticator
{
	StandardStringDigester pwdenc = new StandardStringDigester();
    public static final String PLUGIN_TAG = "[SshConsole]";
    public static final String PLUGIN_NAME = "SshConsole";
    SshServer sshd = null;
    private Configuration config = null;

    public SshConsolePlugin()
    {
        sshd = null;
        
        pwdenc.setAlgorithm("SHA1");
        pwdenc.setInvertPositionOfPlainSaltInEncryptionResults(true);
        pwdenc.setInvertPositionOfSaltInMessageBeforeDigesting(true);
        pwdenc.setIterations(1);
        pwdenc.setPrefix("{SSHA}");
        pwdenc.setSaltSizeBytes(8);
        pwdenc.setStringOutputType("hexadecimal");
        pwdenc.initialize();
    }

    public static void main(String args[])
    throws Throwable
    {
        SshConsolePlugin plugin = new SshConsolePlugin();
        SshServer sshd = SshServer.setUpDefaultServer();
        sshd.setPort(25522);
        sshd.setKeyPairProvider(((org.apache.sshd.common.KeyPairProvider) (new SimpleGeneratorHostKeyProvider("hostkey.ser"))));
        sshd.setPasswordAuthenticator( new PasswordAuthenticator() 
        {
			@Override
			public boolean authenticate(String arg0, String arg1, ServerSession arg2) 
			{
				return true;
			}
		});
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

	@Override
    public boolean authenticate(String username, String password, ServerSession ssession)
    {
        reloadConfig();

        if(getConfig().getBoolean("public-admin", false))
        {
            return true;
        }
        
        if(verifyUserPassword(username, password))
        {
            return true;
        } 

        return false;
    }

	@Override
	public boolean authenticate(String username, PublicKey pKey, ServerSession ssession) 
	{
        reloadConfig();

        if(verifyUserKey(username, pKey))
        {	
            return true;
        } 

        return false;
	}

	public void setUserPassword(String username, String password, boolean encrypt)
	{
		if(encrypt)
		{
			getConfig().set("user."+username, pwdenc.digest(password));
		}
		else
		{
			getConfig().set("user."+username, "{plain}"+password);
		}
	}
	
	public void setUserKey(String username, PublicKey pKey, boolean encrypt)
	{
		String suffix = ("/"+pKey.getAlgorithm()+"/"+pKey.getFormat())
				.replace('.', '-')
				.replace(':', '-');

		if(encrypt)
		{
			String bKey = new String(Base64.encodeBase64(pKey.getEncoded()));
			getConfig().set("user."+username+suffix, pwdenc.digest(bKey));
		}
		else
		{
			String eKey = new BigInteger(pKey.getEncoded()).toString(36).toUpperCase();
			getConfig().set("user."+username+suffix, "{raw}"+eKey);
		}
	}
	
	public boolean verifyUserPassword(String username, String password)
	{
		if(!getConfig().isString("user."+username))
		{
			return false;
		}
		
		if(getConfig().getString("user."+username, "#*+!$%&").equalsIgnoreCase("{plain}"+password))
		{
			return true;
		}

		if(pwdenc.matches(password, getConfig().getString("user."+username, "#*+!$%&")))
		{
			return true;
		}
		
		return false;
	}
	
	public boolean verifyUserKey(String username, PublicKey pKey)
	{
		String suffix = ("/"+pKey.getAlgorithm()+"/"+pKey.getFormat())
				.replace('.', '-')
				.replace(':', '-');
		
		if(!getConfig().isString("user."+username+suffix))
		{
			return false;
		}
		
		String eKey = new BigInteger(pKey.getEncoded()).toString(36).toUpperCase();

		if(getConfig().getString("user."+username+suffix, "#*+!$%&").equalsIgnoreCase("{raw}"+eKey))
		{
			return true;
		}

		if(pwdenc.matches(eKey, getConfig().getString("user."+username+suffix, "#*+!$%&")))
		{
			return true;
		}
		
		String bKey = new String(Base64.encodeBase64(pKey.getEncoded()));

		if(pwdenc.matches(bKey, getConfig().getString("user."+username+suffix, "#*+!$%&")))
		{
			return true;
		}
    	info("user '"+username+"' not found for suffix '"+suffix+"' on key '{raw}"+eKey+"'");
    	info("user '"+username+"' not found for suffix '"+suffix+"' on key '{b64}"+bKey+"'");
		
		return false;
	}
	
    public void log(Level l, String msg, Throwable t)
    {
        getLogger().log(l, msg, t);
    }

    public void log(Level l, String msg)
    {
        getLogger().log(l, msg);
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
        try
        {
            sshd = SshServer.setUpDefaultServer();
            sshd.setPort(getConfig().getInt("port"));
            if(!getConfig().getBoolean("force-nio2"))
            {
                sshd.setIoServiceFactory(new MinaServiceFactory());
            }
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File(getDataFolder(), "hostkey.ser").getAbsolutePath()));
            sshd.setPasswordAuthenticator(this);
            sshd.setPublickeyAuthenticator(this);
            sshd.setShellFactory(this);

            sshd.start();
            
            info("started on *:"+sshd.getPort());
        }
        catch(Exception e)
        {
            error("starting ssh-listener", e);
        }
    }

    private void initConfiguration()
    {
        if(config == null)
        {
            config = ((Configuration) (getConfig()));
        }
        config.options().copyDefaults(true);
        getConfig().set("force-nio2", getConfig().getBoolean("force-nio2", false));
        getConfig().set("public-admin", getConfig().getBoolean("public-admin", false));
        getConfig().set("port", getConfig().getInt("port", 25522));
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