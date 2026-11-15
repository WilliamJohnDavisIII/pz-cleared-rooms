package clearedrooms;

import me.zed_0xff.zombie_buddy.Patch;

/**
 * Build 42.22 fills a building's rooms with zombies when their chunk loads, skipping a room only if
 * RoomDef.doneSpawn is set or the chunk lists it as already spawned (IsoChunk.randomizeBuildingsEtc).
 * doneSpawn is only set when a room is "spotted", but IsoGridSquare.CalcVisibility and checkRoomSeen mark
 * the room explored *before* calling IsoRoom.onSee, and onSee only spots rooms that are not explored yet,
 * so the room the player actually looks into is never spotted and never gets doneSpawn. It then stays
 * fillable, and a cleared or boarded-up house can fill up again.
 *
 * isSpawnedRoom(long) is the question randomizeBuildingsEtc asks right before filling a room (its only
 * caller). We answer "yes" for any room a player has explored and set doneSpawn so the save keeps it.
 */
public final class RoomPatches {
    private RoomPatches() {}

    @Patch(className = "zombie.iso.IsoChunk", methodName = "isSpawnedRoom")
    public static class IsSpawnedRoom {
        @Patch.OnExit
        public static void exit(@Patch.Argument(0) long roomId,
                                @Patch.Return(readOnly = false) boolean spawned) {
            if (!spawned && Fix.keepClear(roomId)) spawned = true;
        }
    }
}
