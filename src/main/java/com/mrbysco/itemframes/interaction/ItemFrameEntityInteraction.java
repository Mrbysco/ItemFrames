package com.mrbysco.itemframes.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mrbysco.itemframes.ItemFramePlugin;
import com.mrbysco.itemframes.component.ItemFrameComponent;
import com.mrbysco.itemframes.util.FrameUtil;
import com.mrbysco.itemframes.util.ItemHelper;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

public class ItemFrameEntityInteraction extends SimpleInstantInteraction {
	public static final BuilderCodec<ItemFrameEntityInteraction> CODEC = BuilderCodec.builder(
					ItemFrameEntityInteraction.class, ItemFrameEntityInteraction::new, SimpleInstantInteraction.CODEC
			)
			.documentation("Interacts with an Item Frame Entity")
			.build();

	@Nonnull
	@Override
	public WaitForDataFrom getWaitForDataFrom() {
		return super.getWaitForDataFrom();
	}


	@Override
	protected void firstRun(@NonNullDecl InteractionType type, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler handler) {
		CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
		World world = commandBuffer.getExternalData().getWorld();
		Ref<EntityStore> targetRef = context.getTargetEntity();
		if (targetRef == null) {
			context.getState().state = InteractionState.Failed;
			return;
		}
		ItemStack itemstack = context.getHeldItem();
		ItemFrameComponent frameComponent = commandBuffer.getComponent(targetRef, ItemFrameComponent.getComponentType());
		if (frameComponent != null) {
			Vector3i targetBlock = frameComponent.getFramePosition();
			int x = targetBlock.getX();
			int y = targetBlock.getY();
			int z = targetBlock.getZ();

			if (frameComponent.getHeldStack() == null) {
				if (itemstack == null || itemstack.isEmpty()) {
					context.getState().state = InteractionState.Failed;
				} else {
					ItemStack stackClone = itemstack.withQuantity(1);
					if (context.getHeldItemContainer() != null) {
						ItemStackSlotTransaction itemstackslottransaction = context.getHeldItemContainer()
								.removeItemStackFromSlot(context.getHeldItemSlot(), itemstack, 1);
						if (!itemstackslottransaction.succeeded()) {
							context.getState().state = InteractionState.Failed;
							return;
						}

						frameComponent.setHeldStack(stackClone);
						commandBuffer.run(entityStore -> {
							FrameUtil.remakeItemEntity(entityStore, targetRef, stackClone);
							world.performBlockUpdate(x, y, z);
						});
					}
				}
			} else {
				if (itemstack == null || itemstack.isEmpty()) {
					ItemHelper.spawnItem(commandBuffer, frameComponent, targetRef);
					frameComponent.setHeldStack(null);
					commandBuffer.run(entityStore -> {
						FrameUtil.remakeItemEntity(entityStore, targetRef, null);
						world.performBlockUpdate(x, y, z);
					});
				}
			}
		} else {
			context.getState().state = InteractionState.Failed;
			return;
		}

		ItemFramePlugin.LOGGER.atInfo().log("Interacted with Item Frame Entity: " + targetRef);

	}
}
