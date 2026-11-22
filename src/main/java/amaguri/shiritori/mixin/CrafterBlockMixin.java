package amaguri.shiritori.mixin;

import amaguri.shiritori.gate.Gate;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** クラフターのレシピ検索結果が、ロック中の完成品なら「レシピ無し」にする。 */
@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {
	@Inject(method = "getPotentialResults", at = @At("RETURN"), cancellable = true)
	private static void shiritori$blockLockedResult(ServerLevel level, CraftingInput input,
			CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> cir) {
		Optional<RecipeHolder<CraftingRecipe>> found = cir.getReturnValue();
		if (found == null || found.isEmpty()) return;
		ItemStack result = found.get().value().assemble(input);
		if (Gate.isLockedCraft(level.getServer(), result)) {
			cir.setReturnValue(Optional.empty());
		}
	}
}
