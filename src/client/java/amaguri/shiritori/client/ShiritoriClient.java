package amaguri.shiritori.client;

import amaguri.shiritori.client.hud.HudRenderer;
import amaguri.shiritori.net.AnswerPayload;
import amaguri.shiritori.net.StatePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class ShiritoriClient implements ClientModInitializer {
	private static KeyMapping answerKey;

	@Override
	public void onInitializeClient() {
		// カテゴリ「しりとり」= key.category.shiritori.main
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("shiritori", "main"));
		// キー名「しりとりで答える」= key.shiritori.answer（初期キー B）
		answerKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.shiritori.answer", InputConstants.KEY_B, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (answerKey.consumeClick()) {
				if (client.player != null) ClientPlayNetworking.send(new AnswerPayload());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(StatePayload.TYPE,
			(payload, context) -> context.client().execute(() -> HudRenderer.setState(payload)));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HudRenderer.clear());

		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
			Identifier.fromNamespaceAndPath("shiritori", "hud"),
			new HudRenderer(() -> answerKey.getTranslatedKeyMessage().getString()));
	}
}
