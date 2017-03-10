package server.events;

public interface AutoEvent {

	int channel = 1;
	
	public void onStart();
	public void onEnd();
	
}
