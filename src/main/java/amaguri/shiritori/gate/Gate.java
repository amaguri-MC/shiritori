package amaguri.shiritori.gate;

import amaguri.shiritori.ShiritoriState;
import amaguri.shiritori.core.ShiritoriGame;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 関門の判定まわりの共通処理（Mixin から呼ぶ）。 */
public final class Gate {
	private Gate() {}

	public static ShiritoriGame game(MinecraftServer server) {
		return ShiritoriState.get(server).game();
	}

	/** 完成品がロック中のアイテムか（レシピIDではなくアイテム種類で判定）。 */
	public static boolean isLockedCraft(MinecraftServer server, ItemStack result) {
		if (server == null || result == null || result.isEmpty()) return false;
		ShiritoriGame g = game(server);
		if (result.getItem() == Items.BLAZE_POWDER) return !g.canCraftBlazePowder();
		if (result.getItem() == Items.ENDER_EYE) return !g.canCraftEnderEye();
		return false;
	}

	public static void lockedCraftMessage(ServerPlayer player, ItemStack result) {
		char head = result.getItem() == Items.BLAZE_POWDER
			? ShiritoriGame.Milestone.BLAZE_POWDER.head : ShiritoriGame.Milestone.ENDER_EYE.head;
		player.sendOverlayMessage(Component.literal("まだ「" + head + "」に来ていない！ まだ作れない").withStyle(net.minecraft.ChatFormatting.RED));
	}
}
