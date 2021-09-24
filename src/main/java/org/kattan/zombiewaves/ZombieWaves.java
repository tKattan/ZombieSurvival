package org.kattan.zombiewaves;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import org.kattan.zombiewaves.constants.Locations;
import org.kattan.zombiewaves.player.player;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.TileEntityTypes;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.kattan.zombiewaves.Teleportation.tpAllPlayersToArena;
import static org.kattan.zombiewaves.Teleportation.tpPlayerToHub;
import static org.kattan.zombiewaves.constants.Locations.HubStartButton;
import static org.spongepowered.api.block.BlockTypes.WOODEN_BUTTON;


@Plugin(
        id = "zombiewaves",
        name = "ZombieWaves",
        description = "Zombie wave killing game",
        authors = {
                "tkattan"
        }
)
public class ZombieWaves {
    @Inject
    private Logger logger;
    @Inject
    private Game game;

    final private long timeofday = 1000;

    Task.Builder taskBuilder = Task.builder();

    HashMap<String, player> playerDic = new HashMap<>();

    private boolean gameStarting = false;
    private boolean gameInProgress = false;

    public void sendMessagesToAllPlayers(String message) {
        game.getServer().getOnlinePlayers().forEach(player -> player.sendMessage(Text.of(message)));
    }

    //region Server Start
    @Listener
    public void onServerIniitialization(GameInitializationEvent event) {
        gameStarting = false;
        gameInProgress = false;

        CommandSpec myCommandSpec = CommandSpec.builder()
                .description(Text.of("Stop Command"))
                .executor((CommandSource src, CommandContext args) -> {
                    game.getServer().getOnlinePlayers().forEach(player -> player.sendMessage(Text.of("Stopping game in 5 sec!")));
                    taskBuilder.execute(
                            () -> {
                                game.getServer().getOnlinePlayers().forEach(this::initPlayer);
                                gameInProgress = false;
                                //resetArena
                            }
                    ).delay(5000, TimeUnit.MILLISECONDS).submit(this);
                    return CommandResult.success();
                })
                .build();
        Sponge.getCommandManager().register(this, myCommandSpec, "stopgame", "sg");
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        logger.info("Server has started.");

        // TODO: forceload chunks

        //game.getEventManager().registerListeners(this, new HubListener());

        initWorld();
    }

    private void initWorld() {
        // TODO: stop mob spawning
        game.getServer().getWorld("world").get().getProperties().setRaining(false);
        // TODO: kill existing mobs
        // TODO: set time to night permanently
        game.getServer().getWorld("world").get().getProperties().setWorldTime(timeofday);
        taskBuilder.execute(
                () -> {game.getServer().getWorld("world").get().getProperties().setWorldTime(timeofday);
                game.getServer().getWorld("world").get().getProperties().setRaining(false);}
        ).delay(3, TimeUnit.MINUTES).interval(3, TimeUnit.MINUTES).submit(this);
        // TODO: others...
    }
    //endregion

    //region Player management
    @Listener
    public void onJoin(ClientConnectionEvent.Join event) {
        Player player = event.getTargetEntity();
        if (!playerDic.containsKey(player.getName()))
            playerDic.put(player.getName(), new player(player));
        initPlayer(player);
    }

    @Listener
    public void onDisconnect(ClientConnectionEvent.Disconnect event){
        Player player = event.getTargetEntity();
        if (playerDic.containsKey(player.getName()) && player.getLocation().getPosition().getX() < -200){
            playerDic.remove(player.getName());
        }
    }

    public void initPlayer(Player player) {
        tpPlayerToHub(player);
        player.gameMode().set(GameModes.ADVENTURE);
        if (!gameInProgress) {
            player.getInventory().clear();
            //TODO: make this work
            player.health().set(20.);
            player.foodLevel().set(20);
        }
        if (player.gameMode().get() != GameModes.ADVENTURE)
            player.sendMessage(Text.of("Error changing gamemode to Adventure. Please contact admin."));
    }
    //endregion

    //region Hub
    @Listener
    public void onButtonPress(InteractBlockEvent event) {
        if (event.getTargetBlock().getLocation().isPresent()) {
            if (event.getTargetBlock().getState().getType() == WOODEN_BUTTON) {
                if (Objects.equals(event.getTargetBlock().getLocation().get(), HubStartButton)) {
                    if (!gameStarting && !gameInProgress) {
                        game.getServer().getOnlinePlayers().forEach(player -> player.sendMessage(Text.of("Game will start in 5 seconds!")));
                        gameStarting = true;

                        initGame();
                        taskBuilder.execute(() -> {
                            logger.info("Game started");
                            startGame();
                        }
                        ).delay(5000, TimeUnit.MILLISECONDS).submit(this);
                    }
                }
            }
        }
    }
    //endregion

    //region Arena
    private void initGame() {
        // TODO: give players items (basic spartan weapon, basic gun, magazine, ammo, wallet)
        givePlayerItems();

        // TODO: change mob config

        // TODO: spawn initial mobs (move from startGame to here when chunkloading is done)

    }

    private void givePlayerItems() {
        if (Locations.PlayerChest_1.getTileEntity().isPresent() && Locations.PlayerChest_Refill.getTileEntity().isPresent()) {
            if (Locations.PlayerChest_1.getTileEntity().get().getType().equals(TileEntityTypes.CHEST) &&
                    Locations.PlayerChest_Refill.getTileEntity().get().getType().equals(TileEntityTypes.CHEST)) {
                Chest chest_player1 = (Chest) Locations.PlayerChest_1.getTileEntity().get();
                int inventorySize = chest_player1.getInventory().size();
                ArrayList<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < inventorySize; i++) {
                    if (chest_player1.getInventory().peek().isPresent())
                        game.getServer().getOnlinePlayers().forEach(player -> player.getInventory().offer(chest_player1.getInventory().peek().get()));
                        items.add(chest_player1.getInventory().poll().get());
                }
                for (int i = 0; i < inventorySize; i++) {
                    chest_player1.getInventory().offer(items.get(i));
                }
            }
        }
    }

    private final ArrayList<UUID> arenaZombies = new ArrayList<>();

    private void spawnZombie(Location<World> spawnLocation) {
        World world = spawnLocation.getExtent();

        Entity zombie = world.createEntity(EntityTypes.ZOMBIE, spawnLocation.getPosition());

        // We need to push a new cause StackFrame to the stack so we can add our own causes
        // In previous versions of the API you had to submit a Cause parameter
        // that would often not contain the real root cause
        // By default the current plugin's PluginContainer is already pushed to the stack.
        try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.PLUGIN);
            world.spawnEntity(zombie);
            arenaZombies.add(zombie.getUniqueId());
        }
    }

    private void startGame() {
        gameStarting = false;
        gameInProgress = true;
        tpAllPlayersToArena();

        taskBuilder.execute(
                () -> {
                    ArrayList<Location<World>> zombieLocations = new ArrayList<>();
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(412, 4, -290)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(423, 4, -273)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(412, 4, -262)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(413, 14, -265)));

                    for (Location<World> zombieLocation : zombieLocations) {
                        spawnZombie(zombieLocation);
                    }
                }
        ).delay(2000, TimeUnit.MILLISECONDS).submit(this);

        // TODO: start timer/wave loop
    }

    @Listener
    public void onZombieKilled(DestructEntityEvent event) {
        if (event.getTargetEntity().getType() == EntityTypes.ZOMBIE) {
            logger.info("Zombie killed");
            Optional<Player> killer = event.getCause().first(Player.class);
            killer.ifPresent(player -> playerDic.get(player.getName()).playerKilledMonster());
            if (arenaZombies.contains(event.getTargetEntity().getUniqueId())) {
                arenaZombies.remove(event.getTargetEntity().getUniqueId());
                if (arenaZombies.isEmpty())
                    allZombiesKilled();
            }
        }
    }

    private void allZombiesKilled() {
        taskBuilder.execute(
                () -> {
                    ArrayList<Location<World>> zombieLocations = new ArrayList<>();
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(369, 4, -243)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(447, 4, -263)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(423, 4, -406)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(423, 4, -406)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(423, 4, -406)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(463, 4, -384)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(339, 4, -278)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(450, 4, -315)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(468, 4, -261)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(438, 4, -328)));
                    zombieLocations.add(new Location<>(Locations.world, new Vector3d(448, 4, -346)));

                    for (Location<World> zombieLocation : zombieLocations) {
                        spawnZombie(zombieLocation);
                    }
                }
        ).delay(15000, TimeUnit.MILLISECONDS).submit(this);
        sendMessagesToAllPlayers("Second Wave coming in 15 secs!");
    }

    //endregion
}
