package com.mrbysco.itemframes.component;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mrbysco.itemframes.ItemFramePlugin;

public class ItemFrameComponent implements Component<EntityStore> {
	public static final BuilderCodec CODEC;
	private ItemStack heldStack;
	private Vector3i framePosition;

	public static ComponentType<EntityStore, ItemFrameComponent> getComponentType() {
		return ItemFramePlugin.get().getItemFrameComponent();
	}

	private ItemFrameComponent() {
	}

	public ItemFrameComponent(ItemStack itemStack, Vector3i position) {
		this.heldStack = itemStack;
		this.framePosition = position;
	}

	public ItemStack getHeldStack() {
		return this.heldStack;
	}

	public void setHeldStack(ItemStack heldStack) {
		this.heldStack = heldStack;
	}

	public Vector3i getFramePosition() {
		return framePosition;
	}

	public void setFramePosition(Vector3i framePosition) {
		this.framePosition = framePosition;
	}

	public Component<EntityStore> clone() {
		return new ItemFrameComponent(this.heldStack, this.framePosition);
	}

	static {
		CODEC = BuilderCodec.builder(ItemFrameComponent.class, ItemFrameComponent::new)
				.append(new KeyedCodec<>("HeldStack", ItemStack.CODEC),
						(component, stack) -> component.heldStack = stack,
						(component) -> component.heldStack).add()
				.append(new KeyedCodec<>("FramePosition", Vector3i.CODEC),
						(component, stack) -> component.framePosition = stack,
						(component) -> component.framePosition).add()
				.build();
	}
}