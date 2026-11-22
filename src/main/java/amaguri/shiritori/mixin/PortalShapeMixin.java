package amaguri.shiritori.mixin;

import amaguri.shiritori.ShiritoriService;
import amaguri.shiritori.core.ShiritoriGame;
import amaguri.shiritori.gate.Gate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 火打石・ファイヤーチャージ・自然発火など、どの点火でも最終的に
 * PortalShape.createPortalBlocks で紫の膜が張られる。ここ1か所で判定する。
 */
@Mixin(PortalShape.class)
public abstract class PortalShapeMixin {
	@Shadow
	private BlockPos bottomLeft;

	@Inject(method = "createPortalBlocks", at = @At("HEAD"), cancellable = true)
	private void shiritori$gate(LevelAccessor accessor, CallbackInfo ci) {
		if (!(accessor instanceof ServerLevel level)) return;
		ShiritoriGame game = Gate.game(level.getServer());
		if (!game.canLightPortal()) {
			Player near = level.getNearestPlayer(bottomLeft.getX() + 0.5, bottomLeft.getY() + 0.5, bottomLeft.getZ() + 0.5, 16.0, false);
			if (near instanceof ServerPlayer sp) {
				sp.sendOverlayMessage(Component.literal("まだ「ね」に来ていない！").withStyle(net.minecraft.ChatFormatting.RED));
			}
			ci.cancel();
			return;
		}
		if (game.portalRegistersNow()) {
			Player near = level.getNearestPlayer(bottomLeft.getX() + 0.5, bottomLeft.getY() + 0.5, bottomLeft.getZ() + 0.5, 16.0, false);
			ShiritoriService.submitMilestone(level.getServer(), near instanceof ServerPlayer sp ? sp : null,
				ShiritoriGame.Milestone.NETHER_PORTAL);
		}
	}
}
