package clearedrooms;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;

import zombie.ZomboidFileSystem;
import zombie.characters.IsoZombie;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.areas.IsoRoom;

/**
 * Zombies the population manager hands back when an area loads. A zombie is let into a room a player
 * has explored only if it was really standing in a room when its area was stored (chunk unload or world
 * save). Anything else got there while virtual: wandering, respawn or redistribution in the native
 * population manager, which knows nothing about the walls, doors and barricades of a player's base.
 *
 * The persistent ids of the zombies that were inside are kept in clearedrooms_inside.bin in the save.
 * A save without that file (older saves, new games) lets every zombie in until it has been saved once.
 * Public, because ZombieBuddy inlines the advice into the game's classes.
 */
public final class Indoor {
    private Indoor() {}

    private static final String FILE = "clearedrooms_inside.bin";
    private static final int VERSION = 1;

    private static final HashSet<Integer> inside = new HashSet<>();
    private static String loadedFor;
    private static boolean legacy;
    private static int dropped;

    private static synchronized void ensureLoaded() {
        String dir = ZomboidFileSystem.instance.getCurrentSaveDir();
        if (dir == null || dir.equals(loadedFor)) return;
        loadedFor = dir;
        inside.clear();
        dropped = 0;
        File f = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave(FILE));
        legacy = !f.isFile();
        if (legacy) {
            System.out.println("[ClearedRooms] no " + FILE + " yet: indoor zombies load as usual until the next save");
            return;
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            int version = in.readInt();
            if (version != VERSION) throw new IllegalStateException("unknown version " + version);
            int n = in.readInt();
            for (int i = 0; i < n; i++) inside.add(in.readInt());
            System.out.println("[ClearedRooms] " + n + " zombies were inside rooms when this save was stored");
        } catch (Throwable t) {
            legacy = true;
            System.out.println("[ClearedRooms] could not read " + FILE + ", indoor zombies load as usual: " + t);
        }
    }

    /** The world is closing (after its last save): read the list again for whichever world loads next. */
    public static synchronized void worldStopped() {
        loadedFor = null;
        inside.clear();
    }

    private static void record(IsoZombie z) {
        if (z == null || z.persistentId == 0 || z.isDead() || z.isReanimatedPlayer()) return;
        IsoGridSquare sq = z.getCurrentSquare();
        if (sq != null && sq.getRoom() != null) inside.add(z.persistentId);
    }

    /** Before IsoChunk zombies are handed to the population manager (the area unloads). */
    public static void chunkUnloading(IsoChunk chunk) {
        try {
            synchronized (Indoor.class) {
                ensureLoaded();
                for (int z = chunk.minLevel; z <= chunk.maxLevel; z++)
                    for (int y = 0; y < 8; y++)
                        for (int x = 0; x < 8; x++) {
                            IsoGridSquare sq = chunk.getGridSquare(x, y, z);
                            if (sq == null || sq.getRoom() == null) continue;
                            ArrayList<IsoMovingObject> mos = sq.getMovingObjects();
                            for (int i = 0; i < mos.size(); i++)
                                if (mos.get(i) instanceof IsoZombie zed) record(zed);
                        }
            }
        } catch (Throwable t) {
            System.out.println("[ClearedRooms] chunk record failed: " + t);
        }
    }

    /** Before one zombie is handed to the population manager. */
    public static void virtualizing(IsoZombie z) {
        try {
            synchronized (Indoor.class) {
                ensureLoaded();
                record(z);
            }
        } catch (Throwable t) {
            System.out.println("[ClearedRooms] zombie record failed: " + t);
        }
    }

    /** On world save (main thread): the real zombies standing in rooms are stored too. */
    public static void saving() {
        try {
            synchronized (Indoor.class) {
                ensureLoaded();
                IsoCell cell = IsoWorld.instance == null ? null : IsoWorld.instance.currentCell;
                if (cell != null) {
                    ArrayList<IsoZombie> zs = cell.getZombieList();
                    for (int i = 0; i < zs.size(); i++) record(zs.get(i));
                }
                File f = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave(FILE));
                File tmp = new File(f.getPath() + ".tmp");
                try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)))) {
                    out.writeInt(VERSION);
                    out.writeInt(inside.size());
                    for (int id : inside) out.writeInt(id);
                }
                Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (Throwable t) {
            System.out.println("[ClearedRooms] saving the indoor list failed: " + t);
        }
    }

    /**
     * Whether the population manager may put this zombie on (x, y, z). False only for a square in a room
     * a player has explored, when the zombie was not in a room when its area was stored.
     */
    public static boolean allow(float x, float y, float z, int persistentId) {
        try {
            IsoCell cell = IsoWorld.instance == null ? null : IsoWorld.instance.currentCell;
            if (cell == null) return true;
            IsoGridSquare sq = cell.getGridSquare((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            if (sq == null) return true;
            IsoRoom room = sq.getRoom();
            RoomDef def = room == null ? null : room.def;
            synchronized (Indoor.class) {
                ensureLoaded();
                boolean wasInside = persistentId != 0 && inside.remove(persistentId);
                if (def == null || !def.explored || legacy || wasInside) return true;
                if (dropped++ < 200) {
                    System.out.println("[ClearedRooms] kept zombie " + persistentId + " out of explored room "
                        + def.getName() + " at " + (int) x + "," + (int) y + "," + (int) z);
                }
                return false;
            }
        } catch (Throwable t) {
            System.out.println("[ClearedRooms] zombie check failed, vanilla behaviour kept: " + t);
            return true;
        }
    }
}
