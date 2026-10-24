package me.estructuras;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Estructuras extends JavaPlugin implements TabExecutor {

    /** Una estructura: leyenda (char -> blockdata) y capas de abajo hacia arriba. '.' = no tocar. */
    record Struct(Map<Character, String> legend, String[][] layers) {}

    private final Map<String, Struct> structs = new LinkedHashMap<>();
    private final Map<UUID, Deque<List<BlockState>>> history = new HashMap<>();

    @Override
    public void onEnable() {
        registrar();
        PluginCommand cmd = getCommand("estructura");
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
        getLogger().info("Cargadas " + structs.size() + " estructuras.");
    }

    private void registrar() {
        // ---------- FOGATA 2x2 (huella 4x4) ----------
        structs.put("fogata_2x2", new Struct(
            Map.of('C', "minecraft:cobblestone",
                   'F', "minecraft:campfire[lit=true,facing=north]",
                   'W', "minecraft:cobblestone_wall",
                   'L', "minecraft:lantern[hanging=false]"),
            new String[][]{
                {"CCCC",
                 "CFFC",
                 "CFFC",
                 "CCCC"},
                {"W..W",
                 "....",
                 "....",
                 "W..W"},
                {"L..L",
                 "....",
                 "....",
                 "L..L"}
            }));

        // ---------- FOGATA 3x3 (huella 5x5) ----------
        structs.put("fogata_3x3", new Struct(
            Map.of('C', "minecraft:cobblestone",
                   'S', "minecraft:stone_bricks",
                   'F', "minecraft:campfire[lit=true,facing=north]",
                   'W', "minecraft:cobblestone_wall",
                   'L', "minecraft:lantern[hanging=false]"),
            new String[][]{
                {"CSSSC",
                 "SFFFS",
                 "SFFFS",
                 "SFFFS",
                 "CSSSC"},
                {"W...W",
                 ".....",
                 ".....",
                 ".....",
                 "W...W"},
                {"L...L",
                 ".....",
                 ".....",
                 ".....",
                 "L...L"}
            }));

        // ---------- BRASERO DE ALMAS 2x2 (huella 4x4) ----------
        structs.put("brasero_almas_2x2", new Struct(
            Map.of('B', "minecraft:polished_blackstone_bricks",
                   'F', "minecraft:soul_campfire[lit=true,facing=north]",
                   'W', "minecraft:polished_blackstone_brick_wall",
                   'L', "minecraft:soul_lantern[hanging=false]"),
            new String[][]{
                {"BBBB",
                 "BFFB",
                 "BFFB",
                 "BBBB"},
                {"W..W",
                 "....",
                 "....",
                 "W..W"},
                {"L..L",
                 "....",
                 "....",
                 "L..L"}
            }));

        // ---------- FOGATA DE ALMAS 3x3 (huella 5x5) ----------
        structs.put("fogata_almas_3x3", new Struct(
            Map.of('C', "minecraft:deepslate_bricks",
                   'S', "minecraft:polished_blackstone_bricks",
                   'F', "minecraft:soul_campfire[lit=true,facing=north]",
                   'W', "minecraft:deepslate_brick_wall",
                   'L', "minecraft:soul_lantern[hanging=false]"),
            new String[][]{
                {"CSSSC",
                 "SFFFS",
                 "SFFFS",
                 "SFFFS",
                 "CSSSC"},
                {"W...W",
                 ".....",
                 ".....",
                 ".....",
                 "W...W"},
                {"L...L",
                 ".....",
                 ".....",
                 ".....",
                 "L...L"}
            }));

        // ---------- HOGUERA PILA DE TRONCOS (huella 5x5, alta) ----------
        structs.put("hoguera_pila", new Struct(
            Map.of('C', "minecraft:cobblestone",
                   'F', "minecraft:campfire[lit=true,facing=north]",
                   'x', "minecraft:spruce_log[axis=x]",
                   'z', "minecraft:spruce_log[axis=z]",
                   'y', "minecraft:spruce_log[axis=y]",
                   'H', "minecraft:hay_block[axis=y]"),
            new String[][]{
                {"CCCCC",
                 "CFFFC",
                 "CFFFC",
                 "CFFFC",
                 "CCCCC"},
                {".....",
                 ".xxx.",
                 ".z.z.",
                 ".xxx.",
                 "....."},
                {".....",
                 "..y..",
                 ".yHy.",
                 "..y..",
                 "....."},
                {".....",
                 ".....",
                 "..y..",
                 ".....",
                 "....."},
                {".....",
                 ".....",
                 "..F..",
                 ".....",
                 "....."}
            }));

        // ---------- TORRE DE HUMO (huella 3x3, alta) ----------
        structs.put("torre_humo", new Struct(
            Map.of('C', "minecraft:stone_bricks",
                   'F', "minecraft:campfire[lit=true,facing=north]",
                   'H', "minecraft:hay_block[axis=y]"),
            new String[][]{
                {"CCC",
                 "CFC",
                 "CCC"},
                {"...",
                 ".H.",
                 "..."},
                {"...",
                 ".H.",
                 "..."},
                {"...",
                 ".H.",
                 "..."}
            }));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("lista")) {
            p.sendMessage(Component.text("Estructuras: " + String.join(", ", structs.keySet()), NamedTextColor.GOLD));
            p.sendMessage(Component.text("Uso: /estructura <nombre> (mira un bloque) | /estructura undo", NamedTextColor.GRAY));
            return true;
        }
        if (args[0].equalsIgnoreCase("undo")) {
            Deque<List<BlockState>> h = history.get(p.getUniqueId());
            if (h == null || h.isEmpty()) {
                p.sendMessage(Component.text("No hay nada que deshacer.", NamedTextColor.RED));
                return true;
            }
            for (BlockState bs : h.pop()) bs.update(true, false);
            p.sendMessage(Component.text("Deshecho.", NamedTextColor.GREEN));
            return true;
        }

        Struct s = structs.get(args[0].toLowerCase());
        if (s == null) {
            p.sendMessage(Component.text("Esa estructura no existe. Usa /estructura lista", NamedTextColor.RED));
            return true;
        }

        Block target = p.getTargetBlockExact(20);
        Block ground = target != null ? target : p.getLocation().getBlock().getRelative(0, -1, 0);

        place(p, s, ground);
        p.sendMessage(Component.text("Colocada: " + args[0], NamedTextColor.GREEN));
        return true;
    }

    private void place(Player p, Struct s, Block ground) {
        int w = 0, d = 0;
        for (String[] layer : s.layers()) {
            d = Math.max(d, layer.length);
            for (String row : layer) w = Math.max(w, row.length());
        }
        int offX = w / 2, offZ = d / 2;
        int baseY = ground.getY() + 1;

        List<BlockState> backup = new ArrayList<>();

        for (int y = 0; y < s.layers().length; y++) {
            String[] layer = s.layers()[y];
            for (int z = 0; z < layer.length; z++) {
                String row = layer[z];
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    if (c == '.') continue;
                    String data = s.legend().get(c);
                    if (data == null) continue;
                    Block b = ground.getWorld().getBlockAt(
                            ground.getX() + x - offX, baseY + y, ground.getZ() + z - offZ);
                    backup.add(b.getState());
                    b.setBlockData(Bukkit.createBlockData(data), false);
                }
            }
        }

        history.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>()).push(backup);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        List<String> out = new ArrayList<>(structs.keySet());
        out.add("lista");
        out.add("undo");
        out.removeIf(n -> !n.startsWith(args[0].toLowerCase()));
        return out;
    }
}
