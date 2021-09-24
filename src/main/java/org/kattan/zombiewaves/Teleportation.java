package org.kattan.zombiewaves;

import org.kattan.zombiewaves.constants.Locations;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.function.Consumer;

public class Teleportation {

    private static final Consumer<Player> tpToArena = t -> t.setLocation(Locations.Arena);
    private static final Consumer<Player> tpToHub = t -> t.setLocation(Locations.Hub);

    public static void tpAllPlayersToArena(){
        Sponge.getServer().getOnlinePlayers().forEach(tpToArena);
    }

    public static void tpAllPlayersToHub(){
        Sponge.getServer().getOnlinePlayers().forEach(tpToHub);
    }

    public static void tpPlayerToHub(Player player){
        player.setLocation(Locations.Hub);
    }

    public static void tpPlayerToArena(Player player){
        player.setLocation(Locations.Arena);
    }
}
