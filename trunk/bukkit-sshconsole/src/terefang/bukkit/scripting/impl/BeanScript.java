package terefang.bukkit.scripting.impl;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.CompiledScript;

import org.bukkit.Bukkit;

import bsh.BshScriptEngine;


public class BeanScript extends AbstractJvmScript 
{
	public static Logger log = Bukkit.getLogger();	
	
	private BshScriptEngine engine;

	private CompiledScript script;
	
	@Override
	public void initScript(Properties properties, String scriptCode) 
	{
		this.scriptProperties=properties;
		this.scriptCode=scriptCode;

		try
		{
			engine = new BshScriptEngine();
			script = engine.compile(scriptCode);
		} 
		catch (Exception e) 
		{
			log.log(Level.WARNING, "error", e);
		}
	}

	@Override
	public Object runScript(Map<String, Object> context, String[] args, boolean multiplicity) 
	{
		BshScriptEngine l_engine = engine;
		CompiledScript l_script = script;
		
		if(multiplicity)
		{
			try
			{
				l_engine = new BshScriptEngine();
				l_script = l_engine.compile(scriptCode);
			} 
			catch (Exception e) 
			{
				log.log(Level.WARNING, "error", e);
			}	
		}

		try
		{
			Bindings l_bind = l_engine.createBindings();
			for(Entry<String,Object> e : context.entrySet())
			{
				l_bind.put(e.getKey(), e.getValue());	
			}
			l_bind.put("args", args);	
			
			Object ret = l_script.eval(l_bind);
			l_bind.clear();
			return ret;
		}
		catch(Exception xe)
		{
			log.log(Level.WARNING, "error in file "+scriptFile, xe);
		}
		
		return null;
	}

}
