package client.command;

import client.MapleClient;

public class CommandProcessor {

	PlayerCommands playerCommands = new PlayerCommands();
	GMCommands gmCommands = new GMCommands();
	AdminCommands adminCommands = new AdminCommands();
	
	public void execute(MapleClient c, char heading, String string, String[] args) {
		System.out.println("Player: " + c.getPlayer().getName() + " used command: " + joinToString(args));
		if(heading == '@') {
    		 playerCommands.execute(c, heading, args[0], args);
    	} else if(heading == '!') {
    		if(c.getPlayer().gmLevel() >= 1) {
    			 if(!gmCommands.execute(c, heading, args[0], args) && c.getPlayer().gmLevel() >= 2) {
    				 adminCommands.execute(c, heading, args[0], args);
    			 }
    		}
    		
    	} else if(heading == '/') {
    		
    	}
	}

	
	public String joinToString(String[] str) {
		StringBuilder sb = new StringBuilder();
		for(String string : str) {
			sb.append(string + " ");
		}
		return sb.toString();
	}
	
}
