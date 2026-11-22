package amaguri.shiritori;

import amaguri.shiritori.core.ShiritoriGame;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** ワールドに1つ・全員共有のしりとり状態。 */
public class ShiritoriState extends SavedData {
	public static final Codec<ShiritoriState> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.STRING.optionalFieldOf("current", String.valueOf(ShiritoriGame.START)).forGetter(s -> String.valueOf(s.game.current())),
		Codec.STRING.listOf().optionalFieldOf("used_keys", List.of()).forGetter(s -> s.game.usedKeys()),
		Codec.STRING.listOf().optionalFieldOf("used_names", List.of()).forGetter(s -> s.game.usedNames()),
		Codec.STRING.listOf().optionalFieldOf("unlocked", List.of()).forGetter(s -> s.game.unlocked().stream().map(Enum::name).toList()),
		Codec.BOOL.optionalFieldOf("cleared", false).forGetter(s -> s.game.isCleared()),
		Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("icons", Map.of()).forGetter(s -> s.icons),
		Codec.STRING.listOf().optionalFieldOf("given", List.of()).forGetter(s -> new ArrayList<>(s.given))
	).apply(i, ShiritoriState::new));

	public static final SavedDataType<ShiritoriState> TYPE = new SavedDataType<>(
		ShiritoriMod.id("state"), ShiritoriState::new, CODEC, DataFixTypes.LEVEL);

	private ShiritoriGame game = new ShiritoriGame();
	private final Map<String, String> icons = new HashMap<>();
	private final Set<String> given = new HashSet<>();

	public ShiritoriState() {}

	private ShiritoriState(String current, List<String> keys, List<String> names, List<String> unlocked, boolean cleared, Map<String, String> icons, List<String> given) {
		List<ShiritoriGame.Milestone> ms = new ArrayList<>();
		for (String n : unlocked) {
			try { ms.add(ShiritoriGame.Milestone.valueOf(n)); } catch (IllegalArgumentException ignored) {}
		}
		char c = current.isEmpty() ? ShiritoriGame.START : current.charAt(0);
		this.game = new ShiritoriGame(c, keys, names, ms, cleared);
		this.icons.putAll(icons);
		this.given.addAll(given);
	}

	public static ShiritoriState get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public ShiritoriGame game() { return game; }

	/** /shiritori set 用：ゲームを差し替える。 */
	public void setGame(ShiritoriGame g) {
		this.game = g;
		this.icons.clear();
		setDirty();
	}

	public void putIcon(String key, String itemId) {
		icons.put(key, itemId);
		setDirty();
	}

	public String icon(String key) { return icons.getOrDefault(key, "minecraft:name_tag"); }

	public void clearIcons() {
		icons.clear();
		setDirty();
	}

	/** 初参加の棒配布済みか。まだなら true を返して記録する。 */
	public boolean markGiven(String uuid) {
		if (!given.add(uuid)) return false;
		setDirty();
		return true;
	}
}
