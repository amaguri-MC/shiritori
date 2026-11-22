package amaguri.shiritori;

import amaguri.shiritori.core.ShiritoriGame;
import amaguri.shiritori.core.ShiritoriGame.Milestone;
import amaguri.shiritori.core.ShiritoriGame.Result;
import amaguri.shiritori.net.StatePayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;

/** 登録・ペナルティ・メッセージ・同期をまとめて行う。サーバースレッドから呼ぶこと。 */
public final class ShiritoriService {
	public static final String FALLBACK_ICON = "minecraft:name_tag";
	public static final int PENALTY_TICKS = 200;

	private ShiritoriService() {}

	/** プレイヤーが出した言葉（アイテム/モブの翻訳キー）を登録する。 */
	public static ShiritoriGame.Result submitWord(ServerPlayer player, String key, String iconItemId) {
		MinecraftServer server = player.level().getServer();
		ShiritoriState state = ShiritoriState.get(server);
		ShiritoriGame game = state.game();

		Readings.Entry entry = Readings.get(key);
		if (entry == null) {
			cannotUse(player, Component.translatable(key));
			return Result.INVALID;
		}
		char before = game.current();
		Result result = game.submit(key, entry.name(), entry.reading());
		state.setDirty();
		switch (result) {
			case OK -> {
				state.putIcon(key, iconItemId);
				player.sendSystemMessage(Component.literal(entry.name() + "（" + entry.reading() + "）を登録！ 次は「" + game.current() + "」").withStyle(ChatFormatting.GREEN), true);
				broadcast(server, chatLine(player, entry.name() + " → 次は「" + game.current() + "」"));
			}
			case CLEARED -> {
				state.putIcon(key, iconItemId);
				broadcast(server, chatLine(player, entry.name() + " → しりとりクリア！"));
				titleAll(server, Component.literal("しりとりクリア！").withStyle(ChatFormatting.GOLD), Component.empty());
			}
			case WRONG_HEAD -> {
				player.sendSystemMessage(Component.literal("頭の文字が違う！（今は「" + before + "」）ウィザー！").withStyle(ChatFormatting.RED), true);
				wither(player);
			}
			case ALREADY_USED -> {
				player.sendSystemMessage(Component.literal(entry.name() + "はもう使った！ ウィザー！").withStyle(ChatFormatting.RED), true);
				wither(player);
			}
			case GAME_OVER -> {
				state.clearIcons();
				broadcast(server, chatLine(player, entry.name() + " → ゲームオーバー"));
				titleAll(server, Component.literal("ゲームオーバー").withStyle(ChatFormatting.RED),
					Component.literal("『ん』で終わった…『" + ShiritoriGame.START + "』からやり直し"));
			}
			case INVALID -> cannotUse(player, Component.literal(entry.name()));
		}
		if (result == Result.OK || result == Result.CLEARED || result == Result.GAME_OVER) sync(server);
		return result;
	}

	/** ネザーゲート点火・ドラゴン撃破など。playerOrNull はきっかけを作った人（居なくてよい）。 */
	public static void submitMilestone(MinecraftServer server, ServerPlayer playerOrNull, Milestone m) {
		ShiritoriState state = ShiritoriState.get(server);
		ShiritoriGame game = state.game();
		Result result = game.submit(m);
		state.setDirty();
		String who = playerOrNull == null ? null : playerOrNull.getName().getString();
		switch (result) {
			case OK -> {
				state.putIcon(m.key, milestoneIcon(m));
				if (playerOrNull != null) {
					playerOrNull.sendSystemMessage(Component.literal(m.display + "（" + m.reading + "）を登録！ 次は「" + game.current() + "」").withStyle(ChatFormatting.GREEN), true);
				}
				broadcast(server, chatLine(who, m.display + " → 次は「" + game.current() + "」"));
			}
			case CLEARED -> {
				state.putIcon(m.key, milestoneIcon(m));
				broadcast(server, chatLine(who, m.display + " → しりとりクリア！"));
				titleAll(server, Component.literal("しりとりクリア！").withStyle(ChatFormatting.GOLD), Component.empty());
			}
			case GAME_OVER -> {
				state.clearIcons();
				broadcast(server, chatLine(who, m.display + " → ゲームオーバー"));
				titleAll(server, Component.literal("ゲームオーバー").withStyle(ChatFormatting.RED),
					Component.literal("『ん』で終わった…『" + ShiritoriGame.START + "』からやり直し"));
			}
			default -> { /* 頭違い・使用済み・無効は何もしない（関門側が事前に判定する） */ }
		}
		if (result == Result.OK || result == Result.CLEARED || result == Result.GAME_OVER) sync(server);
	}

	/** 全員へ現在の状態を送る。 */
	public static void sync(MinecraftServer server) {
		StatePayload payload = payload(ShiritoriState.get(server));
		for (ServerPlayer p : PlayerLookup.all(server)) ServerPlayNetworking.send(p, payload);
	}

	/** 1人へ現在の状態を送る。 */
	public static void sync(ServerPlayer player) {
		ServerPlayNetworking.send(player, payload(ShiritoriState.get(player.level().getServer())));
	}

	/** 保存されている状態から StatePayload を作る。 */
	public static StatePayload payload(ShiritoriState state) {
		ShiritoriGame g = state.game();
		List<String> keys = g.usedKeys();
		List<String> names = g.usedNames();
		int from = Math.max(0, keys.size() - 3);
		List<String> hn = new ArrayList<>();
		List<String> hi = new ArrayList<>();
		for (int i = from; i < keys.size(); i++) {
			hn.add(names.get(i));
			hi.add(state.icon(keys.get(i)));
		}
		List<String> unlocked = g.unlocked().stream().map(Enum::name).toList();
		Milestone next = g.nextGoal();
		return new StatePayload(String.valueOf(g.current()), hn, hi, unlocked, next == null ? "" : next.name(), g.isCleared());
	}

	/** モブのアイコン: スポーンエッグがあればそれ、無ければ名札。 */
	public static String mobIcon(EntityType<?> type) {
		Identifier id = EntityType.getKey(type);
		Identifier egg = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_spawn_egg");
		return BuiltInRegistries.ITEM.containsKey(egg) ? egg.toString() : FALLBACK_ICON;
	}

	private static String milestoneIcon(Milestone m) {
		return switch (m) {
			case NETHER_PORTAL -> "minecraft:obsidian";
			case DRAGON -> "minecraft:dragon_head";
			case BLAZE_POWDER -> "minecraft:blaze_powder";
			case ENDER_EYE -> "minecraft:ender_eye";
		};
	}

	private static void cannotUse(ServerPlayer player, Component name) {
		player.sendSystemMessage(Component.empty().append(name).append("はしりとりに使えない").withStyle(ChatFormatting.GRAY), true);
	}

	private static void wither(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.WITHER, PENALTY_TICKS, 0));
	}

	private static Component chatLine(ServerPlayer player, String text) {
		return Component.literal("<").append(player.getDisplayName()).append("> " + text);
	}

	private static Component chatLine(String whoOrNull, String text) {
		return Component.literal("<" + (whoOrNull == null ? "しりとり" : whoOrNull) + "> " + text);
	}

	private static void broadcast(MinecraftServer server, Component message) {
		server.getPlayerList().broadcastSystemMessage(message, false);
	}

	private static void titleAll(MinecraftServer server, Component title, Component subtitle) {
		for (ServerPlayer p : PlayerLookup.all(server)) {
			p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
			p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
			p.connection.send(new ClientboundSetTitleTextPacket(title));
		}
	}
}
