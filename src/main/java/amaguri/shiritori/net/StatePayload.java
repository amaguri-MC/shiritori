package amaguri.shiritori.net;

import amaguri.shiritori.ShiritoriMod;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C: しりとりの状態。ID shiritori:state */
public record StatePayload(String current, List<String> historyNames, List<String> historyIcons, List<String> unlocked, String nextGoal, boolean cleared) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<StatePayload> TYPE = new CustomPacketPayload.Type<>(ShiritoriMod.id("state"));
	private static final StreamCodec<RegistryFriendlyByteBuf, List<String>> STRINGS = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());
	public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, StatePayload::current,
		STRINGS, StatePayload::historyNames,
		STRINGS, StatePayload::historyIcons,
		STRINGS, StatePayload::unlocked,
		ByteBufCodecs.STRING_UTF8, StatePayload::nextGoal,
		ByteBufCodecs.BOOL, StatePayload::cleared,
		StatePayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
