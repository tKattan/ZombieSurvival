package org.kattan.zombiewaves.constants;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class Locations {
    public static World world = Sponge.getGame().getServer().getWorld("world").get();

    public static Vector3d ArenaVector = new Vector3d(390.0, 4.0, -288.0);
    public static Vector3d HubVector = new Vector3d(-556.0, 4.0, -903.0);
    public static Vector3d HubStartButtonVector = new Vector3d(-554.0, 5.0, -903.0);
    public static Vector3d PlayerChest_1Vector = new Vector3d(-555.0, 4.0, -933.0);
    public static Vector3d PlayerChest_RefillVector = new Vector3d(-553.0, 4.0, -933.0);

    public static Location<World> Arena = new Location<World>(world, ArenaVector);
    public static Location<World> Hub = new Location<World>(world, HubVector);
    public static Location<World> HubStartButton = new Location<World>(world, HubStartButtonVector);
    public static Location<World> PlayerChest_1 = new Location<>(world, PlayerChest_1Vector);
    public static Location<World> PlayerChest_Refill = new Location<>(world, PlayerChest_RefillVector);
}
