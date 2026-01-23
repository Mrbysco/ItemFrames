package com.mrbysco.itemframes.component;

import com.hypixel.hytale.codec.Codec;
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
	private int frameRotation = 0;

	public static ComponentType<EntityStore, ItemFrameComponent> getComponentType() {
		return ItemFramePlugin.get().getItemFrameComponent();
	}

	private ItemFrameComponent() {
	}

	public ItemFrameComponent(ItemStack itemStack, Vector3i position, int rotation) {
		this.heldStack = itemStack;
		this.framePosition = position;
		this.frameRotation = rotation;
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

	public int getFrameRotation() {
		return frameRotation;
	}

	public void setFrameRotation(int frameRotation) {
		this.frameRotation = frameRotation;
	}

	public Component<EntityStore> clone() {
		return new ItemFrameComponent(this.heldStack, this.framePosition, this.frameRotation);
	}

	static {
		CODEC = BuilderCodec.builder(ItemFrameComponent.class, ItemFrameComponent::new)
				.append(new KeyedCodec<>("HeldStack", ItemStack.CODEC),
						(component, stack) -> component.heldStack = stack,
						(component) -> component.heldStack).add()
				.append(new KeyedCodec<>("FramePosition", Vector3i.CODEC),
						(component, pos) -> component.framePosition = pos,
						(component) -> component.framePosition).add()
				.append(new KeyedCodec<>("FrameRotation", Codec.INTEGER),
						(component, rot) -> component.frameRotation = rot,
						(component) -> component.frameRotation).add()
				.build();
	}
}