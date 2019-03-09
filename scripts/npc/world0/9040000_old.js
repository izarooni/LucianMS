// var status = 0;
// var GQItems = new Array(1032033, 4001024, 4001025, 4001026, 4001027, 4001028, 4001029, 4001030, 4001031, 4001032, 4001033, 4001034, 4001035, 4001037);

// function start() {
//     cm.sendSimple("The path to Sharenian starts here. What would you like to do? #b\r\n#L0#Start a Guild Quest#l\r\n#L1#Join your guild's Guild Quest#l");
// }

// function action(mode, type, selection) {
//     if (mode == -1) 
//         cm.dispose();
//     else {
//         if (mode == 0 && status == 0) {
//             cm.dispose();
//             return;
//         }
//         if (mode == 1)
//             status++;
//         else
//             status--;
//         if (status == 1) {
//             if (selection == 0) { //Start
// 				if (cm.getPlayer().getGuildId() == 0 || cm.getPlayer().getGuildRank() >= 3) { //no guild or not guild master/jr. master
//                     cm.sendNext("Only a Master or Jr. Master of the guild can start an instance.");
//                     cm.dispose();
//                 }
//                 else {
//                     var em = cm.getEventManager("GuildQuest");
//                     if (em == null) 
//                         cm.sendOk("This trial is currently under construction.");
//                     else {
// 						if(em.getProperty("gpqOpen").equals("false")) {
// 							cm.sendOk("Another guild has already registered for the quest. Please try again later.");
//                         } else if (getEimForGuild(em, cm.getPlayer().getGuildId()) != null) 
//                             cm.sendOk("Your guild already is already registered.");
//                         else {
//                             var guildId = cm.getPlayer().getGuildId();
//                             var eim = em.newInstance(guildId);
//                             em.startInstance(eim, cm.getPlayer().getName());
// 							em.setProperty("gpqOpen", "false");
//                             var map = eim.getMapInstance(990000000);
//                             map.getPortal(5).setScriptName("guildwaitingenter");
//                             map.getPortal(4).setScriptName("guildwaitingexit");
//                             eim.registerPlayer(cm.getPlayer());
//                             cm.guildMessage(5, "The guild has been entered into the Guild Quest. Please report to Shuang at the Excavation Camp on channel " + cm.getClient().getChannel() + ".");
//                             for (var i = 0; i < GQItems.length; i++) 
//                                 cm.removeAll(GQItems[i]);
//                         }
//                     }
//                     cm.dispose();
//                 }
//             }
//             else if (selection == 1) { //entering existing GQ
//                 if (cm.getPlayer().getGuildId() == 0) { //no guild or not guild master/jr. master
//                     cm.sendNext("You must be in a guild to join an instance.");
//                     cm.dispose();
//                 }
//                 else {
//                     var em = cm.getEventManager("GuildQuest");
//                     if (em == null)
//                         cm.sendOk("This trial is currently under construction.");
//                     else {
//                         var eim = getEimForGuild(em, cm.getPlayer().getGuildId());
//                         if (eim == null) 
//                             cm.sendOk("Your guild is currently not registered for the Guild Quest.");
//                         else {
//                             if ("true".equals(eim.getProperty("canEnter"))) {
//                                 eim.registerPlayer(cm.getPlayer());
//                                 for (var i = 0; i < GQItems.length; i++)
//                                     cm.removeAll(GQItems[i]);
//                             }
//                             else 
//                                 cm.sendOk("I'm sorry, but the guild has gone on without you. Try again later.");
//                         }
//                     }
//                     cm.dispose();
//                 }
//             }
//         }
//     }
// }

// function getEimForGuild(em, id) {
//     var stringId = "" + id;
//     return em.getInstance(stringId);
// }

// function isGuildQuestOwner(em, id) {
// 	var stringId = "" + id;
// 	if(em.getProperty("curGuild").equals(stringId))
// 		return true;
		
// 	return false;
// }