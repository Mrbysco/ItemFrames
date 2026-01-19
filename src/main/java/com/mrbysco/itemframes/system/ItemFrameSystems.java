package com.mrbysco.itemframes.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventItemMerging;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mrbysco.itemframes.ItemFramePlugin;
import com.mrbysco.itemframes.component.BoundEntityComponent;
import com.mrbysco.itemframes.component.ItemFrameComponent;
import com.mrbysco.itemframes.util.FrameUtil;
import com.mrbysco.itemframes.util.ItemHelper;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import java.util.UUID;

public class ItemFrameSystems {

	public static class PlaceSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

		public PlaceSystem() {
			super(PlaceBlockEvent.class);
		}

		@Override
		public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
		                   @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
		                   @Nonnull PlaceBlockEvent event) {
			World world = commandBuffer.getExternalData().getWorld();
			ItemStack stack = event.getItemInHand();
			String itemId = stack.getItemId();
			if (!FrameUtil.isItemFrame(itemId)) return;

			Vector3i targetBlock = event.getTargetBlock();
			Vector3d blockPos = targetBlock.toVector3d();
			RotationTuple rotationTuple = event.getRotation();

			Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
			blockPos.add(0.5, 0.25, 0.5);

			Vector3f rotation = new Vector3f();
			rotation.addRotationOnAxis(Axis.Y, rotationTuple.yaw().getDegrees() - 90);
			rotation.addRotationOnAxis(Axis.X, rotationTuple.pitch().getDegrees());

			holder.putComponent(ItemFrameComponent.getComponentType(), new ItemFrameComponent(null, targetBlock));

			UUID spawnedUUID = spawnItem(commandBuffer, holder, blockPos, rotation, rotationTuple.yaw().getDegrees());
			ItemFramePlugin.LOGGER.atInfo().log("Spawned Item Frame with UUID: " + spawnedUUID);
			if (spawnedUUID != null) {
				commandBuffer.run((entityStore) -> {
					int i = targetBlock.getX();
					int j = targetBlock.getZ();
					long indexChunk = ChunkUtil.indexChunkFromBlock(i, j);
					WorldChunk worldchunk = world.getChunk(indexChunk);
					var chunkRef = worldchunk.getBlockComponentEntity(targetBlock.x, targetBlock.y, targetBlock.z);
					if (chunkRef == null) {
						chunkRef = BlockModule.ensureBlockEntity(worldchunk, targetBlock.x, targetBlock.y, targetBlock.z);
					}

					if (chunkRef != null) {
						Store<ChunkStore> chunkstore2 = world.getChunkStore().getStore();
						chunkstore2.putComponent(chunkRef, ItemFramePlugin.get().getBoundEntityComponent(), new BoundEntityComponent(spawnedUUID));
					}
				});
			}
		}

		private UUID spawnItem(CommandBuffer<EntityStore> store, Holder<EntityStore> holder, Vector3d position, Vector3f rotation, int yawDegrees) {
			switch (yawDegrees) {
				case 0 -> position.add(0, 0, -0.4);
				case 90 -> position.add(-0.4, 0, 0);
				case 180 -> position.add(0, 0, 0.4);
				case 270 -> position.add(0.4, 0, 0);
			}

			holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, rotation));
			holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));

			holder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
			holder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE);
			holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
			UUID uuid = UUID.randomUUID();
			holder.putComponent(UUIDComponent.getComponentType(), new UUIDComponent(uuid));
			holder.ensureComponent(Interactable.getComponentType());

			Interactions interactions = new Interactions();
			interactions.setInteractionId(InteractionType.Use, "Interact_With_Item_Frame");
			interactions.setInteractionHint("server.interactionHints.interactItemFrame");
			holder.addComponent(Interactions.getComponentType(), interactions);

			store.addEntity(holder, AddReason.SPAWN);
			return uuid;
		}

		@NullableDecl
		@Override
		public Query<EntityStore> getQuery() {
			return PlayerRef.getComponentType();
		}
	}

	public static class ItemFrameTick extends EntityTickingSystem<EntityStore> {
		private int tickCounter = 0;


		@Override
		public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
			final int TICKS_PER_SECOND = 10;
			tickCounter += 1;
			if (tickCounter < TICKS_PER_SECOND) {
				return; // Only process once per second
			}
			tickCounter = 0;

			Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
			ItemFrameComponent component = store.getComponent(ref, ItemFrameComponent.getComponentType());
			if (component != null) {
				ItemStack held = component.getHeldStack();

				if (component.getFramePosition() == null) {
					commandBuffer.run((entityStore) -> {
						if (held != null) {
							ItemHelper.spawnItem(entityStore, component, ref);
						}
						store.removeEntity(ref, RemoveReason.REMOVE);
					});
				} else {
					World world = store.getExternalData().getWorld();
					BlockType blockType = world.getBlockType(component.getFramePosition());
					if (blockType == null || !FrameUtil.isItemFrame(blockType.getId())) {
						commandBuffer.run((entityStore) -> {
							if (held != null) {
								ItemHelper.spawnItem(entityStore, component, ref);
							}
							store.removeEntity(ref, RemoveReason.REMOVE);
						});
					}
				}
			}
		}

		@NullableDecl
		@Override
		public Query<EntityStore> getQuery() {
			return ItemFramePlugin.get().getItemFrameComponent();
		}
	}
}
