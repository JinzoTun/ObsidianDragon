package com.obsidian.dragon.gui;

import com.obsidian.dragon.ObsidianDragon;
import com.obsidian.dragon.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Manages the advanced loot editor GUI system with custom dialogs.
 * Clean consolidated implementation: single definitions, safe checks, and updated item editor layout.
 */
public class EditorMenuManager {

    private final ObsidianDragon plugin;
    private final MessageUtil msg;
    private final Map<UUID, EditorSession> editorSessions;
    private final LootConfigManager lootConfigManager;
    // Pagination state for loot editor per-player
    private final Map<UUID, Integer> lootEditorPages = new HashMap<>();

    // GUI constants
    private static final int LOOT_MENU_SIZE = 54;
    private static final int BACK_SLOT = 49; // bottom-center (Back to editor)
    // Bottom-left (slot 45) is used for Add New Loot
    private static final int ADD_SLOT = 45;  // Add New Loot at bottom-left
    private static final int GET_ALL_SLOT = 53; // moved Get All Items to bottom-right
    private static final int SORT_SLOT = 46; // moved Sort to slot 46 (left-middle)
    // Put navigation arrows around the center: prev at 48 (left of back), back at 49 (center), next at 50 (right of back)
    private static final int PREV_PAGE_SLOT = 48; // left arrow (previous page)
    private static final int NEXT_PAGE_SLOT = 50; // right arrow (next page)

    public EditorMenuManager(ObsidianDragon plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageUtil();
        this.editorSessions = new HashMap<>();
        this.lootConfigManager = new LootConfigManager(plugin);
    }

    /**
     * Opens the main editor menu.
     */
    public void openEditorMenu(Player player) {
        Component title = LegacyComponentSerializer.legacySection().deserialize("§0Editor");
        Inventory menu = Bukkit.createInventory(null, 27, title);

        // Vault Block - Opens Loot Editor
        menu.setItem(13, buildMenuItem(Material.VAULT, "§6§lLoot Editor", List.of("§7Click to edit loot items", "", "§e▶ Open Loot Editor")));

        player.openInventory(menu);
    }

    /**
     * Opens the loot editor menu showing all loot items.
     */
    public void openLootEditorMenu(Player player) {
        Component title = LegacyComponentSerializer.legacySection().deserialize("§0Loot Editor");
        Inventory menu = Bukkit.createInventory(null, LOOT_MENU_SIZE, title);

        List<LootConfigManager.LootEntry> lootEntries = lootConfigManager.getAllLootEntries();

        // Determine page and capacity
        final int capacity = getInteriorSlots().size(); // number of items per page
        int page = lootEditorPages.getOrDefault(player.getUniqueId(), 0);
        if (page < 0) page = 0;
        int startIndex = page * capacity;

        // Create border (black) and interior filler (gray)
        // Apply standard menu border and filler
        applyMenuBorderAndFiller(menu);

        // Place loot items into interior slots only (paged)
        List<Integer> interiorSlots = getInteriorSlots();
        int placed = 0;
        for (int i = startIndex; i < lootEntries.size() && placed < interiorSlots.size(); i++) {
            LootConfigManager.LootEntry entry = lootEntries.get(i);
            ItemStack displayItem = createLootDisplayItem(entry);
            int targetSlot = interiorSlots.get(placed);
            menu.setItem(targetSlot, displayItem);
            placed++;
        }

        // Add new loot button (bottom-left)
        menu.setItem(ADD_SLOT, buildMenuItem(Material.EMERALD, "§a§lAdd New Loot", List.of("§7Click to add a new loot item")));

        // Sort by Chance (moved to slot 46)
        menu.setItem(SORT_SLOT, buildMenuItem(Material.COMPARATOR, "§b§lSort by Chance (Desc)", List.of("§7Sort loot entries by chance (high -> low)")));

        // Get All Items (moved to bottom-right)
        menu.setItem(GET_ALL_SLOT, buildMenuItem(Material.CHEST, "§e§lGet All Items", List.of("§7Receive all loot items with their names and lore")));

        // Back button (bottom-center)
        menu.setItem(BACK_SLOT, buildMenuItem(Material.IRON_DOOR, "§7Back", List.of("§7Return to Main Editor")));

        // Previous page arrow (left of center)
        menu.setItem(PREV_PAGE_SLOT, buildMenuItem(Material.ARROW, "§e⟵ Previous Page", List.of("§7Page: §f" + (page + 1) + " / " + Math.max(1, (int)Math.ceil((double)lootEntries.size()/Math.max(1, capacity))), "§7Click to go to the previous page")));

        // Next page arrow (right of center)
        // Use a normal arrow for the next page (keep visuals consistent)
        menu.setItem(NEXT_PAGE_SLOT, buildMenuItem(Material.ARROW, "§eNext Page ⟶", List.of("§7Page: §f" + (page + 1) + " / " + Math.max(1, (int)Math.ceil((double)lootEntries.size()/Math.max(1, capacity))), "§7Click to go to the next page")));

        player.openInventory(menu);
    }

    /**
     * Opens the item editor menu for a specific loot item. Layout:
     * - black border
     * - current item at slot 13 (second row center)
     * - five option buttons centered on row 3 at slots 29..33 in order: swap(bundle), amount, name, chance, lore
     */
    public void openItemEditorMenu(Player player, int lootIndex) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null || session.lootIndex != lootIndex) {
            LootConfigManager.LootEntry entry = lootConfigManager.getLootEntry(lootIndex);
            if (entry == null) {
                msg.send(player, "&cInvalid loot item!");
                return;
            }
            session = new EditorSession(lootIndex, entry);
            editorSessions.put(player.getUniqueId(), session);
        }

        Component title = LegacyComponentSerializer.legacySection().deserialize("§0Item Editor");
        Inventory menu = Bukkit.createInventory(null, LOOT_MENU_SIZE, title);

        // Apply standard menu border and filler
        applyMenuBorderAndFiller(menu);

        // Current item at slot 13
        int currentSlot = 13;
        ItemStack current;
        if (session.entry.material != null && !session.entry.material.isEmpty() && !session.entry.material.equalsIgnoreCase("AIR")) {
            Material mat = Material.getMaterial(session.entry.material);
            if (mat == null) mat = Material.BARRIER;
            current = new ItemStack(mat);
            ItemMeta cm = current.getItemMeta();
            if (cm != null) {
                cm.displayName(LegacyComponentSerializer.legacySection().deserialize("§e§lCurrent Item"));
                List<Component> lore = new ArrayList<>();
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Material: §f" + session.entry.material));
                lore.add(Component.empty());
                lore.add(LegacyComponentSerializer.legacySection().deserialize("§7This is the current loot item"));
                cm.lore(lore);
                current.setItemMeta(cm);
            }
        } else {
            current = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta cm = current.getItemMeta();
            if (cm != null) cm.displayName(LegacyComponentSerializer.legacySection().deserialize("§7No item set"));
        }
        menu.setItem(currentSlot, current);

        // Option slots 29..33 (row 3 centered)
        int baseRow = 3 * 9; // 27
        int[] opts = {baseRow + 2, baseRow + 3, baseRow + 4, baseRow + 5, baseRow + 6};

        // Swap (bundle)
        menu.setItem(opts[0], buildMenuItem(Material.BUNDLE, "§a§lSwap Item Here", List.of("§7Drag & drop an item here to set content")));

        // Amount
        menu.setItem(opts[1], buildMenuItem(Material.HOPPER, "§a§lAmount", List.of("§7Current: §f" + session.entry.amount, "", "§e▶ Click to edit")));

        // Name
        String currentName = session.entry.customName != null && !session.entry.customName.isEmpty() ? "§7Current: " + session.entry.customName : "§7Current: §8None";
        menu.setItem(opts[2], buildMenuItem(Material.NAME_TAG, "§b§lCustom Name", List.of(currentName, "", "§e▶ Click to edit")));

        // Chance
        menu.setItem(opts[3], buildMenuItem(Material.NETHER_STAR, "§d§lChance", List.of("§7Current: §f" + session.entry.chance + "%", "", "§e▶ Click to edit")));

        // Lore
        List<String> loreLines = new ArrayList<>();
        if (session.entry.lore != null && !session.entry.lore.isEmpty()) {
            loreLines.add("§7Current lore:");
            for (String line : session.entry.lore) loreLines.add("§8- " + line);
        } else {
            loreLines.add("§7Current: §8None");
        }
        loreLines.add("");
        loreLines.add("§e▶ Click to edit");
        menu.setItem(opts[4], buildMenuItem(Material.WRITABLE_BOOK, "§6§lLore", loreLines));

        // Delete / Back / Save (preserve previous slots)
        menu.setItem(45, buildMenuItem(Material.RED_STAINED_GLASS_PANE, "§c§lDelete Item", List.of("§7Click to delete this loot item", "§c⚠ This cannot be undone!")));
        menu.setItem(BACK_SLOT, buildMenuItem(Material.IRON_DOOR, "§7Back", List.of("§7Return to Loot Editor")));
        menu.setItem(53, buildMenuItem(Material.LIME_STAINED_GLASS_PANE, "§a§lSave Changes", List.of("§7Click to save all changes")));

        player.openInventory(menu);
    }

    /**
     * Handles clicks in the item editor menu.
     */
    private void handleItemEditorClick(Player player, int slot) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) return;

        switch (slot) {
            case 13 -> {
                // Drop a test copy of the current session item at the player's location
                LootConfigManager.LootEntry e = session.entry;
                Material mat = Material.getMaterial(e.material);
                if (mat == null) mat = Material.BARRIER;
                int amount = 1;
                try { amount = Math.max(1, Integer.parseInt(e.amount)); } catch (Exception ignored) {}

                ItemStack test = new ItemStack(mat, amount);
                ItemMeta tm = test.getItemMeta();
                if (tm != null) {
                    if (e.customName != null && !e.customName.isEmpty()) {
                        tm.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(e.customName));
                    }
                    if (e.lore != null && !e.lore.isEmpty()) {
                        List<Component> loreComp = new ArrayList<>();
                        for (String line : e.lore) loreComp.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
                        tm.lore(loreComp);
                    }
                    test.setItemMeta(tm);
                }

                // Drop the item near the player for testing
                player.getWorld().dropItemNaturally(player.getLocation(), test);
                msg.send(player, "&aDropped a test item at your feet (preserves name & lore).");
            }
            case 29 -> {
                // swap slot (opened by MenuClickListener due to drag/drop) - tolerate click
            }
            case 30 -> showDialog(player, DialogType.AMOUNT, session.entry.amount);
            case 31 -> showDialog(player, DialogType.NAME, session.entry.customName);
            case 32 -> showDialog(player, DialogType.CHANCE, String.valueOf(session.entry.chance));
            case 33 -> {
                String currentLore = session.entry.lore != null ? String.join("|", session.entry.lore) : "";
                showDialog(player, DialogType.LORE, currentLore);
            }
            case 45 -> {
                // Delete
                if (lootConfigManager.deleteLootEntry(session.lootIndex)) {
                    msg.send(player, "&aLoot item deleted!");
                    plugin.getLootManager().reload();
                } else {
                    msg.send(player, "&cFailed to delete loot item!");
                }
                editorSessions.remove(player.getUniqueId());
                openLootEditorMenu(player);
            }
            case BACK_SLOT -> {
                editorSessions.remove(player.getUniqueId());
                openLootEditorMenu(player);
            }
            case 53 -> {
                // Save
                if (lootConfigManager.updateLootEntry(session.lootIndex, session.entry)) {
                    msg.send(player, "&aChanges saved successfully!");
                    plugin.getLootManager().reload();
                } else {
                    msg.send(player, "&cFailed to save changes!");
                }
                editorSessions.remove(player.getUniqueId());
                openLootEditorMenu(player);
            }
        }
    }

    /**
     * Centralized menu click entry for Editor-related menus.
     */
    public void handleMenuClick(Player player, String title, int slot) {
        if (title.contains("Editor") && !title.contains("Loot") && !title.contains("Item")) {
            if (slot == 13) {
                openLootEditorMenu(player);
            }
            return;
        }

        if (title.contains("Loot Editor")) {
            List<Integer> interiorSlots = getInteriorSlots();

            if (slot == ADD_SLOT) {
                lootConfigManager.addNewLootEntry();
                msg.send(player, "&aNew loot item added!");
                // After adding, jump to last page so the new item is visible
                List<LootConfigManager.LootEntry> all = lootConfigManager.getAllLootEntries();
                int capacity = interiorSlots.size();
                int total = all.size();
                int lastPage = Math.max(0, (int) Math.ceil((double) total / Math.max(1, capacity)) - 1);
                lootEditorPages.put(player.getUniqueId(), lastPage);
                openLootEditorMenu(player);
                return;
            }

            if (slot == BACK_SLOT) {
                // clear paging when leaving
                lootEditorPages.remove(player.getUniqueId());
                openEditorMenu(player);
                return;
            }

            if (slot == GET_ALL_SLOT) {
                // Give player all configured loot items
                giveAllLootItems(player);
                return;
            }

            if (slot == SORT_SLOT) {
                sortLootByChance(player);
                return;
            }

            if (slot == PREV_PAGE_SLOT) {
                // Go to previous page if possible
                int page = lootEditorPages.getOrDefault(player.getUniqueId(), 0);
                if (page > 0) {
                    lootEditorPages.put(player.getUniqueId(), page - 1);
                    openLootEditorMenu(player);
                } else {
                    msg.send(player, "&7Already on the first page.");
                }
                return;
            }

            if (slot == NEXT_PAGE_SLOT) {
                // Advance page if possible
                List<LootConfigManager.LootEntry> all = lootConfigManager.getAllLootEntries();
                int capacity = interiorSlots.size();
                int total = all.size();
                int page = lootEditorPages.getOrDefault(player.getUniqueId(), 0);
                int maxPage = Math.max(0, (int) Math.ceil((double) total / Math.max(1, capacity)) - 1);
                if (page < maxPage) {
                    lootEditorPages.put(player.getUniqueId(), page + 1);
                    openLootEditorMenu(player);
                } else {
                    msg.send(player, "&7No more pages.");
                }
                return;
            }

            int index = interiorSlots.indexOf(slot);
            if (index != -1) {
                // Compute the global loot index using current page
                int page = lootEditorPages.getOrDefault(player.getUniqueId(), 0);
                int capacity = interiorSlots.size();
                int globalIndex = page * capacity + index;
                List<LootConfigManager.LootEntry> all = lootConfigManager.getAllLootEntries();
                if (globalIndex >= 0 && globalIndex < all.size()) {
                    openItemEditorMenu(player, globalIndex);
                } else {
                    msg.send(player, "&7No loot item in that slot.");
                }
            }
            return;
        }

        if (title.contains("Item Editor")) {
            handleItemEditorClick(player, slot);
        }
    }

    /**
     * Show a dialog to the player (opens sign/chat input).
     */
    public void showDialog(Player player, DialogType type, String currentValue) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) {
            msg.send(player, "&cNo active editing session!");
            return;
        }
        session.dialogType = type;
        // Close the menu on the next server tick so the client's click handling finishes first.
        // Scheduling avoids the client-side race that sometimes leaves the inventory visible.
        plugin.getServer().getScheduler().runTask(plugin, () -> player.closeInventory());
        openSignInput(player, type, currentValue);
    }

    /**
     * Opens a sign/chat input prompt for the player (marks session awaiting input).
     */
    private void openSignInput(Player player, DialogType type, String currentValue) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) return;

        String instruction = switch (type) {
            case CHANCE -> "§7Enter chance (0-100)";
            case NAME -> "§7Enter custom name";
            case AMOUNT -> "§7Enter amount/range";
            case LORE -> "§7Enter lore (use | for new lines)";
        };

        msg.send(player, "&e&lInput Required");
        msg.send(player, instruction);
        msg.send(player, "&7Current: &f" + (currentValue != null ? currentValue : "None"));
        msg.send(player, "&7Type your value in chat, or type &ccancel &7to go back.");

        session.awaitingChatInput = true;
    }

    /**
     * Handles dialog response from chat input.
     */
    public void handleDialogResponse(Player player, String response) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) {
            msg.send(player, "&cNo active editing session! Please reopen the editor.");
            return;
        }

        session.awaitingChatInput = false;

        if (response == null || response.equalsIgnoreCase("back") || response.equalsIgnoreCase("cancel")) {
            msg.send(player, "&7Input cancelled.");
            openItemEditorMenu(player, session.lootIndex);
            return;
        }

        switch (session.dialogType) {
            case CHANCE -> {
                try {
                    double chance = Double.parseDouble(response);
                    if (chance >= 0 && chance <= 100) {
                        session.entry.chance = chance;
                        msg.send(player, "&aChance updated to §f" + chance + "%");
                        msg.send(player, "&7Click §aSave §7to apply changes!");
                    } else {
                        msg.send(player, "&cChance must be between 0 and 100!");
                    }
                } catch (NumberFormatException e) {
                    msg.send(player, "&cInvalid number format! Please enter a number.");
                }
            }
            case NAME -> {
                session.entry.customName = response;
                msg.send(player, "&aName updated to: " + response);
                msg.send(player, "&7Click §aSave §7to apply changes!");
            }
            case AMOUNT -> {
                session.entry.amount = response;
                msg.send(player, "&aAmount updated to: §f" + response);
                msg.send(player, "&7Click §aSave §7to apply changes!");
            }
            case LORE -> {
                session.entry.lore = Arrays.asList(response.split("\\|"));
                msg.send(player, "&aLore updated!");
                msg.send(player, "&7Click §aSave §7to apply changes!");
            }
        }

        openItemEditorMenu(player, session.lootIndex);
    }

    public boolean isAwaitingChatInput(Player player) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        return session != null && session.awaitingChatInput;
    }

    /**
     * Returns a list of interior slot indices in the 54-slot loot menu (non-border slots).
     */
    private List<Integer> getInteriorSlots() {
        List<Integer> interiorSlots = new ArrayList<>();
        for (int i = 0; i < LOOT_MENU_SIZE; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row != 0 && row != 5 && col != 0 && col != 8) {
                interiorSlots.add(i);
            }
        }
        return interiorSlots;
    }

    /**
     * Opens the content editor (single-slot) where players can swap the actual item.
     */
    public void openContentEditor(Player player) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) {
            msg.send(player, "&cNo active editing session!");
            return;
        }

        Component title = LegacyComponentSerializer.legacySection().deserialize("§0Content");
        Inventory menu = Bukkit.createInventory(null, LOOT_MENU_SIZE, title);

        // Apply standard border and filler
        applyMenuBorderAndFiller(menu);

        int contentSlot = 22;
        ItemStack center;
        if (session.entry.material != null && !session.entry.material.isEmpty() && !session.entry.material.equalsIgnoreCase("AIR")) {
            Material mat = Material.getMaterial(session.entry.material);
            if (mat == null) mat = Material.BARRIER;
            center = new ItemStack(mat);
            ItemMeta cm = center.getItemMeta();
            if (cm != null) {
                if (session.entry.customName != null && !session.entry.customName.isEmpty()) cm.displayName(LegacyComponentSerializer.legacySection().deserialize(session.entry.customName));
                if (session.entry.lore != null && !session.entry.lore.isEmpty()) {
                    List<Component> loreComp = new ArrayList<>();
                    for (String line : session.entry.lore) loreComp.add(LegacyComponentSerializer.legacySection().deserialize(line));
                    cm.lore(loreComp);
                }
                center.setItemMeta(cm);
            }
        } else {
            center = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            ItemMeta pm = center.getItemMeta();
            if (pm != null) pm.displayName(LegacyComponentSerializer.legacySection().deserialize("§7Place your item here (only slot 22 is active)"));
        }
        menu.setItem(contentSlot, center);

        // Back & Save
        menu.setItem(BACK_SLOT, buildMenuItem(Material.IRON_DOOR, "§7Back", null));
        menu.setItem(53, buildMenuItem(Material.LIME_STAINED_GLASS_PANE, "§aSave", null));

        player.openInventory(menu);
    }

    /**
     * Applies an ItemStack (from content menu) to the session.entry (memory only).
     */
    public void applyContentItem(Player player, ItemStack item) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) return;
        if (item == null || item.getType() == Material.AIR) {
            session.entry.material = "AIR";
            session.entry.amount = "1";
            session.entry.customName = "";
            session.entry.lore = new ArrayList<>();
            msg.send(player, "&eContent cleared (will be saved when you click Save)");
            return;
        }

        session.entry.material = item.getType().name();
        session.entry.amount = String.valueOf(Math.max(1, item.getAmount()));

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Component dn = meta.displayName();
            session.entry.customName = dn != null ? LegacyComponentSerializer.legacyAmpersand().serialize(dn) : "";
            List<String> loreStrings = new ArrayList<>();
            List<Component> loreComp = meta.lore();
            if (loreComp != null) {
                for (Component c : loreComp) if (c != null) loreStrings.add(LegacyComponentSerializer.legacyAmpersand().serialize(c));
            }
            session.entry.lore = loreStrings;
        }

        msg.send(player, "&aContent updated in session. Click Save to persist.");
    }

    /**
     * Handles saving content from content editor to config.
     */
    public void handleContentSave(Player player) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        if (session == null) return;
        boolean ok = lootConfigManager.updateLootEntry(session.lootIndex, session.entry);
        if (ok) {
            msg.send(player, "&aContent saved to loot.yml");
            plugin.getLootManager().reload();
        } else {
            msg.send(player, "&cFailed to save content to loot.yml");
        }
        openItemEditorMenu(player, session.lootIndex);
    }

    private ItemStack createLootDisplayItem(LootConfigManager.LootEntry entry) {
        Material material = Material.getMaterial(entry.material);
        if (material == null) material = Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = entry.customName != null && !entry.customName.isEmpty() ? entry.customName : "§f" + entry.material;
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(displayName));
            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Material: §f" + entry.material));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Amount: §f" + entry.amount));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Chance: §f" + entry.chance + "%"));
            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§e▶ Click to edit"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void clearSession(UUID playerId) { editorSessions.remove(playerId); }

    private static class EditorSession {
        int lootIndex;
        LootConfigManager.LootEntry entry;
        DialogType dialogType;
        boolean awaitingChatInput;

        EditorSession(int lootIndex, LootConfigManager.LootEntry entry) {
            this.lootIndex = lootIndex;
            this.entry = entry.clone();
            this.awaitingChatInput = false;
        }
    }

    public enum DialogType { CHANCE, NAME, AMOUNT, LORE }

    public int getSessionIndex(Player player) {
        EditorSession session = editorSessions.get(player.getUniqueId());
        return session != null ? session.lootIndex : -1;
    }

    /**
     * Give all loot items to the player, preserving name & lore.
     */
    private void giveAllLootItems(Player player) {
        List<LootConfigManager.LootEntry> all = lootConfigManager.getAllLootEntries();
        if (all.isEmpty()) {
            msg.send(player, "&7No loot items configured.");
            return;
        }

        for (LootConfigManager.LootEntry e : all) {
            Material mat = Material.getMaterial(e.material);
            if (mat == null) mat = Material.BARRIER;
            int amount = 1;
            try { amount = Math.max(1, Integer.parseInt(e.amount)); } catch (NumberFormatException ignored) {}

            ItemStack is = new ItemStack(mat, amount);
            ItemMeta im = is.getItemMeta();
            if (im != null) {
                if (e.customName != null && !e.customName.isEmpty()) {
                    im.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(e.customName));
                }
                if (e.lore != null && !e.lore.isEmpty()) {
                    List<Component> loreComp = new ArrayList<>();
                    for (String line : e.lore) loreComp.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
                    im.lore(loreComp);
                }
                is.setItemMeta(im);
            }

            // Try to add to inventory; if full, drop at player's location
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(is);
            if (!leftover.isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), is);
            }
        }
        msg.send(player, "&aAll loot items have been given to you (check inventory). If full, items were dropped on the ground.");
    }

    /**
     * Sort loot entries by chance descending and persist.
     */
    private void sortLootByChance(Player player) {
        boolean ok = lootConfigManager.sortByChanceDescending();
        if (ok) {
            msg.send(player, "&aLoot entries sorted by chance (high → low).");
            openLootEditorMenu(player);
        } else {
            msg.send(player, "&cFailed to sort loot entries.");
        }
    }

    /**
     * Apply the standard border and filler to a menu (used in multiple places to avoid code duplication).
     */
    private void applyMenuBorderAndFiller(Inventory menu) {
        // Create border (black) and interior filler (gray)
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) borderMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§8 "));
        if (borderMeta != null) border.setItemMeta(borderMeta);

        ItemStack interior = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta intMeta = interior.getItemMeta();
        if (intMeta != null) intMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§8 "));
        if (intMeta != null) interior.setItemMeta(intMeta);

        // Fill all slots with interior then overwrite border slots with black panes
        for (int i = 0; i < LOOT_MENU_SIZE; i++) {
            menu.setItem(i, interior.clone());
        }
        for (int i = 0; i < LOOT_MENU_SIZE; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) {
                menu.setItem(i, border.clone());
            }
        }
    }

    /**
     * Small helper that builds a menu ItemStack with display name and lore (supports empty lines using empty strings).
     */
    private ItemStack buildMenuItem(Material material, String display, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (display != null) meta.displayName(LegacyComponentSerializer.legacySection().deserialize(display));
            if (loreLines != null && !loreLines.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String l : loreLines) {
                    if (l == null || l.isEmpty()) lore.add(Component.empty());
                    else lore.add(LegacyComponentSerializer.legacySection().deserialize(l));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

}
