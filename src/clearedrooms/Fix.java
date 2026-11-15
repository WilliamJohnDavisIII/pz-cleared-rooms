package clearedrooms;

import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

/** Public, because ZombieBuddy inlines the advice into IsoChunk. */
public final class Fix {
    private Fix() {}

    private static int kept;

    /** True when the room was explored by a player; also marks it spawned for good. */
    public static boolean keepClear(long roomId) {
        try {
            IsoWorld world = IsoWorld.instance;
            if (world == null || world.getMetaGrid() == null) return false;
            RoomDef def = world.getMetaGrid().getRoomDefByID(roomId);
            if (def == null || !def.explored) return false;
            if (!def.doneSpawn) {
                def.doneSpawn = true;
                if (kept++ < 200) {
                    System.out.println("[ClearedRooms] kept explored room " + def.getName() + " at "
                        + def.x + "," + def.y + "," + def.level + " clear (id " + roomId + ")");
                }
            }
            return true;
        } catch (Throwable t) {
            System.out.println("[ClearedRooms] check failed, vanilla behaviour kept: " + t);
            return false;
        }
    }
}
