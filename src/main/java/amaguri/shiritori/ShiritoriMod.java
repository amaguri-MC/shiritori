package amaguri.shiritori;

import amaguri.shiritori.core.ShiritoriGame;
import amaguri.shiritori.net.AnswerPayload;
import amaguri.shiritori.net.StatePayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShiritoriMod implements ModInitializer {
	public static final String MOD_ID = "shiritori";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.init();

		// 通信
		PayloadTypeRegistry.serverboundPlay().register(AnswerPayload.TYPE, AnswerPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE, StatePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(AnswerPayload.TYPE, (payload, context) -> onAnswer(context.player()));

		// ログイン時：初参加なら棒を配って状態を同期
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (ShiritoriState.get(server).markGiven(player.getUUID().toString())) {
				ItemStack stick = new ItemStack(ModItems.SHIRITORI_STICK, 1);
				if (!player.getInventory().add(stick)) player.drop(stick, false);
			}
			ShiritoriService.sync(player);
		});

		// しりとり棒の直接攻撃でのとどめ
		ServerLivingEntityEvents.AFTER_DEATH.register(ShiritoriMod::onDeath);

		// クリエイティブタブ（戦闘）に追加
		ResourceKey<CreativeModeTab> combat = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("combat"));
		CreativeModeTabEvents.modifyOutputEvent(combat).register(output -> output.prepend(ModItems.SHIRITORI_STICK));

		// テスト用コマンド
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
			Commands.literal("shiritori")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("reset").executes(ctx -> {
					MinecraftServer server = ctx.getSource().getServer();
					ShiritoriState state = ShiritoriState.get(server);
					state.setGame(new ShiritoriGame());
					ShiritoriService.sync(server);
					ctx.getSource().sendSuccess(() -> Component.literal("しりとりをリセットした（「" + ShiritoriGame.START + "」から）"), true);
					return 1;
				}))
				.then(Commands.literal("set").then(Commands.argument("char", StringArgumentType.greedyString()).executes(ctx -> {
					String s = StringArgumentType.getString(ctx, "char");
					if (s.isEmpty()) return 0;
					MinecraftServer server = ctx.getSource().getServer();
					ShiritoriState state = ShiritoriState.get(server);
					state.setGame(new ShiritoriGame(s.charAt(0), List.of(), List.of(), List.of(), false));
					ShiritoriService.sync(server);
					ctx.getSource().sendSuccess(() -> Component.literal("今の文字を「" + s.charAt(0) + "」にした"), true);
					return 1;
				})))
				.then(Commands.literal("status").executes(ctx -> {
					ShiritoriGame g = ShiritoriState.get(ctx.getSource().getServer()).game();
					String next = g.nextGoal() == null ? "なし" : g.nextGoal().display + "（頭「" + g.nextGoal().head + "」）";
					String text = "今の文字「" + g.current() + "」 使用 " + g.usedKeys().size() + " 語 / 解禁 " + g.unlocked()
						+ " / 次の関門 " + next + (g.isCleared() ? " / クリア済み" : "");
					ctx.getSource().sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.YELLOW), false);
					return 1;
				}))));
	}

	/** Bキー：メインハンドのアイテムで答える。 */
	private static void onAnswer(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty()) return;
		Item item = stack.getItem();
		if (item == ModItems.SHIRITORI_STICK) {
			player.sendSystemMessage(Component.literal("しりとり棒はしりとりに使えない").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		ShiritoriService.submitWord(player, item.getDescriptionId(), BuiltInRegistries.ITEM.getKey(item).toString());
	}

	/** 攻撃者が ServerPlayer・直接攻撃・メインハンドがしりとり棒ならモブ名で登録する。 */
	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (entity.level().isClientSide()) return;
		if (!(source.getEntity() instanceof ServerPlayer player)) return;
		if (source.getDirectEntity() != player) return;
		if (player.getMainHandItem().getItem() != ModItems.SHIRITORI_STICK) return;
		if (entity instanceof Player) return;
		EntityType<?> type = entity.getType();
		if (type == net.minecraft.world.entity.EntityTypes.ENDER_DRAGON) return; // ドラゴンは関門側が登録する
		ShiritoriService.submitWord(player, type.getDescriptionId(), ShiritoriService.mobIcon(type));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
