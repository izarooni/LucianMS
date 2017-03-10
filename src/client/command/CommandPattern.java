package client.command;

import client.MapleClient;

public interface CommandPattern {
	
	boolean execute(MapleClient c, char header, String command, String[] args);
	
}
