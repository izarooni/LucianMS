package server.events.custom;

import java.awt.Point;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;

import client.MapleCharacter;
import net.server.Server;
import server.TimerManager;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

public class Events {

	public static Events instance = null;
	
	private HashMap<Integer, Integer> participants; // first argument is the character id, the second one is 0 or 1 depending if the player is a winner
	
	private static boolean canJoin = false;
	ScheduledFuture<?> countdownTask;
	private int map;
	
	private Point spawnLocation;
	
	private MapleCharacter starter;
	
	String eventTitle;
	private int time = 0;
	private boolean active;
	
	private final int RETURN_MAP = 910000000;
	public static final int DEFAULT_TIME = 80;
	
	// oxquiz: 109020001
	
	String announcement = "An event is starting in channel %s in %s %s, please use @joinevent to join it.";
	String eventGateClosed = "The event gate has closed";
	
	int[] secondAnnouncements = new int[] { // timestamps where you get a message of how many seconds are remaining.
			60,
			50,
			30,
			15,
			10,
			5,
			3,
			2,
			1
	};

	
	
	public Events() {
		participants = new HashMap<>();
	}
	
	
	public void countdown(int timeLeft) {
		if(canJoin == false) {
			setCanJoin(true);
		}
		countdownTask = TimerManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				int timer = timeLeft;
				if(timer != 0) {
					if(timer % 60 == 0) {
						Server.getInstance().getWorld(starter.getWorld()).broadcastPacket(MaplePacketCreator.serverNotice(6, String.format(announcement, starter.getClient().getChannel(), timer / 60, "minute(s)")));
					} else if(timer <= 60) {
						if(contains(secondAnnouncements, timer)) {
							Server.getInstance().getWorld(starter.getWorld()).broadcastPacket(MaplePacketCreator.serverNotice(6, String.format(announcement, starter.getClient().getChannel(), timer, "seconds")));
						}
					}
					timer--;
					countdown(timer);
				} else {
					start();
				}
			}
			
		}, 1000);
	}
	
	public void create(MapleCharacter creator, String title) {
		this.starter = creator;
		this.eventTitle = title;
		this.map = creator.getMapId();
		this.spawnLocation = creator.getPosition();
	}
	
	public void create(MapleCharacter creator, String title, Point pos) {
		this.starter = creator;
		this.spawnLocation = pos;
		this.eventTitle = title;
		this.map = creator.getMapId();
	}
	
	public boolean joinEvent(MapleCharacter player) {
		if(player.getClient().getChannel() == starter.getClient().getChannel()) {
		 if(!getParticipants().containsKey(player.getId())) {
			MapleMap warpTo = player.getClient().getChannelServer().getMapFactory().getMap(map);
			player.dropMessage(6, "Joining the event..");
			join(player);
			player.changeMap(warpTo, (spawnLocation == null ? new Point(starter.getPosition().x, starter.getPosition().y) : spawnLocation));
		} else {
			player.dropMessage(5, "You are already participating in the event!");
			return false;
		}
			return true;
		} else {
			player.dropMessage(5, "Please go to channel " + starter.getClient().getChannel() + " and try again");
			return false;
		}
	}
	
	public boolean leaveEvent(MapleCharacter player) {
		if(getParticipants().containsKey(player.getId())) {
			if(getParticipants().get(player.getId()) >= 1) {
				player.dropMessage(6, "You have gained " + getParticipants().get(player.getId()) + " event points!");
				player.setEventPoints(player.getEventPoints() + getParticipants().get(player.getId()));
			}
			getParticipants().remove(player.getId());
			player.changeMap(RETURN_MAP);
			return true;
		} else {
			return false;
		}
		
	}
	
	public void end() {
		for(int id : getParticipants().keySet()) {
			MapleCharacter participant = Server.getInstance().getWorld(starter.getWorld()).getPlayerStorage().getCharacterById(id);
			leaveEvent(participant);
		}
		setEventTitle(null);
		setActive(false);
	}
	
	
	public void setSpawnpoint(int x, int y) {
		spawnLocation = new Point(x, y);
	}
	
	public void setSpawnpoint(Point pos) {
		spawnLocation = pos;
	}
	
	private void start() {
		setCanJoin(false);
		setActive(true);
		Server.getInstance().getWorld(starter.getWorld()).broadcastPacket(MaplePacketCreator.serverNotice(6, eventGateClosed));
		
	}
	
	public void join(MapleCharacter player) {
		participants.put(player.getId(), 0);
	}
	
	private boolean contains(int[] secondAnnouncements, int time) {
		for(int i = 0; i < secondAnnouncements.length; i++) {
			if(secondAnnouncements[i] == time) {
				return true;
			}
		}
		return false;
	}


	public static boolean canJoin() {
		return canJoin;
	}


	public static void setCanJoin(boolean canJoin) {
		Events.canJoin = canJoin;
	}
	
	public static Events getInstance() {
		if(instance == null) {
			instance = new Events();
		}
		return instance;
	}
	
	public HashMap<Integer, Integer> getParticipants() {
		return participants;
	}


	public String getEventTitle() {
		return eventTitle;
	}


	public void setEventTitle(String eventTitle) {
		this.eventTitle = eventTitle;
	}


	public int getTime() {
		return time;
	}


	public void setTime(int time) {
		this.time = time;
	}


	public boolean isActive() {
		return active;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}
	
	
	
}
