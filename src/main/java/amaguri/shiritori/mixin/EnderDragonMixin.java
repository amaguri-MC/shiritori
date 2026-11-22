package amaguri.shiritori.mixin;

import amaguri.shiritori.ShiritoriService;
import amaguri.shiritori.core.ShiritoriGame;
import amaguri.shiritori.gate.Gate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
	/** 部位経由のダメージ（EnderDragonPart.hurtServer もここに来る）。 */
	@Inject(method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
		at = @At("HEAD"), cancellable = true)
	private void shiritori$gatePart(ServerLevel level, EnderDragonPart part, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (shiritori$blocked(level, source)) cir.setReturnValue(false);
	}

	/** 本体へ直接のダメージ。 */
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void shiritori$gateBody(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (shiritori$blocked(level, source)) cir.setReturnValue(false);
	}

	/** 死亡演出の最初の1tickだけ（dragonDeathTime はこの後に増える）。 */
	@Inject(method = "tickDeath", at = @At("HEAD"))
	private void shiritori$onDeath(CallbackInfo ci) {
		EnderDragon self = (EnderDragon) (Object) this;
		if (self.dragonDeathTime != 0) return;
		if (!(self.level() instanceof ServerLevel level)) return;
		LivingEntity killer = self.getKillCredit();
		ShiritoriService.submitMilestone(level.getServer(), killer instanceof ServerPlayer sp ? sp : null,
			ShiritoriGame.Milestone.DRAGON);
	}

	private static boolean shiritori$blocked(ServerLevel level, DamageSource source) {
		if (Gate.game(level.getServer()).canHurtDragon()) return false;
		if (source.getEntity() instanceof ServerPlayer sp && level.getGameTime() % 40 == 0) {
			sp.sendOverlayMessage(Component.literal("今の文字では倒せない！").withStyle(net.minecraft.ChatFormatting.RED));
		}
		return true;
	}
}
