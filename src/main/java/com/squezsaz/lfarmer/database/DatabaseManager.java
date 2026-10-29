package com.squezsaz.lfarmer.database;

import com.squezsaz.lfarmer.model.Farmer;
import com.squezsaz.lfarmer.model.FarmerLogEntry;
import com.squezsaz.lfarmer.model.FarmerPermission;
import com.squezsaz.lfarmer.model.PlayerStats;
import com.squezsaz.lfarmer.scheduler.UnifiedScheduler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {

    private final Plugin plugin;
    private final UnifiedScheduler scheduler;
    private HikariDataSource dataSource;
    private boolean isMySql;

    public DatabaseManager(Plugin plugin, UnifiedScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    public void init(FileConfiguration config) {
        String type = config.getString("database.type", "SQLITE").toUpperCase();
        this.isMySql = type.equals("MYSQL") || type.equals("MARIADB");

        HikariConfig hikariConfig = new HikariConfig();

        if (isMySql) {
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String database = config.getString("database.mysql.database", "lfarmer");
            String username = config.getString("database.mysql.username", "root");
            String password = config.getString("database.mysql.password", "");
            boolean ssl = config.getBoolean("database.mysql.ssl", false);

            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl + "&characterEncoding=utf8");
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikariConfig.setMaximumPoolSize(config.getInt("database.pool.maximum_pool_size", 10));
            hikariConfig.setMinimumIdle(config.getInt("database.pool.minimum_idle", 2));
        } else {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "database.db");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            hikariConfig.setMaximumPoolSize(1);
            hikariConfig.setConnectionTestQuery("SELECT 1");
        }

        hikariConfig.setPoolName("LFarmerPool");
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(hikariConfig);
        createTables();
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lfarmer_farmers (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "server_name VARCHAR(64) NOT NULL DEFAULT 'local', " +
                    "world VARCHAR(64) NOT NULL, " +
                    "chunk_x INT NOT NULL, " +
                    "chunk_z INT NOT NULL, " +
                    "loc_x DOUBLE NOT NULL, " +
                    "loc_y DOUBLE NOT NULL, " +
                    "loc_z DOUBLE NOT NULL, " +
                    "owner_uuid VARCHAR(36) NOT NULL, " +
                    "owner_name VARCHAR(32) NOT NULL, " +
                    "level_capacity INT NOT NULL, " +
                    "level_range INT NOT NULL, " +
                    "level_speed INT NOT NULL, " +
                    "level_fortune INT NOT NULL, " +
                    "level_tax INT NOT NULL, " +
                    "fuel_amount DOUBLE NOT NULL, " +
                    "auto_sell BOOLEAN NOT NULL, " +
                    "auto_sell_interval INT NOT NULL DEFAULT 60, " +
                    "auto_replant BOOLEAN NOT NULL, " +
                    "mob_collect BOOLEAN NOT NULL, " +
                    "crop_collect BOOLEAN NOT NULL, " +
                    "ground_collect BOOLEAN NOT NULL DEFAULT 1, " +
                    "total_earned DOUBLE NOT NULL, " +
                    "total_collected BIGINT NOT NULL" +
                    ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lfarmer_storage (" +
                    "farmer_id VARCHAR(36) NOT NULL, " +
                    "material VARCHAR(64) NOT NULL, " +
                    "amount BIGINT NOT NULL, " +
                    "PRIMARY KEY (farmer_id, material)" +
                    ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lfarmer_members (" +
                    "farmer_id VARCHAR(36) NOT NULL, " +
                    "member_uuid VARCHAR(36) NOT NULL, " +
                    "can_open BOOLEAN NOT NULL, " +
                    "can_withdraw BOOLEAN NOT NULL, " +
                    "can_sell BOOLEAN NOT NULL, " +
                    "can_upgrade BOOLEAN NOT NULL, " +
                    "PRIMARY KEY (farmer_id, member_uuid)" +
                    ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lfarmer_disabled_items (" +
                    "farmer_id VARCHAR(36) NOT NULL, " +
                    "material VARCHAR(64) NOT NULL, " +
                    "PRIMARY KEY (farmer_id, material)" +
                    ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lfarmer_player_stats (" +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "stat_date VARCHAR(10) NOT NULL, " +
                    "crops_harvested BIGINT NOT NULL DEFAULT 0, " +
                    "mobs_collected BIGINT NOT NULL DEFAULT 0, " +
                    "items_sold BIGINT NOT NULL DEFAULT 0, " +
                    "money_earned DOUBLE NOT NULL DEFAULT 0, " +
                    "fuel_used DOUBLE NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (player_uuid, stat_date)" +
                    ")");

            if (isMySql) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lfarmer_logs (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "timestamp BIGINT NOT NULL, " +
                        "date_str VARCHAR(10) NOT NULL, " +
                        "player_uuid VARCHAR(36) NOT NULL, " +
                        "player_name VARCHAR(32) NOT NULL, " +
                        "actor_name VARCHAR(32) NOT NULL, " +
                        "action VARCHAR(32) NOT NULL, " +
                        "details VARCHAR(255) NOT NULL, " +
                        "server_name VARCHAR(64) NOT NULL, " +
                        "location_str VARCHAR(64) NOT NULL" +
                        ")");
            } else {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lfarmer_logs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "timestamp BIGINT NOT NULL, " +
                        "date_str VARCHAR(10) NOT NULL, " +
                        "player_uuid VARCHAR(36) NOT NULL, " +
                        "player_name VARCHAR(32) NOT NULL, " +
                        "actor_name VARCHAR(32) NOT NULL, " +
                        "action VARCHAR(32) NOT NULL, " +
                        "details VARCHAR(255) NOT NULL, " +
                        "server_name VARCHAR(64) NOT NULL, " +
                        "location_str VARCHAR(64) NOT NULL" +
                        ")");
            }

            try {
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_lfarmer_logs_uuid ON lfarmer_logs(player_uuid)");
            } catch (SQLException ignored) {
            }
            try {
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_lfarmer_logs_time ON lfarmer_logs(timestamp)");
            } catch (SQLException ignored) {
            }

            try {
                stmt.executeUpdate("ALTER TABLE lfarmer_farmers ADD COLUMN server_name VARCHAR(64) NOT NULL DEFAULT 'local'");
            } catch (SQLException ignored) {
            }
            try {
                stmt.executeUpdate("ALTER TABLE lfarmer_farmers ADD COLUMN auto_sell_interval INT NOT NULL DEFAULT 60");
            } catch (SQLException ignored) {
            }
            try {
                stmt.executeUpdate("ALTER TABLE lfarmer_farmers ADD COLUMN ground_collect BOOLEAN NOT NULL DEFAULT 1");
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not initialize database tables: " + e.getMessage());
        }
    }

    public List<Farmer> loadAllFarmers() {
        List<Farmer> list = new ArrayList<>();
        String selectFarmers = "SELECT * FROM lfarmer_farmers";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectFarmers);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                String worldName = rs.getString("world");
                int chunkX = rs.getInt("chunk_x");
                int chunkZ = rs.getInt("chunk_z");
                double locX = rs.getDouble("loc_x");
                double locY = rs.getDouble("loc_y");
                double locZ = rs.getDouble("loc_z");
                UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                String ownerName = rs.getString("owner_name");

                World world = Bukkit.getWorld(worldName);
                Location loc = (world != null) ? new Location(world, locX, locY, locZ) : null;

                Farmer farmer = new Farmer(id, worldName, chunkX, chunkZ, loc, ownerUuid, ownerName);
                try {
                    farmer.setServerName(rs.getString("server_name"));
                } catch (SQLException ignored) {
                }
                farmer.setCapacityLevel(rs.getInt("level_capacity"));
                farmer.setRangeLevel(rs.getInt("level_range"));
                farmer.setSpeedLevel(rs.getInt("level_speed"));
                farmer.setFortuneLevel(rs.getInt("level_fortune"));
                farmer.setTaxLevel(rs.getInt("level_tax"));
                farmer.setFuelAmount(rs.getDouble("fuel_amount"));
                farmer.setAutoSell(rs.getBoolean("auto_sell"));
                try {
                    int interval = rs.getInt("auto_sell_interval");
                    if (interval > 0) {
                        farmer.setAutoSellInterval(interval);
                    }
                } catch (SQLException ignored) {
                }
                farmer.setAutoReplant(rs.getBoolean("auto_replant"));
                farmer.setMobCollect(rs.getBoolean("mob_collect"));
                farmer.setCropCollect(rs.getBoolean("crop_collect"));
                try {
                    farmer.setGroundCollect(rs.getBoolean("ground_collect"));
                } catch (SQLException ignored) {
                    farmer.setGroundCollect(true);
                }
                farmer.setTotalEarned(rs.getDouble("total_earned"));
                farmer.setTotalCollected(rs.getLong("total_collected"));

                loadStorage(conn, farmer);
                loadMembers(conn, farmer);
                loadDisabledItems(conn, farmer);

                farmer.setDirty(false);
                list.add(farmer);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load farmers: " + e.getMessage());
        }

        return list;
    }

    private void loadStorage(Connection conn, Farmer farmer) throws SQLException {
        String query = "SELECT material, amount FROM lfarmer_storage WHERE farmer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, farmer.getId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String matName = rs.getString("material");
                    long amount = rs.getLong("amount");
                    Material mat = Material.matchMaterial(matName);
                    if (mat != null && amount > 0) {
                        farmer.getStorage().put(mat, amount);
                    }
                }
            }
        }
    }

    private void loadMembers(Connection conn, Farmer farmer) throws SQLException {
        String query = "SELECT member_uuid, can_open, can_withdraw, can_sell, can_upgrade FROM lfarmer_members WHERE farmer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, farmer.getId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID memberUuid = UUID.fromString(rs.getString("member_uuid"));
                    FarmerPermission perm = new FarmerPermission(
                            rs.getBoolean("can_open"),
                            rs.getBoolean("can_withdraw"),
                            rs.getBoolean("can_sell"),
                            rs.getBoolean("can_upgrade")
                    );
                    farmer.getMembers().put(memberUuid, perm);
                }
            }
        }
    }

    private void loadDisabledItems(Connection conn, Farmer farmer) throws SQLException {
        String query = "SELECT material FROM lfarmer_disabled_items WHERE farmer_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, farmer.getId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String matName = rs.getString("material");
                    Material mat = Material.matchMaterial(matName);
                    if (mat != null) {
                        farmer.getDisabledItems().add(mat);
                    }
                }
            }
        }
    }

    public void saveFarmer(Farmer farmer, boolean async) {
        if (async) {
            scheduler.runAsync(() -> saveFarmerInternal(farmer));
        } else {
            saveFarmerInternal(farmer);
        }
    }

    private synchronized void saveFarmerInternal(Farmer farmer) {
        if (!farmer.isDirty()) {
            return;
        }

        String upsertFarmer;
        if (isMySql) {
            upsertFarmer = "INSERT INTO lfarmer_farmers (id, server_name, world, chunk_x, chunk_z, loc_x, loc_y, loc_z, owner_uuid, owner_name, " +
                    "level_capacity, level_range, level_speed, level_fortune, level_tax, fuel_amount, auto_sell, auto_sell_interval, auto_replant, " +
                    "mob_collect, crop_collect, ground_collect, total_earned, total_collected) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE server_name=VALUES(server_name), loc_x=VALUES(loc_x), loc_y=VALUES(loc_y), loc_z=VALUES(loc_z), " +
                    "owner_uuid=VALUES(owner_uuid), owner_name=VALUES(owner_name), level_capacity=VALUES(level_capacity), " +
                    "level_range=VALUES(level_range), level_speed=VALUES(level_speed), level_fortune=VALUES(level_fortune), " +
                    "level_tax=VALUES(level_tax), fuel_amount=VALUES(fuel_amount), auto_sell=VALUES(auto_sell), auto_sell_interval=VALUES(auto_sell_interval), " +
                    "auto_replant=VALUES(auto_replant), mob_collect=VALUES(mob_collect), crop_collect=VALUES(crop_collect), ground_collect=VALUES(ground_collect), " +
                    "total_earned=VALUES(total_earned), total_collected=VALUES(total_collected)";
        } else {
            upsertFarmer = "INSERT OR REPLACE INTO lfarmer_farmers (id, server_name, world, chunk_x, chunk_z, loc_x, loc_y, loc_z, owner_uuid, owner_name, " +
                    "level_capacity, level_range, level_speed, level_fortune, level_tax, fuel_amount, auto_sell, auto_sell_interval, auto_replant, " +
                    "mob_collect, crop_collect, ground_collect, total_earned, total_collected) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(upsertFarmer)) {
                ps.setString(1, farmer.getId().toString());
                ps.setString(2, farmer.getServerName() != null ? farmer.getServerName() : "local");
                ps.setString(3, farmer.getWorld());
                ps.setInt(4, farmer.getChunkX());
                ps.setInt(5, farmer.getChunkZ());
                Location loc = farmer.getLocation();
                ps.setDouble(6, loc != null ? loc.getX() : 0.0);
                ps.setDouble(7, loc != null ? loc.getY() : 0.0);
                ps.setDouble(8, loc != null ? loc.getZ() : 0.0);
                ps.setString(9, farmer.getOwnerUuid().toString());
                ps.setString(10, farmer.getOwnerName());
                ps.setInt(11, farmer.getCapacityLevel());
                ps.setInt(12, farmer.getRangeLevel());
                ps.setInt(13, farmer.getSpeedLevel());
                ps.setInt(14, farmer.getFortuneLevel());
                ps.setInt(15, farmer.getTaxLevel());
                ps.setDouble(16, farmer.getFuelAmount());
                ps.setBoolean(17, farmer.isAutoSell());
                ps.setInt(18, farmer.getAutoSellInterval());
                ps.setBoolean(19, farmer.isAutoReplant());
                ps.setBoolean(20, farmer.isMobCollect());
                ps.setBoolean(21, farmer.isCropCollect());
                ps.setBoolean(22, farmer.isGroundCollect());
                ps.setDouble(23, farmer.getTotalEarned());
                ps.setLong(24, farmer.getTotalCollected());
                ps.executeUpdate();
            }

            try (PreparedStatement psDel = conn.prepareStatement("DELETE FROM lfarmer_storage WHERE farmer_id = ?")) {
                psDel.setString(1, farmer.getId().toString());
                psDel.executeUpdate();
            }

            String insertStorage = "INSERT INTO lfarmer_storage (farmer_id, material, amount) VALUES (?, ?, ?)";
            try (PreparedStatement psIns = conn.prepareStatement(insertStorage)) {
                for (Map.Entry<Material, Long> entry : farmer.getStorage().entrySet()) {
                    if (entry.getValue() > 0) {
                        psIns.setString(1, farmer.getId().toString());
                        psIns.setString(2, entry.getKey().name());
                        psIns.setLong(3, entry.getValue());
                        psIns.addBatch();
                    }
                }
                psIns.executeBatch();
            }

            try (PreparedStatement psDelMem = conn.prepareStatement("DELETE FROM lfarmer_members WHERE farmer_id = ?")) {
                psDelMem.setString(1, farmer.getId().toString());
                psDelMem.executeUpdate();
            }

            String insertMembers = "INSERT INTO lfarmer_members (farmer_id, member_uuid, can_open, can_withdraw, can_sell, can_upgrade) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psInsMem = conn.prepareStatement(insertMembers)) {
                for (Map.Entry<UUID, FarmerPermission> entry : farmer.getMembers().entrySet()) {
                    psInsMem.setString(1, farmer.getId().toString());
                    psInsMem.setString(2, entry.getKey().toString());
                    psInsMem.setBoolean(3, entry.getValue().canOpen());
                    psInsMem.setBoolean(4, entry.getValue().canWithdraw());
                    psInsMem.setBoolean(5, entry.getValue().canSell());
                    psInsMem.setBoolean(6, entry.getValue().canUpgrade());
                    psInsMem.addBatch();
                }
                psInsMem.executeBatch();
            }

            try (PreparedStatement psDelDis = conn.prepareStatement("DELETE FROM lfarmer_disabled_items WHERE farmer_id = ?")) {
                psDelDis.setString(1, farmer.getId().toString());
                psDelDis.executeUpdate();
            }

            String insertDisabled = "INSERT INTO lfarmer_disabled_items (farmer_id, material) VALUES (?, ?)";
            try (PreparedStatement psInsDis = conn.prepareStatement(insertDisabled)) {
                for (Material mat : farmer.getDisabledItems()) {
                    psInsDis.setString(1, farmer.getId().toString());
                    psInsDis.setString(2, mat.name());
                    psInsDis.addBatch();
                }
                psInsDis.executeBatch();
            }

            conn.commit();
            farmer.setDirty(false);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save farmer " + farmer.getId() + ": " + e.getMessage());
        }
    }

    public void saveAll(Collection<Farmer> farmers, boolean async) {
        if (async) {
            scheduler.runAsync(() -> {
                for (Farmer farmer : farmers) {
                    saveFarmerInternal(farmer);
                }
            });
        } else {
            for (Farmer farmer : farmers) {
                saveFarmerInternal(farmer);
            }
        }
    }

    public void deleteFarmer(UUID id, boolean async) {
        Runnable runnable = () -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lfarmer_farmers WHERE id = ?")) {
                    ps.setString(1, id.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lfarmer_storage WHERE farmer_id = ?")) {
                    ps.setString(1, id.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lfarmer_members WHERE farmer_id = ?")) {
                    ps.setString(1, id.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lfarmer_disabled_items WHERE farmer_id = ?")) {
                    ps.setString(1, id.toString());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete farmer " + id + ": " + e.getMessage());
            }
        };

        if (async) {
            scheduler.runAsync(runnable);
        } else {
            runnable.run();
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Farmer loadFarmer(UUID id) {
        String select = "SELECT * FROM lfarmer_farmers WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String worldName = rs.getString("world");
                    int chunkX = rs.getInt("chunk_x");
                    int chunkZ = rs.getInt("chunk_z");
                    double locX = rs.getDouble("loc_x");
                    double locY = rs.getDouble("loc_y");
                    double locZ = rs.getDouble("loc_z");
                    UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
                    String ownerName = rs.getString("owner_name");

                    World world = Bukkit.getWorld(worldName);
                    Location loc = (world != null) ? new Location(world, locX, locY, locZ) : null;

                    Farmer farmer = new Farmer(id, worldName, chunkX, chunkZ, loc, ownerUuid, ownerName);
                    try {
                        farmer.setServerName(rs.getString("server_name"));
                    } catch (SQLException ignored) {
                    }
                    farmer.setCapacityLevel(rs.getInt("level_capacity"));
                    farmer.setRangeLevel(rs.getInt("level_range"));
                    farmer.setSpeedLevel(rs.getInt("level_speed"));
                    farmer.setFortuneLevel(rs.getInt("level_fortune"));
                    farmer.setTaxLevel(rs.getInt("level_tax"));
                    farmer.setFuelAmount(rs.getDouble("fuel_amount"));
                    farmer.setAutoSell(rs.getBoolean("auto_sell"));
                    try {
                        int interval = rs.getInt("auto_sell_interval");
                        if (interval > 0) {
                            farmer.setAutoSellInterval(interval);
                        }
                    } catch (SQLException ignored) {
                    }
                    farmer.setAutoReplant(rs.getBoolean("auto_replant"));
                    farmer.setMobCollect(rs.getBoolean("mob_collect"));
                    farmer.setCropCollect(rs.getBoolean("crop_collect"));
                    try {
                        farmer.setGroundCollect(rs.getBoolean("ground_collect"));
                    } catch (SQLException ignored) {
                        farmer.setGroundCollect(true);
                    }
                    farmer.setTotalEarned(rs.getDouble("total_earned"));
                    farmer.setTotalCollected(rs.getLong("total_collected"));

                    loadStorage(conn, farmer);
                    loadMembers(conn, farmer);
                    loadDisabledItems(conn, farmer);

                    farmer.setDirty(false);
                    return farmer;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load farmer " + id + ": " + e.getMessage());
        }
        return null;
    }

    public PlayerStats loadPlayerStatsForDate(UUID playerUuid, String date) {
        String sql = "SELECT crops_harvested, mobs_collected, items_sold, money_earned, fuel_used FROM lfarmer_player_stats WHERE player_uuid = ? AND stat_date = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerStats(
                            rs.getLong("crops_harvested"),
                            rs.getLong("mobs_collected"),
                            rs.getLong("items_sold"),
                            rs.getDouble("money_earned"),
                            rs.getDouble("fuel_used")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load player stats for date: " + e.getMessage());
        }
        return null;
    }

    public PlayerStats loadAggregatedPlayerStats(UUID playerUuid, String startDate) {
        String sql = startDate != null
                ? "SELECT SUM(crops_harvested) as ch, SUM(mobs_collected) as mc, SUM(items_sold) as isold, SUM(money_earned) as me, SUM(fuel_used) as fu FROM lfarmer_player_stats WHERE player_uuid = ? AND stat_date >= ?"
                : "SELECT SUM(crops_harvested) as ch, SUM(mobs_collected) as mc, SUM(items_sold) as isold, SUM(money_earned) as me, SUM(fuel_used) as fu FROM lfarmer_player_stats WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            if (startDate != null) {
                ps.setString(2, startDate);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerStats(
                            rs.getLong("ch"),
                            rs.getLong("mc"),
                            rs.getLong("isold"),
                            rs.getDouble("me"),
                            rs.getDouble("fu")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load aggregated player stats: " + e.getMessage());
        }
        return new PlayerStats();
    }

    public void savePlayerStats(UUID playerUuid, String statDate, PlayerStats stats) {
        String sql;
        if (isMySql) {
            sql = "INSERT INTO lfarmer_player_stats (player_uuid, stat_date, crops_harvested, mobs_collected, items_sold, money_earned, fuel_used) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE crops_harvested=VALUES(crops_harvested), mobs_collected=VALUES(mobs_collected), " +
                    "items_sold=VALUES(items_sold), money_earned=VALUES(money_earned), fuel_used=VALUES(fuel_used)";
        } else {
            sql = "INSERT OR REPLACE INTO lfarmer_player_stats (player_uuid, stat_date, crops_harvested, mobs_collected, items_sold, money_earned, fuel_used) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, statDate);
            ps.setLong(3, stats.getCropsHarvested());
            ps.setLong(4, stats.getMobsCollected());
            ps.setLong(5, stats.getItemsSold());
            ps.setDouble(6, stats.getMoneyEarned());
            ps.setDouble(7, stats.getFuelUsed());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save player stats: " + e.getMessage());
        }
    }

    public void saveLogEntry(FarmerLogEntry entry) {
        String sql = "INSERT INTO lfarmer_logs (timestamp, date_str, player_uuid, player_name, actor_name, action, details, server_name, location_str) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, entry.getTimestamp());
            ps.setString(2, entry.getDateStr());
            ps.setString(3, entry.getPlayerUuid().toString());
            ps.setString(4, entry.getPlayerName());
            ps.setString(5, entry.getActorName());
            ps.setString(6, entry.getAction());
            ps.setString(7, entry.getDetails());
            ps.setString(8, entry.getServerName());
            ps.setString(9, entry.getLocationStr());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save log entry: " + e.getMessage());
        }
    }

    public List<FarmerLogEntry> loadPlayerLogs(UUID playerUuid, int offset, int limit) {
        List<FarmerLogEntry> list = new ArrayList<>();
        String sql = "SELECT id, timestamp, date_str, player_uuid, player_name, actor_name, action, details, server_name, location_str " +
                "FROM lfarmer_logs WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new FarmerLogEntry(
                            rs.getLong("id"),
                            rs.getLong("timestamp"),
                            rs.getString("date_str"),
                            UUID.fromString(rs.getString("player_uuid")),
                            rs.getString("player_name"),
                            rs.getString("actor_name"),
                            rs.getString("action"),
                            rs.getString("details"),
                            rs.getString("server_name"),
                            rs.getString("location_str")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load player logs: " + e.getMessage());
        }
        return list;
    }

    public int countPlayerLogs(UUID playerUuid) {
        String sql = "SELECT COUNT(*) FROM lfarmer_logs WHERE player_uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to count player logs: " + e.getMessage());
        }
        return 0;
    }

    public int purgeOldLogs(long olderThanTimestamp) {
        String sql = "DELETE FROM lfarmer_logs WHERE timestamp < ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, olderThanTimestamp);
            return ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to purge old logs: " + e.getMessage());
            return 0;
        }
    }
}
