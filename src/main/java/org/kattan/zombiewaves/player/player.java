package org.kattan.zombiewaves.player;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

public class player {
    public int numberOfKills = 0;
    private final Player playerObject;

    public player(Player pl){
        playerObject = pl;
    }

    public void playerKilledMonster(){
        numberOfKills++;
        playerObject.sendMessage(Text.of("Kills: " + numberOfKills));
    }
}
