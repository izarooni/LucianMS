package server.events;

import java.util.ArrayList;

import server.TimerManager;
import server.events.custom.auto.TestEvent;
import server.events.custom.auto.TestEvent2;

public class AutoEventManager {
	
	private boolean running = false;
	
	private int curr = 0;
	
	private static ArrayList<AutoEvent> events;
	
	private int timeInSeconds;
	
	public AutoEventManager() {
		 
		 events = new ArrayList<>();
	}
	
	public void runLater(int eventTime) {
		TimerManager.getInstance().schedule(() -> {
			if(!running) {
				if(curr >= events.size()-1) curr = 0; else curr++;
				events.get(curr).onStart();
				runEvent(eventTime);
				running = true;
			}
		}, 2 * 1000 * 60 * 60); // 2 hours
		
	}
	
	public boolean runEvent(int time) {
		TimerManager.getInstance().schedule(() -> {
			if(running) {
				// if it's not running, you did something wrong.. anders.
				if(curr >= events.size()-1) curr = 0; else curr++;
				events.get(curr).onEnd();
				running = false;
				runLater(20);
				
			}
		}, time * 1000 * 60);
		return false;
	}
	
	public static void registerEvent(AutoEvent event) {
		getEvents().add(event);
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public int getCurr() {
		return curr;
	}

	public void setCurr(int curr) {
		this.curr = curr;
	}

	public static ArrayList<AutoEvent> getEvents() {
		return events;
	}

	public static void setEvents(ArrayList<AutoEvent> events) {
		AutoEventManager.events = events;
	}

	public int getTimeInSeconds() {
		return timeInSeconds;
	}

	public void setTimeInSeconds(int timeInSeconds) {
		this.timeInSeconds = timeInSeconds;
	}
	// we're just going to register all the events at the start.
	public void registerAll() {
		AutoEventManager.registerEvent(new TestEvent());
		AutoEventManager.registerEvent(new TestEvent2());
	}
	
	
	
	

	
	
}
