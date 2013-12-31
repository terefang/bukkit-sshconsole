package terefang.bukkit.scripting.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

import terefang.bukkit.scripting.JvmScript;

public abstract class AbstractJvmScript implements JvmScript
{
	File scriptFile;
	String scriptCode;
	Properties scriptProperties;
	
	@Override
	public File getScriptFile() 
	{
		return scriptFile;
	}

	@Override
	public void setScriptFile(File scriptFile) 
	{
		this.scriptFile = scriptFile;
	}

	@Override
	public Properties getScriptProperties() 
	{
		return scriptProperties;
	}

	@Override
	public void setScriptProperties(Properties scriptProperties) 
	{
		this.scriptProperties = scriptProperties;
	}

	@Override
	public void initScript(Properties properties, File scriptFile)
	{
		this.scriptProperties = new Properties(properties);
		this.scriptFile = scriptFile;
		
		Reader rdr = null;
		try
		{
			rdr = new FileReader(scriptFile);
			initScript(properties, rdr);
		}
		catch(Exception xe) {}
		finally 
		{ 
			if(rdr!=null)
			{
			try{rdr.close(); } catch(Exception xe) {};
			}
		}
	}

	@Override
	public void initScript(Properties properties, Reader scriptFile)
	{
		String line = "#";
		StringBuilder sb = new StringBuilder();
		BufferedReader rdr = null;
		try
		{
			rdr = new BufferedReader(scriptFile);
			
			while(line.startsWith("#"))
			{
				line = rdr.readLine();
			}

			sb.append(line+"\n");

			while((line = rdr.readLine())!=null)
			{
				sb.append(line+"\n");
			}
		}
		catch(Exception xe) {}
		finally 
		{ 
			initScript(properties, sb.toString());
		}
	}

	@Override
	public abstract void initScript(Properties properties, String scriptCode);

	@Override
	public abstract Object runScript(Map<String, Object> context, String[] args, boolean multiplicity);
}
