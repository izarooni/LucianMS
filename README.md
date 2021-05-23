# LucianMS
Now known as Chirithy!  
**Java 8** and above is required so long as the [Nashorn Script Engine](https://docs.oracle.com/javase/8/docs/jdk/api/nashorn/jdk/nashorn/api/scripting/NashornScriptEngine.html) is available.  
_I'm look at you Java 11 (They [deprecated](https://github.com/junit-team/junit5/issues/1481) the feature)._

# Start The Server
[Download the latest release](https://github.com/izarooni/LucianMS/releases)  
In your favorite [MySQL](https://dev.mysql.com/downloads/mysql/) browser, execute the .sql script `backups/chirithy_clean.sql`.  
Edit worlds (rates, messages, etc.) in `world.ini`.  
Edit all IP inside `server-config.json` depending if you want a local or remote server.  
Edit `database.properties` to connect to your server database.  
Add your own `wz` folder containing your server XMLs. You can make these files using HaRepacker or similar tools.

### Windows
Simply run `launch-channel.bat` and `launch-login.bat`. Follow any given instructions.

---
### Java arguments
If using your own script, these arguments must be included in your Java CLI:
```
-Dwzpath=wz
-Dlog4j.configurationFile=log4j.xml
```

The `-D` prefix assigns the key and value to your system properties.  
`wzpath` is a relative path to your wz directory containing server XMLs.  
`log4j.configurationFile` is a relative path to your log4j configuration file.  
Both are typically contained in your root directory (where the `scripts` folder is contained).

### Understand the bound ports
This server emulator is required to run as two separate JVM processes.  
The ports used are as follows and can be configured via file editing `server-config.json`:  
```
Login Server:   8484  
Channel Server: 7575 + channel-ID + (world-ID * 100)  
``` 
If using windows, be sure to un-block the `TCP` ports from your in-bound firewall rules.  

All three of these processes are to run on the same system as they connect via LAN and communicate with raw packet data.

---

### Modifying the IGCihper
Under your `server-config.json` properties, should the `UseNewEncryption` property be assigned to `true`, your game client localhost will need to update its `IGCipher` hash numbers.  
In the IGCipher client functions, the number will need to be changed from `F25350C6` to `FF11252D`.
**Note:** These numbers are represented in the Big Endian format in the client, and Little Endian in the server. 