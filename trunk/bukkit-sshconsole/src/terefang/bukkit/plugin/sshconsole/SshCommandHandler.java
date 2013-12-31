package terefang.bukkit.plugin.sshconsole;

import com.google.common.base.Charsets;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.*;
import jline.ConsoleReader;
import org.apache.sshd.server.*;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.session.ServerSession;
import org.bukkit.Bukkit;

import terefang.bukkit.scripting.JvmScriptFactory;
import terefang.bukkit.scripting.impl.AbstractJvmScript;

public class SshCommandHandler extends Handler
implements Runnable, Command, SessionAware, ChannelSessionAware
{

    SshConsolePlugin sshdPlugin = null;
    String command = null;
    private Level logLevel = null;
    private InputStream in = null;
    private InputStream pin = null;
    private OutputStream out = null;
    private OutputStream err = null;
    private ExitCallback callback = null;
    private Environment environment = null;
    private Thread thread = null;
    private ConsoleReader reader = null;
    OutputStreamWriter pwriter = null;
    ServerSession serverSession = null;
    ChannelSession channelSession = null;
    public static SimpleDateFormat sfmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

    public SshCommandHandler(SshConsolePlugin sshdPlugin, String command)
    {
        this.sshdPlugin = null;
        logLevel = Level.INFO;
        this.sshdPlugin = sshdPlugin;
        this.command = command;
    }

    public void run()
    {
        try
        {
            pwriter = new OutputStreamWriter(out, Charsets.UTF_8) 
            {
            	@Override
                public void write(int c)
                throws IOException
                {
                    if(c == 10)
                    {
                        super.write(10);
                        super.write(13);
                    } else
                    {
                        super.write(c);
                    }
                }

            	@Override
                public void write(char cbuf[], int off, int len)
                throws IOException
                {
                    for(int i = 0; i < len; i++)
                    {
                        write(((int) (cbuf[i + off])));
                    }

                }

            	@Override
                public void write(String str, int off, int len)
                throws IOException
                {
                    write(str.toCharArray(), off, len);
                }

            };
            
            pin = new FilterInputStream(in) 
            {
            	@Override
                public int read()
                    throws IOException
                {
                    int c = super.read();
                    if(c == 127)
                    {
                        c = 8;
                    }
                    return c;
                }
            };
            
            try
            {
                reader = new ConsoleReader(pin, (Writer) pwriter);
                reader.printString("***** Welcome to CraftBukkit Shell.");
                reader.printNewline();
                reader.flushConsole();
                reader.setDefaultPrompt("server # ");
                if(serverSession != null)
                {
                    reader.setDefaultPrompt(serverSession.getUsername()+"@console # ");
                }

	            String line;
	            while((line = reader.readLine()) != null) 
	            {
	                handleUserInput(line.trim());
	            }
	        }
	        catch(InterruptedIOException interruptedioexception)
	        {
	            // ignore 
	        }
	        catch(Exception exception)
	        {
	            // ignore 
	        }
		}
        finally
        {
            callback.onExit(0);
        }
    }

    private void handleUserInput(String line)
    throws InterruptedIOException
    {
        line = line.trim();
        if(line.length() == 0)
        {
        	handleHelpCommand();
            return;
        }
        
        if(line.charAt(0) == '?')
        {
            handleHelpCommand();
        } 
        else
        if(line.charAt(0) == '.')
        {
            handleConsoleCommand(line.substring(1));
        } 
        else
        if(line.charAt(0) == '$')
        {
            handleScriptCommand(line.substring(1));
        } 
        else
        if(line.charAt(0) == '!')
        {
            handleSayCommand(line.substring(1));
        } 
        else
        if(line.charAt(0) == '/')
        {
            handleServerCommand(line.substring(1));
        }
        else
        {
            handleHelpCommand();
        } 
    }

    private synchronized void handleHelpCommand()
    {
        println("Use '.exit' to Exit.");
        println("Use '/<command> <args...>' for server commands.");
        println("Use '$<file> <args...>' for script commands.");
        println("Use '!...' to Shout.\n");
    }

    private static String[] translateCommandline(final String toProcess) 
    {
        if (toProcess == null || toProcess.length() == 0) {
            // no command? no string
            return new String[0];
        }

        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        Vector v = new Vector();
        StringBuffer current = new StringBuffer();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
            case inQuote:
                if ("\'".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            case inDoubleQuote:
                if ("\"".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            default:
                if ("\'".equals(nextTok)) {
                    state = inQuote;
                } else if ("\"".equals(nextTok)) {
                    state = inDoubleQuote;
                } else if (" ".equals(nextTok)) {
                    if (lastTokenHasBeenQuoted || current.length() != 0) {
                        v.addElement(current.toString());
                        current = new StringBuffer();
                    }
                } else {
                    current.append(nextTok);
                }
                lastTokenHasBeenQuoted = false;
                break;
            }
        }

        if (lastTokenHasBeenQuoted || current.length() != 0) {
            v.addElement(current.toString());
        }

        if (state == inQuote || state == inDoubleQuote) {
            throw new IllegalArgumentException("Unbalanced quotes in "
                    + toProcess);
        }

        String[] args = new String[v.size()];
        v.copyInto(args);
        return args;
    }
    
    private synchronized void handleScriptCommand(String line)
    throws InterruptedIOException
    {
		File sDir = new File(sshdPlugin.getDataFolder(), "scripts");
		sDir.mkdirs();

		if(line.length()==0)
    	{
    		String[] scripts = sDir.list(new FilenameFilter() 
    		{
				@Override
				public boolean accept(File dir, String name) 
				{
					return new File(dir,name).isFile();
				}
			});
			println("----");
    		for(String x : scripts)
    		{
    			println("> "+x);
    		}
			println("----");
    	}
		else
		{
			String script = line.trim();
			int off = script.indexOf(' ');
			if(off>0)
			{
				script = script.substring(0,off);
				line = line.substring(off).trim();
			}
			else
			{
				line = "";
			}
			
			File sFile = new File(sDir,script);
			if(sFile.isFile())
			{
				try 
				{
					AbstractJvmScript xscript = JvmScriptFactory.create("bsh");
					xscript.initScript(new Properties(), sFile);
					Map<String, Object> context = new HashMap();
					context.put("CONSOLE", reader);
					context.put("PLUGIN", sshdPlugin);
					context.put("SERVER", sshdPlugin.getServer());
					xscript.runScript(context, translateCommandline(line), true);
				} 
				catch (Exception xe) 
				{
					println(xe.getMessage());
					for(StackTraceElement ste : xe.getStackTrace())
					{
						println("c/"+ste.getClassName()+
							",m/"+ste.getMethodName()+
							",f/"+ste.getFileName()+
							":"+ste.getLineNumber());
					}
				}
			}
		}
    }

    private synchronized void handleConsoleCommand(String line)
    throws InterruptedIOException
    {
        if(line.equalsIgnoreCase("exit"))
        {
            throw new InterruptedIOException();
        }
    }

    private synchronized void handleServerCommand(String line)
    {
        try
        {
            Bukkit.dispatchCommand(((org.bukkit.command.CommandSender) (Bukkit.getConsoleSender())), line);
        }
        catch(Exception xe)
        {
            sshdPlugin.warn(xe.getMessage(), ((Throwable) (xe)));
        }
    }

    private synchronized void handleSayCommand(String line)
    {
        try
        {
            Bukkit.broadcastMessage("["+serverSession.getUsername()+"/Admin] "+line);
        }
        catch(Exception xe)
        {
            sshdPlugin.warn(xe.getMessage(), ((Throwable) (xe)));
        }
    }

    public void setSession(ServerSession ss)
    {
        serverSession = ss;
    }

    public void setChannelSession(ChannelSession cs)
    {
        channelSession = cs;
    }

    public InputStream getIn()
    {
        return in;
    }

    public OutputStream getOut()
    {
        return out;
    }

    public OutputStream getErr()
    {
        return err;
    }

    public Environment getEnvironment()
    {
        return environment;
    }

    public void setInputStream(InputStream in)
    {
        this.in = in;
    }

    public void setOutputStream(OutputStream out)
    {
        this.out = out;
    }

    public void setErrorStream(OutputStream err)
    {
        this.err = err;
    }

    public void setExitCallback(ExitCallback callback)
    {
        this.callback = callback;
    }

    public void start(Environment env)
        throws IOException
    {
        environment = env;
        thread = new Thread(this, "craftbukkit-ssh-console");
        thread.start();
    }

    public void destroy()
    {
        if(reader != null)
        {
           	try { reader.flushConsole(); } catch(Exception exception) { }
           	try { in.close(); } catch(Exception exception) { }
        	try { out.close(); } catch(Exception exception) { }
        	try { err.close(); } catch(Exception exception) { }
        }
        thread.interrupt();
    }

    public void flush()
    {
    }

    public void close()
    {
    }

    public synchronized void publish(LogRecord record)
    {
        try
        {
            if(record.getLevel().intValue() >= logLevel.intValue())
            {
                reader.printString(sfmt.format(new Date(record.getMillis())));
                reader.printString(" ");
                reader.printString(record.getLevel().getName());
                reader.printString(" ");
                reader.printString(record.getMessage());
                reader.printNewline();
                reader.flushConsole();
            }
        }
        catch(Exception exception) { }
    }

    public synchronized void println(Level lvl, String msg)
    {
        println(lvl+" - "+msg);
    }

    public void println(String msg)
    {
        try
        {
            reader.printString(msg);
            reader.printNewline();
            reader.flushConsole();
        }
        catch(Exception exception) { }
    }

}