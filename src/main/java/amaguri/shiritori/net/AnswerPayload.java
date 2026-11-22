package amaguri.shiritori.net;

import amaguri.shiritori.ShiritoriMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: 手のアイテムで答える（フィールド無し）。ID shiritori:answer */
public record AnswerPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AnswerPayload> TYPE = new CustomPacketPayload.Type<>(ShiritoriMod.id("answer"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AnswerPayload> CODEC = StreamCodec.unit(new AnswerPayload());

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
