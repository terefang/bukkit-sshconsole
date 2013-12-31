package terefang.bukkit.scripting.impl;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

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
				if(e.getValue()!=null)
				{
					l_bind.put(e.getKey(), e.getValue());
				}
				else
				{
					log.log(Level.WARNING, "binding '"+e.getKey()+"' of file "+scriptFile+" is NULL");
				}
			}
			l_bind.put("args", args);	
			
			ScriptContext sc = new SimpleScriptContext();
			sc.setBindings(l_bind, ScriptContext.GLOBAL_SCOPE);
			
			Object ret = l_script.eval(sc);
			
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
