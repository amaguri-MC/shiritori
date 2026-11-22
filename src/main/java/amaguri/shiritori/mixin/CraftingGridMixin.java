package amaguri.shiritori.mixin;

import amaguri.shiritori.gate.Gate;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 作業台(CraftingMenu)とインベントリ2x2(InventoryMenu)は、どちらも
 * CraftingMenu.slotChangedCraftingGrid で完成品スロットを更新する。
 * 更新後にロック中の完成品を空にして、クライアントにも空を送る。
 * 完成品スロットが空なので shift クリックでも取れない。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingGridMixin {
	@Inject(method = "slotChangedCraftingGrid", at = @At("RETURN"))
	private static void shiritori$blockLockedResult(AbstractContainerMenu menu, ServerLevel level, Player player,
			CraftingContainer craftSlots, ResultContainer resultSlots, RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
		ItemStack result = resultSlots.getItem(0);
		if (!Gate.isLockedCraft(level.getServer(), result)) return;
		ItemStack blocked = result.copy();
		resultSlots.setItem(0, ItemStack.EMPTY);
		menu.setRemoteSlot(0, ItemStack.EMPTY);
		if (player instanceof ServerPlayer sp) {
			sp.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
			Gate.lockedCraftMessage(sp, blocked);
		}
	}
}
