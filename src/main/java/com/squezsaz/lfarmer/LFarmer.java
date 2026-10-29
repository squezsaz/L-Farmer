package com.squezsaz.lfarmer;

import com.squezsaz.lfarmer.command.FarmerCommand;
import com.squezsaz.lfarmer.command.FarmerTabCompleter;
import com.squezsaz.lfarmer.database.DatabaseManager;
import com.squezsaz.lfarmer.gui.GUIManager;
import com.squezsaz.lfarmer.hook.HookManager;
import com.squezsaz.lfarmer.listener.ChunkLifecycleListener;
import com.squezsaz.lfarmer.listener.FarmerPlacementListener;
import com.squezsaz.lfarmer.listener.GUIListener;
import com.squezsaz.lfarmer.locale.LanguageManager;
import com.squezsaz.lfarmer.model.Farmer;
import com.squezsaz.lfarmer.npc.FarmerNPCManager;
import com.squezsaz.lfarmer.scheduler.UnifiedScheduler;
import com.squezsaz.lfarmer.service.AutoSellService;
import com.squezsaz.lfarmer.service.FarmerManager;
import com.squezsaz.lfarmer.service.FuelService;
import com.squezsaz.lfarmer.service.GroundCollectService;
import com.squezsaz.lfarmer.service.HarvestService;
import com.squezsaz.lfarmer.service.LogService;
import com.squezsaz.lfarmer.service.MobCollectService;
import com.squezsaz.lfarmer.service.StatsService;
import com.squezsaz.lfarmer.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public class LFarmer extends JavaPlugin {

    private static LFarmer instance;

    private UnifiedScheduler scheduler;
    private DatabaseManager databaseManager;
    private LanguageManager languageManager;
    private HookManager hookManager;
    private FarmerManager farmerManager;
    private FarmerNPCManager npcManager;
    private GUIManager guiManager;
    private HarvestService harvestService;
    private MobCollectService mobCollectService;
    private GroundCollectService groundCollectService;
    private AutoSellService autoSellService;
    private FuelService fuelService;
    private StatsService statsService;
    private LogService logService;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResourceIfNotExists("prices.yml");

        this.scheduler = new UnifiedScheduler(this);
        this.languageManager = new LanguageManager(this);
        this.languageManager.load(getConfig().getString("language", "tr"));

        this.databaseManager = new DatabaseManager(this, scheduler);
        this.databaseManager.init(getConfig());

        this.farmerManager = new FarmerManager(this);
        this.farmerManager.loadLevels(getConfig());

        this.fuelService = new FuelService(this);
        this.fuelService.loadFuelConfig();

        this.hookManager = new HookManager(this);
        this.hookManager.init();

        this.npcManager = new FarmerNPCManager(this);
        this.guiManager = new GUIManager(this);

        List<Farmer> loaded = databaseManager.loadAllFarmers();
        for (Farmer farmer : loaded) {
            farmerManager.registerFarmer(farmer);
        }

        this.harvestService = new HarvestService(this);
        this.harvestService.start();

        this.mobCollectService = new MobCollectService(this);

        this.groundCollectService = new GroundCollectService(this);
        this.groundCollectService.start();

        this.autoSellService = new AutoSellService(this);
        this.autoSellService.start();

        this.statsService = new StatsService(this);
        this.scheduler.runAsyncRepeating(() -> statsService.flushDirty(), 60L, 60L);

        this.logService = new LogService(this);

        registerListeners();
        registerCommands();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                npcManager.onChunkLoad(chunk);
            }
        }

        Bukkit.getConsoleSender().sendMessage(ColorUtil.parse("<gold><bold>[L-Farmer] Eklenti başarıyla aktif edildi! Yazar: squezsaz</bold></gold>"));
        if (scheduler.isFolia()) {
            Bukkit.getConsoleSender().sendMessage(ColorUtil.parse("<green>[L-Farmer] Folia sunucu yazılımı tespit edildi ve bölgesel iş parçacıkları aktif edildi!</green>"));
        }
    }

    @Override
    public void onDisable() {
        if (harvestService != null) {
            harvestService.stop();
        }
        if (groundCollectService != null) {
            groundCollectService.stop();
        }
        if (autoSellService != null) {
            autoSellService.stop();
        }
        if (npcManager != null) {
            npcManager.despawnAll();
        }
        if (statsService != null) {
            statsService.flushDirty();
        }
        if (databaseManager != null && farmerManager != null) {
            databaseManager.saveAll(farmerManager.getAllFarmers().values(), false);
            databaseManager.close();
        }

        Bukkit.getConsoleSender().sendMessage(ColorUtil.parse("<red>[L-Farmer] Eklenti devre dışı bırakıldı!</red>"));
    }

    public void reloadPlugin() {
        reloadConfig();
        saveResourceIfNotExists("prices.yml");

        languageManager.load(getConfig().getString("language", "tr"));
        farmerManager.loadLevels(getConfig());
        fuelService.loadFuelConfig();
        hookManager.getConfigPriceProvider().load();

        if (harvestService != null) {
            harvestService.start();
        }
        if (groundCollectService != null) {
            groundCollectService.start();
        }
        if (autoSellService != null) {
            autoSellService.start();
        }

        for (Farmer farmer : farmerManager.getAllFarmers().values()) {
            npcManager.updateHologram(farmer.getId());
        }
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new GUIListener(), this);
        pm.registerEvents(new FarmerPlacementListener(this), this);
        pm.registerEvents(new ChunkLifecycleListener(this), this);
        pm.registerEvents(mobCollectService, this);
        pm.registerEvents(groundCollectService, this);
    }

    private void registerCommands() {
        FarmerCommand cmd = new FarmerCommand(this);
        FarmerTabCompleter tab = new FarmerTabCompleter();

        registerCommand("farmer", cmd, tab);
        registerCommand("ciftci", cmd, tab);
        registerCommand("çiftçi", cmd, tab);
        registerCommand("lfarmer", cmd, tab);
    }

    private void registerCommand(String name, FarmerCommand cmd, FarmerTabCompleter tab) {
        PluginCommand pc = getCommand(name);
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(tab);
        }
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            saveResource(resourcePath, false);
        }
    }

    public static LFarmer getInstance() {
        return instance;
    }

    public UnifiedScheduler getScheduler() {
        return scheduler;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public FarmerManager getFarmerManager() {
        return farmerManager;
    }

    public FarmerNPCManager getNpcManager() {
        return npcManager;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public HarvestService getHarvestService() {
        return harvestService;
    }

    public MobCollectService getMobCollectService() {
        return mobCollectService;
    }

    public GroundCollectService getGroundCollectService() {
        return groundCollectService;
    }

    public AutoSellService getAutoSellService() {
        return autoSellService;
    }

    public FuelService getFuelService() {
        return fuelService;
    }

    public StatsService getStatsService() {
        return statsService;
    }

    public LogService getLogService() {
        return logService;
    }
}
