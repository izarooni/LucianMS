package scripting;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import client.MapleClient;

public class CommandParser {

	private ScriptEngine scriptEngine;
	private ScriptEngineManager scriptManager;
	
	public boolean parseCommand(MapleClient c) {
		if(scriptManager == null) {
			scriptManager = new ScriptEngineManager();
		}
		
		scriptEngine = scriptManager.getEngineByName("javascript");
		
		
		
		return false;
	}
	
}
