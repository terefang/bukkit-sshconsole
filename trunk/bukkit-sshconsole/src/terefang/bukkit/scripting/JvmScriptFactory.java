package terefang.bukkit.scripting;

import terefang.bukkit.scripting.impl.AbstractJvmScript;
import terefang.bukkit.scripting.impl.BeanScript;

public abstract class JvmScriptFactory 
{
	public static AbstractJvmScript create(String lang) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		if("bsh".equalsIgnoreCase(lang) || "beanshell".equalsIgnoreCase(lang))
		{
			return new BeanScript();
		}
		
		return null;
	}
	
}
