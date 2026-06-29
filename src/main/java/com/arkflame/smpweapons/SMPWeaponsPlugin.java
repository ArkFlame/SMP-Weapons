package com.arkflame.smpweapons;

import com.arkflame.smpweapons.ability.AbilityEngine;
import com.arkflame.smpweapons.ability.AbilityItemProtectionService;
import com.arkflame.smpweapons.ability.CooldownService;
import com.arkflame.smpweapons.ability.FallProtectionService;
import com.arkflame.smpweapons.ability.GlideService;
import com.arkflame.smpweapons.ability.InventoryPassiveService;
import com.arkflame.smpweapons.ability.ShieldPassiveService;
import com.arkflame.smpweapons.block.TemporaryBlockService;
import com.arkflame.smpweapons.command.DynamicCommandRegistry;
import com.arkflame.smpweapons.command.SMPWeaponsCommand;
import com.arkflame.smpweapons.config.WeaponManager;
import com.arkflame.smpweapons.hook.SMPRegionsHook;
import com.arkflame.smpweapons.item.ItemIdentityService;
import com.arkflame.smpweapons.item.WeaponItemFactory;
import com.arkflame.smpweapons.listener.AbilityItemProtectionListener;
import com.arkflame.smpweapons.listener.MenuClickListener;
import com.arkflame.smpweapons.listener.DynamicCommandInterceptListener;
import com.arkflame.smpweapons.listener.GlideToggleListener;
import com.arkflame.smpweapons.listener.TotemPopListener;
import com.arkflame.smpweapons.listener.WeaponListener;
import com.arkflame.smpweapons.menu.MenuManager;
import com.arkflame.smpweapons.projectile.ProjectileService;
import com.arkflame.smpweapons.shape.ShapeEngine;
import com.arkflame.smpweapons.util.FoliaAPI;
import com.arkflame.smpweapons.util.TextBridge;
import com.arkflame.smpweapons.util.YamlUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SMPWeaponsPlugin extends JavaPlugin {
    private FoliaAPI schedulerBridge;
    private TextBridge text;
    private ItemIdentityService itemIdentityService;
    private WeaponItemFactory itemFactory;
    private WeaponManager weaponManager;
    private MenuManager menuManager;
    private CooldownService cooldownService;
    private FallProtectionService fallProtectionService;
    private GlideService glideService;
    private ShapeEngine shapeEngine;
    private TemporaryBlockService temporaryBlockService;
    private ProjectileService projectileService;
    private AbilityEngine abilityEngine;
    private AbilityItemProtectionService abilityItemProtectionService;
    private DynamicCommandRegistry dynamicCommandRegistry;
    private ShieldPassiveService shieldPassiveService;
    private InventoryPassiveService inventoryPassiveService;
    private SMPRegionsHook smpRegionsHook;

    @Override
    public void onEnable() {
        saveDefaultFiles();
        loadServices();
        registerCommands();
        registerListeners();
        getLogger().info("SMP Weapons " + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (this.dynamicCommandRegistry != null) {
            this.dynamicCommandRegistry.shutdown();
        }
        if (this.inventoryPassiveService != null) {
            this.inventoryPassiveService.stop();
        }
        if (this.abilityItemProtectionService != null) {
            this.abilityItemProtectionService.clearAll();
        }
        if (this.cooldownService != null) {
            this.cooldownService.resetAll();
        }
        if (this.projectileService != null) {
            this.projectileService.clear();
        }
        if (this.glideService != null) {
            this.glideService.clearAll();
        }
        if (this.temporaryBlockService != null) {
            this.temporaryBlockService.restoreAllNow();
        }
        if (this.schedulerBridge != null) {
            this.schedulerBridge.cancelAllTasks();
        }
        if (this.text != null) {
            this.text.close();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        if (this.temporaryBlockService != null && getConfig().getBoolean("real-blocks.restore-original-block", true)) {
            this.temporaryBlockService.restoreAllNow();
        }
        if (this.inventoryPassiveService != null) {
            this.inventoryPassiveService.stop();
        }
        if (this.projectileService != null) {
            this.projectileService.clear();
        }
        if (this.glideService != null) {
            this.glideService.clearAll();
        }
        if (this.schedulerBridge != null) {
            this.schedulerBridge.cancelAllTasks();
        }
        if (this.text != null) {
            this.text.close();
        }
        if (this.abilityItemProtectionService != null) {
            this.abilityItemProtectionService.clearAll();
        }
        loadServices();
    }

    private void loadServices() {
        this.schedulerBridge = new FoliaAPI(this);
        final YamlConfiguration messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        this.text = new TextBridge(this, YamlUtil.map(messages));
        this.smpRegionsHook = new SMPRegionsHook(this, getConfig().getConfigurationSection("hooks.smp-regions"));
        this.itemIdentityService = new ItemIdentityService(this);
        this.itemFactory = new WeaponItemFactory(this.text, this.itemIdentityService);
        this.weaponManager = new WeaponManager(this, this.itemIdentityService);
        this.weaponManager.load();
        final YamlConfiguration menus = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "menus.yml"));
        this.menuManager = new MenuManager(this.text, this.weaponManager, this.itemFactory);
        this.menuManager.load(menus);
        this.fallProtectionService = new FallProtectionService();
        this.glideService = new GlideService(this.schedulerBridge);
        this.shapeEngine = new ShapeEngine(getConfig().getInt("engine.max-shape-points", 4096));
        this.temporaryBlockService = new TemporaryBlockService(
                this,
                this.schedulerBridge,
                this.shapeEngine,
                getConfig().getInt("engine.max-block-changes-per-tick", 300),
                getConfig().getInt("engine.max-active-temporary-blocks-global", 5000),
                getConfig().getDouble("virtual-blocks.viewer-radius", 48.0D),
                getConfig().getBoolean("virtual-blocks.force-packet-only", true),
                getConfig().getString("virtual-blocks.cleanup-particle", "WHITE_SMOKE")
        );
        this.projectileService = new ProjectileService(this.schedulerBridge, this.temporaryBlockService, getConfig().getInt("engine.max-active-projectiles-per-player", 10), getConfig().getInt("engine.max-active-projectiles-global", 200), this.smpRegionsHook);
        this.cooldownService = new CooldownService(this.text, this.schedulerBridge);
        this.abilityItemProtectionService = new AbilityItemProtectionService(this);
        this.abilityEngine = new AbilityEngine(
                this,
                this.schedulerBridge,
                this.fallProtectionService,
                this.cooldownService,
                this.temporaryBlockService,
                this.projectileService,
                this.glideService,
                getConfig().getInt("engine.max-air-loop-ticks", getConfig().getInt("settings.max-air-loop-ticks", 120)),
                getConfig().getInt("engine.max-target-distance", getConfig().getInt("settings.max-target-distance", 32))
        );
        this.projectileService.setAbilityEngine(this.abilityEngine);
        this.shieldPassiveService = new ShieldPassiveService(this.weaponManager);
        this.inventoryPassiveService = new InventoryPassiveService(this);
        this.inventoryPassiveService.start();
        if (this.dynamicCommandRegistry == null) {
            this.dynamicCommandRegistry = new DynamicCommandRegistry(this);
        }
        this.dynamicCommandRegistry.reload();
    }

    private void registerCommands() {
        final SMPWeaponsCommand command = new SMPWeaponsCommand(this);
        final PluginCommand pluginCommand = getCommand("smpweapons");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
    }

    private void registerListeners() {
        final AbilityItemProtectionListener protectionListener = new AbilityItemProtectionListener(this);
        getServer().getPluginManager().registerEvents(protectionListener, this);
        getServer().getPluginManager().registerEvents(new WeaponListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuClickListener(this), this);
        getServer().getPluginManager().registerEvents(new DynamicCommandInterceptListener(this), this);
        registerAbilityItemSwapListenerIfAvailable(protectionListener);
        registerGlideToggleListenerIfAvailable();
        registerTotemPopListenerIfAvailable();
    }

    @SuppressWarnings("unchecked")
    private void registerGlideToggleListenerIfAvailable() {
        try {
            final Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName("org.bukkit.event.entity.EntityToggleGlideEvent").asSubclass(Event.class);
            final GlideToggleListener listener = new GlideToggleListener(this);
            getServer().getPluginManager().registerEvent(eventClass, listener, EventPriority.HIGHEST, new EventExecutor() {
                @Override
                public void execute(final org.bukkit.event.Listener ignored, final Event event) {
                    listener.handle(event);
                }
            }, this, false);
        } catch (final ClassNotFoundException ignored) {
            getLogger().info("Elytra glide event not available on this server; Rocket Spear will use best-effort gliding.");
        }
    }

    @SuppressWarnings("unchecked")
    private void registerAbilityItemSwapListenerIfAvailable(final AbilityItemProtectionListener listener) {
        try {
            final Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName("org.bukkit.event.player.PlayerSwapHandItemsEvent").asSubclass(Event.class);
            getServer().getPluginManager().registerEvent(eventClass, listener, EventPriority.HIGHEST, new EventExecutor() {
                @Override
                public void execute(final org.bukkit.event.Listener ignored, final Event event) {
                    listener.handleSwapHandItems(event);
                }
            }, this, false);
        } catch (final ClassNotFoundException ignored) {
            // 1.8 has no offhand swap event.
        }
    }

    @SuppressWarnings("unchecked")
    private void registerTotemPopListenerIfAvailable() {
        try {
            final Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName("org.bukkit.event.entity.EntityResurrectEvent").asSubclass(Event.class);
            final TotemPopListener listener = new TotemPopListener(this);
            getServer().getPluginManager().registerEvent(eventClass, listener, EventPriority.HIGHEST, new EventExecutor() {
                @Override
                public void execute(final org.bukkit.event.Listener ignored, final Event event) {
                    listener.handle(event);
                }
            }, this, false);
        } catch (final ClassNotFoundException ignored) {
            getLogger().info("Entity resurrect event not available on this server; Ultratotem pop activation requires a modern server.");
        }
    }

    private void saveDefaultFiles() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("menus.yml");
        saveResourceIfMissing("weapons/default-weapons.yml");
        saveResourceIfMissing("weapons/more-weapons.yml");
        saveResourceIfMissing("weapons/custom-weapons.yml");
        saveResourceIfMissing("weapons/examples.yml");
        saveResourceIfMissing("weapons/client-weapons.yml");
    }

    private void saveResourceIfMissing(final String path) {
        final File file = new File(getDataFolder(), path);
        if (!file.exists()) {
            saveResource(path, false);
        }
    }

    public FoliaAPI getSchedulerBridge() { return schedulerBridge; }
    public TextBridge getText() { return text; }
    public WeaponManager getWeaponManager() { return weaponManager; }
    public WeaponItemFactory getItemFactory() { return itemFactory; }
    public MenuManager getMenuManager() { return menuManager; }
    public CooldownService getCooldownService() { return cooldownService; }
    public AbilityEngine getAbilityEngine() { return abilityEngine; }
    public AbilityItemProtectionService getAbilityItemProtectionService() { return abilityItemProtectionService; }
    public FallProtectionService getFallProtectionService() { return fallProtectionService; }
    public GlideService getGlideService() { return glideService; }
    public TemporaryBlockService getTemporaryBlockService() { return temporaryBlockService; }
    public ProjectileService getProjectileService() { return projectileService; }
    public ShieldPassiveService getShieldPassiveService() { return shieldPassiveService; }
    public SMPRegionsHook getSMPRegionsHook() { return smpRegionsHook; }
}
