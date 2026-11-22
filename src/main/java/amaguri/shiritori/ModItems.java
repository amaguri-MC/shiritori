package amaguri.shiritori;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public final class ModItems {
	public static final ResourceKey<Item> SHIRITORI_STICK_KEY = ResourceKey.create(Registries.ITEM, ShiritoriMod.id("shiritori_stick"));
	/** しりとり棒（攻撃力は木の剣相当）。 */
	public static final Item SHIRITORI_STICK = Registry.register(BuiltInRegistries.ITEM, SHIRITORI_STICK_KEY,
		new Item(new Item.Properties().setId(SHIRITORI_STICK_KEY).sword(ToolMaterial.WOOD, 3.0F, -2.4F)));

	private ModItems() {}

	/** クラスを読み込んで登録を走らせる。 */
	public static void init() {}
}
