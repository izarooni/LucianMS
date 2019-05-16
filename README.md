# LucianMS
Now known as Chirithy!  
**Java 8** and above is required so long as the [Nashorn Script Engine](https://docs.oracle.com/javase/8/docs/jdk/api/nashorn/jdk/nashorn/api/scripting/NashornScriptEngine.html) is available.  
_I'm look at you Java 11 (They [deprecated](https://github.com/junit-team/junit5/issues/1481) the feature)._

# Starting the server
#### Understand the bound ports
This server emulator is required to run as two separate JVM processes.  
One for the Login Server (default bound to port `8484`) and one for the Channel Server (default bound to `7575 + i + (n * 100)`) where `i` is the Channel ID and `n` is the World ID.  
The default ports (`8484` and `7575`) can be configured via `server-config.json` but the channel port calculation remains the same.  
Be sure to un-block the Login Server and Channel Server range of `TCP` ports from your in-bound firewall rules.  

If using a Discord bot, the bound port is `8483` and should not be allowed through firewall.

All three of these processes a currently programmed to run on the same system as they connect and communicate via localized network connections and raw packet data (No packet encryption modifications are used whatsoever as data is transferred through the local network).

---

#### Creating your .bat file
Your most basic command line with **required** arguments for starting a server JVM is as follows:
```
java -jar SERVER.jar -Dwzpath=wz -Dnashorn.args=--language=es6
```
- `-jar SERVER.jar` runs the JAR file as an executable file where `SERVER.jar` is to be replaced with the path to your compiled JAR file for your login and channel server
- `-Dwzpath=wz` creates a System property called `wzpath` and assigns the value `wz`
- `-Dnashorn.args=--language=es6` creates a System property which enables _some_ ES6 features in the JDK8+ script engine processor
 
---

#### Utilizing the new encryption
Under your `server-config.json` properties, should the `UseNewEncryption` property be assigned to `true`, your game client localhost will need to update its `IGCipher` hash numbers.  
In the IGCipher client functions, the number will need to be changed from `F25350C6` to `FF11252D`.
**Note:** These numbers are represented in the Big Endian format in the client, and Little Endian in the server. 