package server.events.custom;

import client.MapleCharacter;
import net.SendOpcode;
import net.server.channel.handlers.UseItemHandler;
import scheduler.Task;
import scheduler.TaskExecutor;
import server.life.MapleLifeFactory;
import server.life.MapleNPC;
import tools.annotation.PacketWorker;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

/**
 * @author izarooni
 */
public class FlappyBirdController extends GenericEvent {

    private static final int BirdNpc = 9000042;
    private static final int GroundY = 240;

    private static final double HSpeed = 105.0;
    private static final double JSpeed = 300.0;
    private static final double FallingSpeed = 9;

    private MapleNPC npc = null;
    private Task task = null;

    private Vector2D location = new Vector2D(0, -250);
    private Vector2D velocity = new Vector2D(0, 0);

    private boolean pLeftSide = false;

    public FlappyBirdController() {
        registerAnnotationPacketEvents(this);
    }

    @Override
    public void registerPlayer(MapleCharacter player) {
        player.dropMessage("Registered to Flappy Bird");
        player.addGenericEvent(this);
        npc = MapleLifeFactory.getNPC(FlappyBirdController.BirdNpc);
        sendFieldEnter(player);

        npc = new MapleNPC(BirdNpc, null);

        task = TaskExecutor.createRepeatingTask(new FlappyBirdWorker(player), 1000 / 60, 0);
    }

    @Override
    public void unregisterPlayer(MapleCharacter player) {
        player.dropMessage("Unregistered from Flappy Bird");
        player.removeGenericEvent(this);

        sendFieldLeave(player);
        npc = null;

        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void onPlayerDisconnect(MapleCharacter player) {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @PacketWorker
    public void onItemUse(UseItemHandler event) {
        int itemId = event.getItemId();
        if (itemId == 2002001) { // jump
            velocity.y = -JSpeed;
        } else if (itemId == 2002002) { // exit
            unregisterPlayer(event.getClient().getPlayer());
        }
        event.setCanceled(true);
    }

    private void sendFieldEnter(MapleCharacter player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(23);
        mplew.writeShort(SendOpcode.SPAWN_NPC.getValue());
        mplew.writeInt(npc.getObjectId());
        mplew.writeInt(npc.getId());
        mplew.writeShort(npc.getPosition().x);
        mplew.writeShort(npc.getCy());
        mplew.write(npc.getF() == 1 ? 0 : 1);
        mplew.writeShort(npc.getFh());
        mplew.writeShort(npc.getRx0());
        mplew.writeShort(npc.getRx1());
        mplew.write(1);
        player.announce(mplew.getPacket());
    }

    private void sendFieldLeave(MapleCharacter player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);
        mplew.writeShort(SendOpcode.REMOVE_NPC.getValue());
        mplew.writeInt(npc.getObjectId());
        player.announce(mplew.getPacket());
    }

    private class Vector2D {

        private double x, y;

        private Vector2D(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return String.format("Vector2D{x=%s, y=%s}", x, y);
        }

        private Point toPoint() {
            return new Point((int) x, (int) y);
        }

        private void set(double x, double y) {
            this.x = x;
            this.y = y;
        }

        private void subtract(double x, double y) {
            this.x -= x;
            this.y -= y;
        }

        private void add(double x, double y) {
            this.x += x;
            this.y += y;
        }
    }

    private class FlappyBirdWorker implements Runnable {

        private final MapleCharacter player;
        private long printLast = 0L;
        private long timeLast = 0L;

        private FlappyBirdWorker(MapleCharacter player) {
            this.player = player;
        }

        @Override
        public void run() {
            long timeNow = System.nanoTime();
            double dTime = (timeNow - timeLast) / 1e9;
            timeLast = timeNow;

            location.x += velocity.x * dTime;
            location.y += velocity.y * dTime;

            if (location.y < GroundY) {
                velocity.y += FallingSpeed;
            } else if (location.y >= GroundY) {
                location.y = GroundY;
                velocity.y = 0;
            }

            int px = player.getPosition().x;

            if ((int) location.x < px) {
                velocity.x = HSpeed;
                pLeftSide = false;
            } else if ((int) location.x > px) {
                velocity.x = -HSpeed;
                pLeftSide = true;
            } else {
                velocity.x = 0;
            }

            if (System.currentTimeMillis() - printLast > 1000) {
                printLast = System.currentTimeMillis();
                System.out.println("delta-time: " + dTime);
                System.out.println("Locaiton: " + location.toString());
                System.out.println("Velocity: " + velocity.toString());
                System.out.println("-------------- END --------------");
            }

            npc.getPosition().setLocation(location.toPoint());
            npc.setRx0((int) location.x);
            npc.setRx1((int) location.x);
            npc.setCy((int) location.y);
            npc.setF(pLeftSide ? 0 : 1);
            npc.setFh(0);

            // update location
            sendFieldLeave(player);
            sendFieldEnter(player);
        }
    }
}
