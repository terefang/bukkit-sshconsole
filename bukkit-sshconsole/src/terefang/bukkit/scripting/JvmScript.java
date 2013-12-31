package terefang.bukkit.scripting;

import java.io.File;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

public interface JvmScript 
{
	public void initScript(Properties properties, File scriptFile);
	public void initScript(Properties properties, Reader scriptFile);
	public void initScript(Properties properties, String scriptCode);

	public File getScriptFile();
	public void setScriptFile(File scriptFile);

	public Properties getScriptProperties();
	public void setScriptProperties(Properties scriptProperties);

	public Object runScript(Map<String,Object> context, String[] args, boolean multiplicity);
}
