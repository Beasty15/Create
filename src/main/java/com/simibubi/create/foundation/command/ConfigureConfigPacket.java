package com.simibubi.create.foundation.command;

import com.simibubi.create.content.contraptions.goggles.GoggleConfigScreen;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.render.backend.FastRenderDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfigureConfigPacket extends SimplePacketBase {

	private final String option;
	private final String value;

	public ConfigureConfigPacket(String option, String value) {
		this.option = option;
		this.value = value;
	}

	public ConfigureConfigPacket(PacketBuffer buffer) {
		this.option = buffer.readString(32767);
		this.value = buffer.readString(32767);
	}

	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeString(option);
		buffer.writeString(value);
	}

	@Override
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get()
			.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
				try {
					Actions.valueOf(option)
						.performAction(value);
				} catch (IllegalArgumentException e) {
					LogManager.getLogger()
						.warn("Received ConfigureConfigPacket with invalid Option: " + option);
				}
			}));

		ctx.get()
			.setPacketHandled(true);
	}

	enum Actions {
		rainbowDebug(() -> Actions::rainbowDebug),
		overlayScreen(() -> Actions::overlayScreen),
		fixLighting(() -> Actions::experimentalLighting),
		overlayReset(() -> Actions::overlayReset),
		experimentalRendering(() -> Actions::experimentalRendering),

		;

		private final Supplier<Consumer<String>> consumer;

		Actions(Supplier<Consumer<String>> action) {
			this.consumer = action;
		}

		void performAction(String value) {
			consumer.get()
				.accept(value);
		}

		@OnlyIn(Dist.CLIENT)
		private static void rainbowDebug(String value) {
			ClientPlayerEntity player = Minecraft.getInstance().player;
			if (player == null || "".equals(value)) return;

			if (value.equals("info")) {
				ITextComponent text = new StringTextComponent("Rainbow Debug Utility is currently: ").appendSibling(boolToText(AllConfigs.CLIENT.rainbowDebug.get()));
				player.sendStatusMessage(text, false);
				return;
			}

			AllConfigs.CLIENT.rainbowDebug.set(Boolean.parseBoolean(value));
			ITextComponent text = boolToText(AllConfigs.CLIENT.rainbowDebug.get()).appendSibling(new StringTextComponent(" Rainbow Debug Utility").applyTextStyle(TextFormatting.WHITE));
			player.sendStatusMessage(text, false);
		}

		@OnlyIn(Dist.CLIENT)
		private static void experimentalRendering(String value) {
			ClientPlayerEntity player = Minecraft.getInstance().player;
			if (player == null || "".equals(value)) return;

			if (value.equals("info")) {
				ITextComponent text = new StringTextComponent("Experimental Rendering is currently: ").appendSibling(boolToText(AllConfigs.CLIENT.experimentalRendering.get()));
				player.sendStatusMessage(text, false);
				return;
			}

			AllConfigs.CLIENT.experimentalRendering.set(Boolean.parseBoolean(value));
			ITextComponent text = boolToText(AllConfigs.CLIENT.experimentalRendering.get()).appendSibling(new StringTextComponent(" Experimental Rendering").applyTextStyle(TextFormatting.WHITE));
			player.sendStatusMessage(text, false);

			FastRenderDispatcher.refresh();
		}

		@OnlyIn(Dist.CLIENT)
		private static void overlayReset(String value) {
			AllConfigs.CLIENT.overlayOffsetX.set(0);
			AllConfigs.CLIENT.overlayOffsetY.set(0);
		}

		@OnlyIn(Dist.CLIENT)
		private static void overlayScreen(String value) {
			ScreenOpener.open(new GoggleConfigScreen());
		}

		@OnlyIn(Dist.CLIENT)
		private static void experimentalLighting(String value) {
			ForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.set(true);
			Minecraft.getInstance().worldRenderer.loadRenderers();
		}

		private static ITextComponent boolToText(boolean b) {
			return b
					? new StringTextComponent("enabled").applyTextStyle(TextFormatting.DARK_GREEN)
					: new StringTextComponent("disabled").applyTextStyle(TextFormatting.RED);
		}
	}
}
