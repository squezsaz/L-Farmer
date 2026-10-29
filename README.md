[CENTER]
[SIZE=6][B]L-FARMER[/B][/SIZE]
[SIZE=4][B]Modular Automated Farming Solution[/B][/SIZE]
[SIZE=2]Minecraft 1.21+ | Paper, Purpur, Folia, Spigot[/SIZE]
[/CENTER]

[SIZE=5][B]Overview[/B][/SIZE]
L-Farmer is an enterprise-grade automated farming plugin designed specifically for modern Minecraft 1.21+ servers and Folia multithreading architecture. It provides reliable, high-performance crop harvesting, item collection, storage management, and team collaboration with zero unnecessary server overhead.

By utilizing Folia-native regional schedulers, asynchronous database connection pooling (HikariCP), and packet-level TextDisplay entities, L-Farmer runs lag-free even under heavy server loads.


[SIZE=5][B]Features[/B][/SIZE]

[B]Automated Harvesting & Collection[/B]
[LIST]
[*]Auto-Harvest: Automatically detects and harvests mature crops (Wheat, Carrots, Potatoes, Beetroots, Nether Wart, Cocoa Beans, Sugar Cane, Cactus, Melons, Pumpkins, Sweet Berries, and Bamboo).
[*]Auto-Replant: Instantly replants the appropriate seed upon harvesting.
[*]Mob Drop Collector: Collects drops from mobs and spawners within the operational perimeter.
[*]Ground Item Pickup: Vacuum drops lying on the ground directly into storage (restricted to whitelisted items).
[/LIST]

[B]Chunk Range Visualization[/B]
[LIST]
[*]Players can toggle a visual range display in the farmer interface.
[*]Spawns particle boundaries around all operational chunks for 10 seconds to clearly outline the coverage area.
[/LIST]

[B]Portable Farmer Egg[/B]
[LIST]
[*]Farmers are packaged as a single inventory item upon pickup or block break.
[*]All stored inventory items, upgrade tiers, and fuel amounts are preserved inside the item.
[/LIST]

[B]Five Modular Upgrade Categories[/B]
[LIST]
[*]Capacity: Expand storage capacity from 5,000 items up to 150,000+ items.
[*]Range: Increase working area from 1 chunk up to 3x3 or 5x5 chunks.
[*]Speed: Decrease harvesting check interval from 100 ticks down to 20 ticks.
[*]Fortune: Add double-drop probability up to 100% bonus yield.
[*]Tax Reduction: Reduce auto-sell tax rate from 20% down to 0%.
[/LIST]

[B]Fuel & Operating Costs[/B]
[LIST]
[*]Optional fuel system requiring upkeep for harvesting operations.
[*]Supports solid fuels (Coal, Charcoal, Coal Blocks, Lava Buckets, Blaze Rods).
[*]Allows economy-based salary payments to replenish fuel instantly.
[/LIST]

[B]Access Control & Member Management[/B]
[LIST]
[*]Add trusted teammates with granular permissions.
[*]Configurable access flags: Open GUI, Withdraw items, Sell items, and Purchase upgrades.
[/LIST]

[B]Audit Logging[/B]
[LIST]
[*]Detailed tracking of all actions: placements, removals, upgrades, fuel additions, sales, and withdrawals.
[*]Accessible in-game via an interactive log GUI (/farmer log) or through daily plain-text log files on disk.
[/LIST]

[B]Claim & Protection Compatibility[/B]
[LIST]
[*]Restricts placement outside owned claims, islands, or plots.
[*]Full integration with L-Claim, Lands, SuperiorSkyblock2, BentoBox, Towny, GriefPrevention, WorldGuard, and PlotSquared.
[*]Configurable world blacklists and region restrictions.
[/LIST]

[B]Holographic Displays[/B]
[LIST]
[*]Uses Minecraft 1.21 TextDisplay entities directly.
[*]Displays real-time owner, storage fill rate, and status without external hologram plugin dependencies.
[/LIST]

[B]Multi-Language Support[/B]
[LIST]
[*]Full out-of-the-box support for English and Turkish with 100% key parity.
[*]Historical logs dynamically format based on the active server locale.
[/LIST]


[SIZE=5][B]Commands & Permissions[/B][/SIZE]

[TABLE]
[TR]
[TH][B]Command[/B][/TH]
[TH][B]Description[/B][/TH]
[TH][B]Permission[/B][/TH]
[/TR]
[TR]
[TD]/farmer[/TD]
[TD]Opens the farmer GUI for the current chunk[/TD]
[TD][ICODE]farmer.use[/ICODE][/TD]
[/TR]
[TR]
[TD]/farmer help[/TD]
[TD]Displays the help guide[/TD]
[TD][ICODE]farmer.use[/ICODE][/TD]
[/TR]
[TR]
[TD]/farmer list[/TD]
[TD]Lists all placed farmers owned by the player[/TD]
[TD][ICODE]farmer.use[/ICODE][/TD]
[/TR]
[TR]
[TD]/farmer stats[/TD]
[TD]Opens the statistics dashboard[/TD]
[TD][ICODE]farmer.use[/ICODE][/TD]
[/TR]
[TR]
[TD]/farmer remove[/TD]
[TD]Picks up and packages the farmer at the current chunk[/TD]
[TD][ICODE]farmer.use[/ICODE][/TD]
[/TR]
[TR]
[TD]/farmer log <player> [page][/TD]
[TD]Opens the audit log viewer for a player[/TD]
[TD][ICODE]farmer.admin[/ICODE][/TD]
[/TR]
[TR]
[TD]/farmer admin [player/page] [page][/TD]
[TD]Opens the server-wide farmer management panel[/TD]
[TD][ICODE]farmer.admin[/ICODE][/TD]
[/TR]
[TR]
[TD]/farmer give <player> [amount][/TD]
[TD]Gives farmer egg item(s) to a player[/TD]
[TD][ICODE]farmer.admin.give[/ICODE][/TD]
[/TR]
[TR]
[TD]/farmer reload[/TD]
[TD]Reloads all configuration and locale files[/TD]
[TD][ICODE]farmer.admin.reload[/ICODE][/TD]
[/TR]
[/TABLE]

[B]Placement Limits[/B]
[LIST]
[*][ICODE]farmer.limit.<number>[/ICODE] - Sets the maximum placed farmers per player (e.g. [ICODE]farmer.limit.3[/ICODE]).
[*][ICODE]farmer.limit.admin[/ICODE] - Grants unlimited farmer placement.
[/LIST]


[SIZE=5][B]PlaceholderAPI[/B][/SIZE]

[TABLE]
[TR]
[TH][B]Placeholder[/B][/TH]
[TH][B]Description[/B][/TH]
[/TR]
[TR]
[TD][ICODE]%lfarmer_placed%[/ICODE][/TD]
[TD]Number of active farmers placed by the player[/TD]
[/TR]
[TR]
[TD][ICODE]%lfarmer_max%[/ICODE][/TD]
[TD]Maximum number of farmers the player can place[/TD]
[/TR]
[TR]
[TD][ICODE]%lfarmer_storage_total%[/ICODE][/TD]
[TD]Total items currently stored across player's farmers[/TD]
[/TR]
[TR]
[TD][ICODE]%lfarmer_total_earned%[/ICODE][/TD]
[TD]Total currency earned from farmer sales[/TD]
[/TR]
[TR]
[TD][ICODE]%lfarmer_total_collected%[/ICODE][/TD]
[TD]Total items harvested by player's farmers[/TD]
[/TR]
[TR]
[TD][ICODE]%lfarmer_chunk_has_farmer%[/ICODE][/TD]
[TD]Whether current chunk contains an active farmer (true/false)[/TD]
[/TR]
[TR]
[TD][ICODE]%lfarmer_chunk_farmer_owner%[/ICODE][/TD]
[TD]Owner name of the farmer in current chunk[/TD]
[/TR]
[TR]
[TD][ICODE]%lfarmer_chunk_farmer_level%[/ICODE][/TD]
[TD]Capacity upgrade level of the farmer in current chunk[/TD]
[/TR]
[/TABLE]


[SIZE=5][B]Supported Integrations[/B][/SIZE]

[LIST]
[*][B]Claims & Lands:[/B] L-Claim, Lands, SuperiorSkyblock2, BentoBox, Towny, GriefPrevention, WorldGuard, PlotSquared
[*][B]Economy & Prices:[/B] Vault, PlayerPoints, ShopGUIPlus, EconomyShopGUI, prices.yml
[*][B]Cosmetics & Addons:[/B] ItemsAdder, PlaceholderAPI
[*][B]Server Software:[/B] Spigot, Paper, Purpur, Folia (1.21+)
[/LIST]


[SIZE=5][B]Configuration[/B][/SIZE]

[SPOILER="config.yml"]
[CODE=yaml]
language: en
server_name: "Survival-1"

logging:
  enabled: true
  purge_after_days: 15

database:
  type: SQLITE # SQLITE or MYSQL
  mysql:
    host: localhost
    port: 3306
    database: lfarmer
    username: root
    password: ""
    ssl: false

default_limit: 1

disabled_worlds:
  - "world_the_end"

farmer_item:
  material: VILLAGER_SPAWN_EGG
  name: "<gold><bold>Farmer Spawn Egg</bold></gold>"
  custom_model_data: 0

npc:
  type: VILLAGER

itemsadder:
  enabled: false
  custom_model: "custom_namespace:custom_farmer"

harvest:
  check_interval_ticks: 60

ground_collect:
  check_interval_ticks: 20

auto_sell:
  interval_seconds: 60
  notify_actionbar: true

fuel:
  enabled: false
  cost_per_harvest: 0.1
  cost_per_mob: 0.05
  cost_per_ground_pickup: 0.02
  max_capacity: 10000.0
  salary:
    cost: 500.0
    fuel_reward: 1000.0
[/CODE]
[/SPOILER]


[SIZE=5][B]Installation[/B][/SIZE]
[LIST=1]
[*]Download L-Farmer.jar.
[*]Place the file into your server's plugins directory.
[*]Verify that the server runs Java 21+ and Minecraft 1.21+.
[*]Start or restart the server.
[*]Adjust settings in config.yml, prices.yml, and locale files as desired.
[*]Execute /farmer reload to apply any modifications.
[/LIST]
