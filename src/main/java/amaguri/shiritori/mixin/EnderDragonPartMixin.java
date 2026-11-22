package amaguri.shiritori.mixin;

import amaguri.shiritori.gate.Gate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 部位が受けるダメージの入口（念のため部位側でも止める）。 */
@Mixin(EnderDragonPart.class)
public abstract class EnderDragonPartMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void shiritori$gate(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (Gate.game(level.getServer()).canHurtDragon()) return;
		if (source.getEntity() instanceof ServerPlayer sp && level.getGameTime() % 40 == 0) {
			sp.sendOverlayMessage(Component.literal("今の文字では倒せない！").withStyle(net.minecraft.ChatFormatting.RED));
		}
		cir.setReturnValue(false);
	}
}
