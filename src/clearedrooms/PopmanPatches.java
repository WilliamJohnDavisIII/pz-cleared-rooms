package clearedrooms;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.characters.IsoZombie;
import zombie.iso.IsoChunk;

/**
 * The room fill (RoomPatches) is not the only way zombies end up in a cleared house. When an area unloads
 * or the world is saved, its zombies go to the native population manager, which keeps simulating them
 * (wandering, respawn, redistribution) without the player's walls and barricades. When the area loads,
 * ZombiePopulationManager.addZombieStanding / addZombieMoving put them back wherever the population
 * manager says, checking only for a floor and the player's spawn point. Here they also skip any explored
 * room the zombie was not already standing in (see Indoor).
 */
public final class PopmanPatches {
    private PopmanPatches() {}

    @Patch(className = "zombie.popman.ZombiePopulationManager", methodName = "addZombieStanding")
    public static class AddStanding {
        @Patch.OnEnter(skipOn = true)
        public static boolean enter(@Patch.Argument(0) float x, @Patch.Argument(1) float y,
                                    @Patch.Argument(2) float z, @Patch.Argument(6) int persistentId) {
            return !Indoor.allow(x, y, z, persistentId);
        }
    }

    @Patch(className = "zombie.popman.ZombiePopulationManager", methodName = "addZombieMoving")
    public static class AddMoving {
        @Patch.OnEnter(skipOn = true)
        public static boolean enter(@Patch.Argument(0) float x, @Patch.Argument(1) float y,
                                    @Patch.Argument(2) float z, @Patch.Argument(8) int persistentId) {
            return !Indoor.allow(x, y, z, persistentId);
        }
    }

    @Patch(className = "zombie.popman.ZombiePopulationManager", methodName = "removeChunkFromWorld")
    public static class ChunkUnload {
        @Patch.OnEnter
        public static void enter(@Patch.Argument(0) IsoChunk chunk) {
            Indoor.chunkUnloading(chunk);
        }
    }

    @Patch(className = "zombie.popman.ZombiePopulationManager", methodName = "virtualizeZombie")
    public static class Virtualize {
        @Patch.OnEnter
        public static void enter(@Patch.Argument(0) IsoZombie zombie) {
            Indoor.virtualizing(zombie);
        }
    }

    @Patch(className = "zombie.popman.ZombiePopulationManager", methodName = "stop")
    public static class WorldStop {
        @Patch.OnEnter
        public static void enter() {
            Indoor.worldStopped();
        }
    }

    @Patch(className = "zombie.MapCollisionData", methodName = "save")
    public static class WorldSave {
        @Patch.OnEnter
        public static void enter() {
            Indoor.saving();
        }
    }
}
