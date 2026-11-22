# C が使った 26.2 API（apidump で確認済み）
確認元: /home/claude/apidump の minecraft/ minecraft-client/ fabric/

| API | 出典 |
|---|---|
| KeyMapping(String, int, KeyMapping$Category) / consumeClick() / getTranslatedKeyMessage() | minecraft-client/net/minecraft/client.txt |
| KeyMapping$Category.register(Identifier) | 同上 |
| InputConstants.KEY_B | minecraft-client/com/mojang/blaze3d/platform.txt |
| KeyMappingHelper.registerKeyMapping(KeyMapping) | fabric/.../client/keymapping/v1.txt |
| ClientTickEvents.END_CLIENT_TICK | fabric/.../client/event/lifecycle/v1.txt |
| ClientPlayNetworking.registerGlobalReceiver(Type, PlayPayloadHandler) / send(CustomPacketPayload) / Context.client() | fabric/.../client/networking/v1.txt |
| ClientPlayConnectionEvents.DISCONNECT | 同上 |
| HudElement.extractRenderState(GuiGraphicsExtractor, DeltaTracker) / HudElementRegistry.attachElementAfter(Identifier, Identifier, HudElement) / VanillaHudElements.MISC_OVERLAYS | fabric/.../client/rendering/v1/hud.txt |
| GuiGraphicsExtractor: text(Font,String,int,int,int,boolean) / fill(int,int,int,int,int) / fakeItem(ItemStack,int,int) / pose() / guiHeight() | minecraft-client/net/minecraft/client/gui.txt |
| TooltipRenderUtil.extractTooltipBackground(GuiGraphicsExtractor,int,int,int,int,Identifier) と PADDING_LEFT/RIGHT/TOP/BOTTOM | minecraft-client/.../gui/screens/inventory/tooltip.txt |
| Minecraft.getInstance() / font / gui / player / getDebugOverlay() | minecraft-client/net/minecraft/client.txt |
| Gui.hud（public final Hud）/ Hud.isHidden()（F1） | minecraft-client/net/minecraft/client/gui.txt |
| DebugScreenOverlay.showDebugScreen()（F3） | minecraft-client/net/minecraft/client/gui/components.txt |
| Font.width(String) | minecraft-client/net/minecraft/client/gui.txt |
| BuiltInRegistries.ITEM.getValue(Identifier) / Identifier.tryParse / fromNamespaceAndPath | minecraft/net/minecraft/core/registries.txt, core.txt, resources.txt |
| ItemStack(ItemLike) / isEmpty() | minecraft/net/minecraft/world/item.txt |

## apidump に載っていないもの
- pose() が返す org.joml.Matrix3x2fStack の pushMatrix / popMatrix / translate / scale（JOML 標準API。dump は MC 本体のみ）
- amaguri.shiritori.net.StatePayload.TYPE / new AnswerPayload()（S 担当が作る。CONTRACT に static TYPE の記載なし → S に「TYPE 定数を public static final で」と要確認）
- TooltipRenderUtil の最後の引数 Identifier に null（バニラ既定スタイル）を渡している
