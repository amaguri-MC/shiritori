# mixin_check（apidump の javap で、各ターゲットクラス自身の宣言を確認）

| Mixin | ターゲットクラス | 対象メンバー | ターゲット自身の宣言 | 根拠（apidump） |
|---|---|---|---|---|
| CraftingGridMixin | world.inventory.CraftingMenu | `protected static slotChangedCraftingGrid(AbstractContainerMenu, ServerLevel, Player, CraftingContainer, ResultContainer, RecipeHolder<CraftingRecipe>)` @Inject RETURN（InventoryMenu もこれを共用） | 宣言あり | world/inventory.txt 1377 |
| CrafterBlockMixin | world.level.block.CrafterBlock | `public static getPotentialResults(ServerLevel, CraftingInput)` @Inject RETURN | 宣言あり | world/level/block.txt 2510 |
| PortalShapeMixin | world.level.portal.PortalShape | `public createPortalBlocks(LevelAccessor)` @Inject HEAD | 宣言あり | world/level/portal.txt 68 |
| PortalShapeMixin | world.level.portal.PortalShape | `private final BlockPos bottomLeft` @Shadow（final を外す必要は無い・読み取りのみ） | 宣言あり | world/level/portal.txt 53 |
| EnderDragonMixin | world.entity.boss.enderdragon.EnderDragon | `public hurt(ServerLevel, EnderDragonPart, DamageSource, float)` @Inject HEAD | 宣言あり | enderdragon.txt 48 付近 |
| EnderDragonMixin | EnderDragon | `public hurtServer(ServerLevel, DamageSource, float)` @Inject HEAD | 宣言あり | enderdragon.txt |
| EnderDragonMixin | EnderDragon | `protected tickDeath()` @Inject HEAD（`public int dragonDeathTime` を読む） | 宣言あり | enderdragon.txt |
| EnderDragonPartMixin | world.entity.boss.enderdragon.EnderDragonPart | `public final hurtServer(ServerLevel, DamageSource, float)` @Inject HEAD | 宣言あり | enderdragon.txt 2 行目付近 |

呼び出しのみ（Inject/Shadow ではない）: ServerPlayer.sendOverlayMessage（26.2 に displayClientMessage は無く、これに置換）、ServerPlayer.connection、AbstractContainerMenu.setRemoteSlot/incrementStateId/containerId、LivingEntity.getKillCredit。
