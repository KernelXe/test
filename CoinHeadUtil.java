package com.kngame.kncoin.utils;

import com.kngame.kncoin.Kncoin;
import com.kngame.kncoin.enums.CoinType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;


public class CoinHeadUtil {

    private static boolean headDBAvailable = false;
    private static Object headDatabaseAPI = null;


    private static final String COMMON_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzM3NjQ4YWU3YTU2NGE1Mjg3NzkyYjA1ZmFjYzc3M2UxNDBkY2Y1OGRkMWM4MDc1NDFhNTkyZDUwZmM2NzFlMCJ9fX0=";
    private static final String RARE_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzY5YjFhNTJiMjQ3OTEzNWJkNWY2MzBiZjdkY2RjMzY3NDYyYTVhYjcyYjkzNTE5NTFkNmMxZTNiNmY5ZGMzMSJ9fX0=";
    private static final String LEGENDARY_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDZiYTYzMzQ0ZjQ5ZGQxYzRmNmJkOThjZDEzNmU0NzU1NzgzOTNkMGEwZDE1ZjYyZDI0ODVlOWJkMGEwZmIxOCJ9fX0=";

    static {
        try {
            Class<?> headDatabaseClass = null;
            try {
                headDatabaseClass = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
            } catch (ClassNotFoundException e1) {
                try {
                    headDatabaseClass = Class.forName("me.arcaniax.hdb.HeadDatabaseAPI");
                } catch (ClassNotFoundException e2) {
                    try {
                        headDatabaseClass = Class.forName("me.shynixn.hdb.api.HeadDatabaseAPI");
                    } catch (ClassNotFoundException e3) {
                        throw new ClassNotFoundException("No HeadDatabase API found");
                    }
                }
            }

            headDatabaseAPI = headDatabaseClass.getMethod("getInstance").invoke(null);
            headDBAvailable = true;
            System.out.println("[Kncoin] ✅ HeadDatabase found");

        } catch (Exception e) {
            headDBAvailable = false;
            System.out.println("[Kncoin] ❌ HeadDatabase not found, using base64 textures");
        }
    }


    public static ItemStack createCoinHead(CoinType coinType) {
        Kncoin plugin = Kncoin.getInstance();


        if (plugin != null && plugin.getConfigManager().isHeadDBEnabled() && headDBAvailable) {
            String headDBId = plugin.getConfigManager().getCoinHeadDBId(coinType);
            if (!headDBId.isEmpty()) {
                ItemStack headDBItem = createHeadFromHeadDB(headDBId, coinType);
                if (headDBItem != null) {
                    System.out.println("[Kncoin] ✅ Using HeadDB: " + headDBId + " for " + coinType);
                    return headDBItem;
                }
            }
        }


        ItemStack base64Head = createHeadFromBase64Texture(coinType);
        if (base64Head != null) {
            System.out.println("[Kncoin] ✅ Using Base64 texture for " + coinType);
            return base64Head;
        }


        System.out.println("[Kncoin] ⚠️ Using fallback head for " + coinType);
        return createFallbackHead(coinType);
    }


    private static ItemStack createHeadFromBase64Texture(CoinType coinType) {
        try {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta == null) {
                System.out.println("[Kncoin] ❌ SkullMeta is null");
                return null;
            }

            String base64 = getBase64ForCoinType(coinType);


            boolean success = false;


            if (!success) {
                success = tryPlayerProfileMethod(meta, base64);
                if (success) System.out.println("[Kncoin] ✅ PlayerProfile method worked");
            }


            if (!success) {
                success = tryGameProfileReflection(meta, base64);
                if (success) System.out.println("[Kncoin] ✅ GameProfile reflection worked");
            }


            if (!success) {
                success = tryDirectNBTMethod(meta, base64);
                if (success) System.out.println("[Kncoin] ✅ Direct NBT method worked");
            }

            if (success) {
                customizeHead(meta, coinType);
                head.setItemMeta(meta);
                return head;
            } else {
                System.out.println("[Kncoin] ❌ All base64 methods failed for " + coinType);
            }

        } catch (Exception e) {
            System.out.println("[Kncoin] ❌ Base64 creation error: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }


    private static boolean tryPlayerProfileMethod(SkullMeta meta, String base64) {
        try {

            org.bukkit.Server server = org.bukkit.Bukkit.getServer();


            Method createProfileMethod = server.getClass().getMethod("createPlayerProfile", UUID.class, String.class);
            Object profile = createProfileMethod.invoke(server, UUID.randomUUID(), "CoinHead");


            Method getTexturesMethod = profile.getClass().getMethod("getTextures");
            Object textures = getTexturesMethod.invoke(profile);


            String textureUrl = extractUrlFromBase64(base64);
            if (textureUrl != null) {
                java.net.URL skinURL = new java.net.URL(textureUrl);


                Method setSkinMethod = textures.getClass().getMethod("setSkin", java.net.URL.class);
                setSkinMethod.invoke(textures, skinURL);


                Method setTexturesMethod = profile.getClass().getMethod("setTextures", textures.getClass().getSuperclass());
                setTexturesMethod.invoke(profile, textures);


                Method setProfileMethod = meta.getClass().getMethod("setPlayerProfile", profile.getClass().getSuperclass());
                setProfileMethod.invoke(meta, profile);

                return true;
            }
        } catch (Exception e) {
            System.out.println("[Kncoin] PlayerProfile method failed: " + e.getMessage());
        }
        return false;
    }


    private static boolean tryGameProfileReflection(SkullMeta meta, String base64) {
        try {

            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object gameProfile = gameProfileClass.getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), "CoinHead");


            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object property = propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", base64);


            Method getPropertiesMethod = gameProfileClass.getMethod("getProperties");
            Object propertyMap = getPropertiesMethod.invoke(gameProfile);
            Method putMethod = propertyMap.getClass().getMethod("put", Object.class, Object.class);
            putMethod.invoke(propertyMap, "textures", property);


            Field profileField = null;


            String[] possibleFields = {"profile", "serializedProfile", "gameProfile"};
            for (String fieldName : possibleFields) {
                try {
                    profileField = meta.getClass().getDeclaredField(fieldName);
                    break;
                } catch (NoSuchFieldException e) {
                    continue;
                }
            }

            if (profileField != null) {
                profileField.setAccessible(true);
                profileField.set(meta, gameProfile);
                return true;
            }

        } catch (Exception e) {
            System.out.println("[Kncoin] GameProfile reflection failed: " + e.getMessage());
        }
        return false;
    }


    private static boolean tryDirectNBTMethod(SkullMeta meta, String base64) {
        try {

            String hashedUUID = generateUUIDFromBase64(base64);
            UUID coinUUID = UUID.fromString(hashedUUID);


            org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(coinUUID);
            meta.setOwningPlayer(offlinePlayer);

            return true;
        } catch (Exception e) {
            System.out.println("[Kncoin] Direct NBT method failed: " + e.getMessage());
        }
        return false;
    }


    private static String extractUrlFromBase64(String base64) {
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(base64);
            String json = new String(decoded, "UTF-8");


            String urlStart = "\"url\":\"";
            int startIndex = json.indexOf(urlStart);
            if (startIndex != -1) {
                startIndex += urlStart.length();
                int endIndex = json.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    return json.substring(startIndex, endIndex);
                }
            }
        } catch (Exception e) {
            System.out.println("[Kncoin] URL extraction failed: " + e.getMessage());
        }
        return null;
    }


    private static String generateUUIDFromBase64(String base64) {
        try {
            // ใช้ hash ของ base64 สร้าง UUID
            int hash = base64.hashCode();
            long most = hash;
            long least = hash * 31L;

            UUID uuid = new UUID(most, least);
            return uuid.toString();
        } catch (Exception e) {
            // Fallback UUID
            return "f84c6a79-0a4e-45e0-879b-cd49ebd4c4e2";
        }
    }


    private static String getBase64ForCoinType(CoinType coinType) {
        switch (coinType) {
            case RARE:
                return RARE_TEXTURE;
            case LEGENDARY:
                return LEGENDARY_TEXTURE;
            default:
                return COMMON_TEXTURE;
        }
    }


    private static ItemStack createHeadFromHeadDB(String headId, CoinType coinType) {
        if (!headDBAvailable || headDatabaseAPI == null) {
            return null;
        }

        try {
            Class<?> headDatabaseClass = headDatabaseAPI.getClass();
            ItemStack head = null;

            String[] methodNames = {"getItemHead", "getHead", "getHeadItemStack", "getHeadById"};

            for (String methodName : methodNames) {
                try {
                    head = (ItemStack) headDatabaseClass.getMethod(methodName, String.class)
                            .invoke(headDatabaseAPI, headId);
                    if (head != null && head.getType() == Material.PLAYER_HEAD) {
                        break;
                    }
                } catch (Exception e) {
                    continue;
                }
            }

            if (head != null && head.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    customizeHead(meta, coinType);
                    head.setItemMeta(meta);
                }
                return head;
            }

        } catch (Exception e) {
            System.out.println("[Kncoin] HeadDB error: " + e.getMessage());
        }

        return null;
    }


    private static ItemStack createFallbackHead(CoinType coinType) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {

            String playerName = getFallbackPlayerName(coinType);
            try {

                meta.setOwner(playerName);
            } catch (Exception e) {

                try {
                    org.bukkit.OfflinePlayer player = org.bukkit.Bukkit.getOfflinePlayer(playerName);
                    meta.setOwningPlayer(player);
                } catch (Exception e2) {
                    System.out.println("[Kncoin] Fallback head creation failed: " + e2.getMessage());
                }
            }

            customizeHead(meta, coinType);
            head.setItemMeta(meta);
        }

        return head;
    }


    private static String getFallbackPlayerName(CoinType coinType) {
        switch (coinType) {
            case RARE:
                return "jeb_"; // หัวสีรุ้ง
            case LEGENDARY:
                return "Notch"; // หัวผู้สร้าง Minecraft
            default:
                return "Steve"; // หัวพื้นฐาน
        }
    }


    private static void customizeHead(SkullMeta meta, CoinType coinType) {
        Kncoin plugin = Kncoin.getInstance();


        String displayName;
        if (plugin != null) {
            displayName = plugin.getConfigManager().getCoinName(coinType);
        } else {
            displayName = getDefaultCoinName(coinType);
        }


        String[] lines = displayName.split("\\\\n");
        String mainName = lines[0];

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', mainName));


        java.util.List<String> lore = createEnhancedCoinLore(coinType, lines);
        meta.setLore(lore);
    }


    private static String getDefaultCoinName(CoinType coinType) {
        switch (coinType) {
            case RARE:
                return "&9&l✧ &b&lRARE COIN &9&l✧\\n&3เหรียญหายาก";
            case LEGENDARY:
                return "&6&l★ &e&lLEGENDARY COIN &6&l★\\n&c&lเหรียญในตำนาน";
            default:
                return "&f&l✦ &e&lCOMMON COIN &f&l✦\\n&7เหรียญธรรมดา";
        }
    }


    private static java.util.List<String> createEnhancedCoinLore(CoinType coinType, String[] nameLines) {
        java.util.List<String> lore = new java.util.ArrayList<>();


        if (nameLines.length > 1) {
            for (int i = 1; i < nameLines.length; i++) {
                lore.add(ChatColor.translateAlternateColorCodes('&', nameLines[i]));
            }
            lore.add("");
        }


        lore.add(ChatColor.translateAlternateColorCodes('&', "&7▸ &fย่อค้างใกล้เหรียญนี้"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7▸ &fเป็นเวลา &e3 วินาที &fเพื่อเก็บ"));
        lore.add("");


        switch (coinType) {
            case COMMON:
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7┌─ &fข้อมูลรางวัล &7─┐"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7│ &aExp: &f+5"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7│ &eไอเท็ม: &fธรรมดา"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7│ &bโอกาส: &f70%"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7└─────────────┘"));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&8&l✦ &7เหรียญที่พบได้ทั่วไป &8&l✦"));
                break;

            case RARE:
                lore.add(ChatColor.translateAlternateColorCodes('&', "&9┌─ &bข้อมูลรางวัล &9─┐"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&9│ &aExp: &f+15"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&9│ &eไอเท็ม: &bหายาก"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&9│ &bโอกาส: &f25%"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&9└─────────────┘"));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&9&l✧ &3เหรียญที่หายากกว่าปกติ &9&l✧"));
                break;

            case LEGENDARY:
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6┌─ &eข้อมูลรางวัล &6─┐"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6│ &aExp: &f+50"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6│ &eไอเท็ม: &6ในตำนาน"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6│ &bโอกาส: &f5%"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6└─────────────┘"));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6&l★ &c&lเหรียญที่หายากที่สุด! &6&l★"));
                break;
        }

        return lore;
    }


    public static boolean isCoinHead(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        String displayName = item.getItemMeta().getDisplayName();
        if (displayName == null) {
            return false;
        }
        String stripped = ChatColor.stripColor(displayName).toLowerCase();
        return stripped.contains("coin") &&
                (stripped.contains("common") || stripped.contains("rare") || stripped.contains("legendary"));
    }

    public static CoinType getCoinTypeFromItem(ItemStack item) {
        if (!isCoinHead(item)) {
            return null;
        }
        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase();
        if (displayName.contains("legendary")) {
            return CoinType.LEGENDARY;
        } else if (displayName.contains("rare")) {
            return CoinType.RARE;
        } else if (displayName.contains("common")) {
            return CoinType.COMMON;
        }
        return null;
    }

    public static ItemStack createWandItem() {
        ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
        org.bukkit.inventory.meta.ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&l⚡ Kncoin Wand &6&l⚡"));
            meta.setLore(Arrays.asList(
                    ChatColor.translateAlternateColorCodes('&', "&7▸ &fคลิกซ้าย: กำหนดตำแหน่งที่ 1"),
                    ChatColor.translateAlternateColorCodes('&', "&7▸ &fคลิกขวา: กำหนดตำแหน่งที่ 2"),
                    ChatColor.translateAlternateColorCodes('&', "&7▸ &fใช้ &e/kncoin create <ชื่อ> &fเพื่อสร้างพื้นที่"),
                    "",
                    ChatColor.translateAlternateColorCodes('&', "&6&l⚡ &eไม้กายสิทธิ์เลือกพื้นที่ &6&l⚡")
            ));
            wand.setItemMeta(meta);
        }
        return wand;
    }

    public static boolean isWandItem(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_AXE) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        String displayName = item.getItemMeta().getDisplayName();
        return displayName != null && ChatColor.stripColor(displayName).contains("Kncoin Wand");
    }

    public static boolean isHeadDBAvailable() {
        return headDBAvailable;
    }

    public static String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (formatted.length() > 0) {
                formatted.append(" ");
            }
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1));
            }
        }
        return formatted.toString();
    }


    public static void testBase64Textures() {
        System.out.println("[Kncoin] ================================");
        System.out.println("[Kncoin] 🧪 Testing Enhanced Base64 System");
        System.out.println("[Kncoin] ================================");

        for (CoinType coinType : CoinType.values()) {
            System.out.println("[Kncoin] Testing " + coinType + "...");
            String base64 = getBase64ForCoinType(coinType);
            System.out.println("[Kncoin] Base64: " + base64.substring(0, 50) + "...");

            String url = extractUrlFromBase64(base64);
            System.out.println("[Kncoin] Extracted URL: " + url);

            try {
                ItemStack head = createHeadFromBase64Texture(coinType);
                System.out.println("[Kncoin] " + coinType + " result: " +
                        (head != null ? "✅ SUCCESS" : "❌ FAILED"));

                if (head != null && head.hasItemMeta()) {
                    System.out.println("[Kncoin] Display Name: " + head.getItemMeta().getDisplayName());
                    System.out.println("[Kncoin] Lore Lines: " +
                            (head.getItemMeta().getLore() != null ? head.getItemMeta().getLore().size() : 0));
                }
            } catch (Exception e) {
                System.out.println("[Kncoin] " + coinType + " error: ❌ " + e.getMessage());
            }
            System.out.println("[Kncoin] --------------------------------");
        }

        System.out.println("[Kncoin] ================================");
    }


    public static ItemStack createSampleCoin(CoinType coinType) {
        ItemStack coin = createCoinHead(coinType);
        if (coin != null && coin.hasItemMeta()) {
            SkullMeta meta = (SkullMeta) coin.getItemMeta();
            java.util.List<String> lore = meta.getLore();
            if (lore == null) lore = new java.util.ArrayList<>();

            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&8&l▬▬▬ &7ตัวอย่าง &8&l▬▬▬"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7สำหรับทดสอบระบบเท่านั้น"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&cไม่สามารถเก็บได้จริง"));

            meta.setLore(lore);
            coin.setItemMeta(meta);
        }
        return coin;
    }
}