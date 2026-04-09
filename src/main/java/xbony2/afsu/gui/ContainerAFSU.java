package xbony2.afsu.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import xbony2.afsu.tileentity.TileEntityAFSU;

public class ContainerAFSU extends Container {
	private final TileEntityAFSU tile;
	private int cachedEnergyLow = -1;
	private int cachedEnergyHigh = -1;
	private int cachedRedstoneMode = -1;

	public ContainerAFSU(InventoryPlayer playerInventory, TileEntityAFSU tile) {
		this.tile = tile;
		final int inventoryTop = 114;
		final int hotbarTop = 172;
		final EntityEquipmentSlot[] armorOrder = new EntityEquipmentSlot[] {
			EntityEquipmentSlot.FEET, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.HEAD
		};

		for (int i = 0; i < armorOrder.length; i++) {
			final EntityEquipmentSlot armorSlot = armorOrder[i];
			final int slotIndex = 36 + i;
			this.addSlotToContainer(new Slot(playerInventory, slotIndex, 8 + i * 18, 84) {
				@Override
				public int getSlotStackLimit() {
					return 1;
				}

				@Override
				public boolean isItemValid(ItemStack stack) {
					return stack.getItem().isValidArmor(stack, armorSlot, playerInventory.player);
				}

				@Override
				public String getSlotTexture() {
					switch (armorSlot) {
						case FEET:
							return "minecraft:items/empty_armor_slot_boots";
						case LEGS:
							return "minecraft:items/empty_armor_slot_leggings";
						case CHEST:
							return "minecraft:items/empty_armor_slot_chestplate";
						case HEAD:
							return "minecraft:items/empty_armor_slot_helmet";
						default:
							return super.getSlotTexture();
					}
				}
			});
		}

		// Charge-from-AFSU slot (top)
		this.addSlotToContainer(new Slot(tile, 0, 56, 17));
		// Discharge-into-AFSU slot (bottom)
		this.addSlotToContainer(new Slot(tile, 1, 56, 53));

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, inventoryTop + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, hotbarTop));
		}
	}

	public TileEntityAFSU getTile() {
		return this.tile;
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return this.tile.isUsableByPlayer(playerIn);
	}

	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		int energy = this.tile.getStoredInt();
		int low = energy & 0xFFFF;
		int high = (energy >>> 16) & 0xFFFF;
		int redstoneMode = this.tile.getRedstoneMode();
		for (IContainerListener listener : this.listeners) {
			if (low != this.cachedEnergyLow) listener.sendWindowProperty(this, 0, low);
			if (high != this.cachedEnergyHigh) listener.sendWindowProperty(this, 1, high);
			if (redstoneMode != this.cachedRedstoneMode) listener.sendWindowProperty(this, 2, redstoneMode);
		}
		this.cachedEnergyLow = low;
		this.cachedEnergyHigh = high;
		this.cachedRedstoneMode = redstoneMode;
	}

	@Override
	public void updateProgressBar(int id, int data) {
		super.updateProgressBar(id, data);
		if (id == 0) this.tile.setField(0, data);
		if (id == 1) this.tile.setField(1, data);
		if (id == 2) this.tile.setField(2, data);
	}

	@Override
	public boolean enchantItem(EntityPlayer playerIn, int id) {
		if (id == 0) {
			this.tile.cycleRedstoneMode();
			detectAndSendChanges();
			return true;
		}
		return false;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
		Slot slot = this.inventorySlots.get(index);
		if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;

		ItemStack stackInSlot = slot.getStack();
		ItemStack original = stackInSlot.copy();
		boolean fromMachine = index >= 4 && index < 6;

		if (fromMachine) {
			if (!this.mergeItemStack(stackInSlot, 6, this.inventorySlots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (index < 4) {
				if (!this.mergeItemStack(stackInSlot, 6, this.inventorySlots.size(), false)) {
					return ItemStack.EMPTY;
				}
				if (stackInSlot.isEmpty()) {
					slot.putStack(ItemStack.EMPTY);
				} else {
					slot.onSlotChanged();
				}
				slot.onTake(playerIn, stackInSlot);
				return original;
			}

			boolean canCharge = this.tile.canChargeItem(stackInSlot);
			boolean canDischarge = this.tile.canDischargeItem(stackInSlot);
			if (!canCharge && !canDischarge) {
				return ItemStack.EMPTY;
			}

			// Match MFSU behavior: higher charge prefers discharge slot, lower charge prefers charge slot.
			double charge = ic2.api.item.ElectricItem.manager.getCharge(stackInSlot);
			double maxCharge = ic2.api.item.ElectricItem.manager.getMaxCharge(stackInSlot);
			boolean preferDischarge = maxCharge > 0.0D && (charge / maxCharge) >= 0.5D;

			if (preferDischarge) {
				if (canDischarge && !this.mergeItemStack(stackInSlot, 5, 6, false)) {
					if (!canCharge || !this.mergeItemStack(stackInSlot, 4, 5, false)) {
						return ItemStack.EMPTY;
					}
				}
			} else if (canCharge && !this.mergeItemStack(stackInSlot, 4, 5, false)) {
				if (!canDischarge || !this.mergeItemStack(stackInSlot, 5, 6, false)) {
					return ItemStack.EMPTY;
				}
			}
		}

		if (stackInSlot.isEmpty()) {
			slot.putStack(ItemStack.EMPTY);
		} else {
			slot.onSlotChanged();
		}

		if (stackInSlot.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		slot.onTake(playerIn, stackInSlot);
		this.tile.markDirty();
		return original;
	}
}
