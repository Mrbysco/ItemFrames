package com.mrbysco.itemframes.util;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.AssetIconProperties;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mrbysco.itemframes.ItemFramePlugin;
import com.mrbysco.itemframes.component.BoundEntityComponent;
import com.mrbysco.itemframes.component.ItemFrameComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("UnusedReturnValue")
public class FrameUtil {
	/**
	 * The component type for item frame components (For mod compatibility)
	 */
	public static final Supplier<ComponentType<EntityStore, ItemFrameComponent>> ITEM_FRAME_COMPONENT = () ->
			ItemFramePlugin.get().getItemFrameComponent();
	/**
	 * List of item frame block IDs
	 */
	private static final List<String> itemFrameItems = List.of(
			"ItemFrames_Item_Frame",
			"ItemFrames_Item_Frame_Ancient",
			"ItemFrames_Item_Frame_Bamboo",
			"ItemFrames_Item_Frame_Kweebec",
			"ItemFrames_Item_Frame_Light",
			"ItemFrames_Item_Frame_Lumberjack",
			"ItemFrames_Item_Frame_Tavern"
	);

	/**
	 * Check if the given frame ID corresponds to an item frame
	 *
	 * @param frameId The frame ID to check
	 * @return True if the frame ID is an item frame, false otherwise
	 */
	public static boolean isItemFrame(String frameId) {
		return itemFrameItems.contains(frameId);
	}

	/**
	 * Get the item frame's entity store reference at the given position
	 *
	 * @param world The world
	 * @param pos   The position of the item frame block
	 * @return The entity store reference of the item frame, or null if not found
	 */
	public static Ref<EntityStore> getFrameEntity(World world, Vector3i pos) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		long indexChunk = ChunkUtil.indexChunkFromBlock(x, z);
		WorldChunk worldchunk = world.getChunk(indexChunk);
		var chunkRef = worldchunk.getBlockComponentEntity(x, y, z);
		if (chunkRef == null) {
			chunkRef = BlockModule.ensureBlockEntity(worldchunk, x, y, z);
		}

		BlockType blockType = worldchunk.getBlockType(pos);
		if (blockType == null) {
			return null;
		}

		if (chunkRef == null) {
			return null;
		}

		Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
		BoundEntityComponent boundEntityComponent = chunkStore.getComponent(chunkRef, ItemFramePlugin.get().getBoundEntityComponent());
		if (boundEntityComponent != null && FrameUtil.isItemFrame(blockType.getId()) && boundEntityComponent.getAttachedEntity() != null) {
			return world.getEntityRef(boundEntityComponent.getAttachedEntity());
		}
		return null;
	}

	/**
	 * Check if the block at the given position is an item frame
	 *
	 * @param world The world
	 * @param pos   The position of the block to check
	 * @return True if the block is an item frame, false otherwise
	 */
	public static boolean isFrameBlock(World world, Vector3i pos) {
		Ref<EntityStore> frameRef = getFrameEntity(world, pos);
		return frameRef != null;
	}

	/**
	 * Set the item in the item frame at the given position
	 *
	 * @param world     The world
	 * @param pos       The position of the item frame block
	 * @param stack     The item stack to set in the item frame
	 * @param overwrite Whether to overwrite the existing item if present
	 * @return True if the item was set successfully, false otherwise
	 */
	public static boolean setFrameItem(World world, Vector3i pos, @Nullable ItemStack stack, boolean overwrite) {
		if (!isFrameBlock(world, pos)) return false;

		Ref<EntityStore> frameRef = getFrameEntity(world, pos);
		if (frameRef == null) {
			return false;
		}

		Store<EntityStore> store = world.getEntityStore().getStore();
		ItemFrameComponent frameComponent = store.getComponent(frameRef, ItemFrameComponent.getComponentType());
		if (frameComponent == null) {
			return false;
		}

		if (frameComponent.getHeldStack() == null || overwrite) {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			frameComponent.setHeldStack(stack);
			FrameUtil.remakeItemEntity(store, frameRef, stack);
			world.performBlockUpdate(x, y, z);
			return true;
		}
		return false;
	}

	/**
	 * Get the item in the item frame at the given position
	 *
	 * @param world The world
	 * @param pos   The position of the item frame block
	 * @return The item stack in the item frame, or null if not found
	 */
	public static ItemStack getFrameItem(World world, Vector3i pos) {
		if (!isFrameBlock(world, pos)) return null;

		Ref<EntityStore> frameRef = getFrameEntity(world, pos);
		if (frameRef == null) {
			return null;
		}

		Store<EntityStore> store = world.getEntityStore().getStore();
		ItemFrameComponent frameComponent = store.getComponent(frameRef, ItemFrameComponent.getComponentType());
		if (frameComponent == null) {
			return null;
		}

		return frameComponent.getHeldStack();
	}

	/**
	 * Remake the item entity for the given item stack
	 *
	 * @param store  the entity store
	 * @param oldRef the old entity reference
	 * @param stack  the item stack to set
	 * @return the new entity reference
	 */
	public static Ref<EntityStore> remakeItemEntity(
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> oldRef,
			@Nullable ItemStack stack
	) {
		// Remove existing variant-specific components
		removeVariantComponents(store, oldRef);

		// Copy the existing entity (includes transform, network, uuid, flags, etc.)
		Holder<EntityStore> holder = store.copyEntity(oldRef);

		float scale = 0.5F;

		if (stack != null) {
			ItemStack newStack = new ItemStack(stack.getItemId(), 1);
			Item item = newStack.getItem();
			AssetIconProperties properties = item.getIconProperties();
			if (properties != null) {
				scale = properties.getScale();
			}

			newStack.setOverrideDroppedItemAnimation(true);
			holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(newStack));

			Model model = getItemModel(item);
			if (model != null) {
				String modelId = getItemModelId(item);
				if (modelId != null) {
					holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
					holder.addComponent(
							PersistentModel.getComponentType(),
							new PersistentModel(new Model.ModelReference(modelId, scale, null, true))
					);
				}
			} else if (item.hasBlockType()) {
				holder.addComponent(BlockEntity.getComponentType(), new BlockEntity(newStack.getItemId()));
				holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(scale * 2.0F));
			} else {
				holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(scale));
			}
		}

		// Remove old entity AFTER copying
		store.removeEntity(oldRef, RemoveReason.REMOVE);

		// Respawn cleanly
		return store.addEntity(holder, AddReason.SPAWN);
	}

	public static void removeVariantComponents(Store<EntityStore> store, Ref<EntityStore> ref) {
		// variant-specific visuals/identity
		store.removeComponentIfExists(ref, ModelComponent.getComponentType());
		store.removeComponentIfExists(ref, PersistentModel.getComponentType());
		store.removeComponentIfExists(ref, BlockEntity.getComponentType());
		// scaling / network identity
		store.removeComponentIfExists(ref, EntityScaleComponent.getComponentType());

		// Remove item component last
		store.removeComponentIfExists(ref, ItemComponent.getComponentType());
	}

	@Nullable
	public static String getItemModelId(@Nonnull Item item) {
		String s = item.getModel();
		if (s == null && item.hasBlockType()) {
			BlockType blocktype = BlockType.getAssetMap().getAsset(item.getId());
			if (blocktype != null && blocktype.getCustomModel() != null) {
				s = blocktype.getCustomModel();
			}
		}

		return s;
	}

	@Nullable
	public static Model getItemModel(@Nonnull Item item) {
		String s = getItemModelId(item);
		if (s == null) {
			return null;
		} else {
			ModelAsset modelasset = ModelAsset.getAssetMap().getAsset(s);
			return modelasset != null ? Model.createStaticScaledModel(modelasset, 0.5F) : null;
		}
	}
}
