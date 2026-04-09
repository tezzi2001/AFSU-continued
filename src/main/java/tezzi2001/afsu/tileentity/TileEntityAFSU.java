package tezzi2001.afsu.tileentity;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.item.ElectricItem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

public class TileEntityAFSU extends TileEntity implements ITickable, IEnergySink, IEnergySource, IInventory {
	public static final int MAX_OUTPUT = 8192;
	public static final int MAX_STORAGE = 1000000000;
	private static final int TIER = 5;
	public static final int REDSTONE_MODE_IGNORED = 0;
	public static final int REDSTONE_MODE_LOW = 1;
	public static final int REDSTONE_MODE_HIGH = 2;

	private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
	private boolean energyNetAdded;
	private double energy;
	private int clientEnergyLow;
	private int redstoneMode = REDSTONE_MODE_IGNORED;
	private int redstoneOutputLevel;
	private EnumFacing outputSide = EnumFacing.NORTH;

	public String getName() {
		return "tile.afsu.afsu.name";
	}

	@Override
	public void update() {
		if (this.world == null || this.world.isRemote) return;

		if (!this.energyNetAdded) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
			this.energyNetAdded = true;
		}

		// Slot 0: charge item from AFSU (matches IC2 electric storage top slot)
		ItemStack charge = this.items.get(0);
		if (!charge.isEmpty() && this.energy > 0.0D) {
			double maxTransfer = Math.min(MAX_OUTPUT, this.energy);
			double possible = ElectricItem.manager.charge(charge, maxTransfer, TIER, false, true);
			double moved = possible > 0.0D ? ElectricItem.manager.charge(charge, Math.min(maxTransfer, possible), TIER, false, false) : 0.0D;
			if (moved > 0.0D) {
				this.energy = Math.max(0.0D, this.energy - moved);
				normalizeEnergy();
				markDirty();
			}
		}

		// Slot 1: discharge item into AFSU (matches IC2 electric storage bottom slot)
		ItemStack discharge = this.items.get(1);
		if (!discharge.isEmpty()) {
			double free = Math.max(0.0D, MAX_STORAGE - this.energy);
			double maxTransfer = Math.min(MAX_OUTPUT, free);
			if (maxTransfer > 0.0D) {
				double possible = ElectricItem.manager.discharge(discharge, maxTransfer, TIER, false, true, true);
				double moved = possible > 0.0D ? ElectricItem.manager.discharge(discharge, Math.min(maxTransfer, possible), TIER, false, true, false) : 0.0D;
				if (moved > 0.0D) {
					this.energy = Math.min(MAX_STORAGE, this.energy + moved);
					normalizeEnergy();
					markDirty();
				}
			}
		}

		int newRsLevel = shouldEmitRedstone() ? 15 : 0;
		if (newRsLevel != this.redstoneOutputLevel) {
			this.redstoneOutputLevel = newRsLevel;
			if (this.world != null) {
				this.world.notifyNeighborsOfStateChange(this.pos, this.getBlockType(), false);
			}
		}
	}

	public void cycleRedstoneMode() {
		this.redstoneMode = (this.redstoneMode + 1) % 3;
		markDirty();
	}

	public int getRedstoneMode() {
		return this.redstoneMode;
	}

	public EnumFacing getOutputSide() {
		return this.outputSide;
	}

	public void setOutputSide(EnumFacing outputSide) {
		if (outputSide == null || outputSide == this.outputSide) return;
		this.outputSide = outputSide;
		reloadEnergyNet();
		markDirty();
	}

	public boolean canChargeItem(ItemStack stack) {
		if (stack.isEmpty()) return false;
		double maxCharge = ElectricItem.manager.getMaxCharge(stack);
		if (maxCharge <= 0.0D) return false;
		double current = ElectricItem.manager.getCharge(stack);
		return current < maxCharge;
	}

	public boolean canDischargeItem(ItemStack stack) {
		if (stack.isEmpty()) return false;
		return ElectricItem.manager.getCharge(stack) > 0.0D;
	}

	private boolean shouldEmitRedstone() {
		if (this.redstoneMode == REDSTONE_MODE_LOW) {
			return this.energy > 0.0D && this.energy < MAX_STORAGE;
		}
		if (this.redstoneMode == REDSTONE_MODE_HIGH) {
			return this.energy >= (MAX_STORAGE - 0.5D);
		}
		return false;
	}

	public boolean isEmittingRedstone() {
		return this.redstoneOutputLevel > 0;
	}

	@Override
	public void onChunkUnload() {
		unloadEnergyNet();
		super.onChunkUnload();
	}

	@Override
	public void invalidate() {
		unloadEnergyNet();
		super.invalidate();
	}

	private void unloadEnergyNet() {
		if (this.world != null && !this.world.isRemote && this.energyNetAdded) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
			this.energyNetAdded = false;
		}
	}

	private void reloadEnergyNet() {
		if (this.world == null || this.world.isRemote || !this.energyNetAdded) return;
		MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
		MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
	}

	public int getStoredInt() {
		return (int) Math.round(this.energy);
	}

	public int getCapacityInt() {
		return MAX_STORAGE;
	}

	public void setStoredEnergy(double value) {
		this.energy = Math.max(0.0D, Math.min(MAX_STORAGE, value));
		normalizeEnergy();
		markDirty();
	}

	private void normalizeEnergy() {
		if (this.energy < 0.000001D) {
			this.energy = 0.0D;
		} else if (this.energy > MAX_STORAGE - 0.000001D) {
			this.energy = MAX_STORAGE;
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setDouble("energy", this.energy);
		nbt.setInteger("redstoneMode", this.redstoneMode);
		nbt.setByte("outputSide", (byte) this.outputSide.getIndex());
		ItemStackHelper.saveAllItems(nbt, this.items);
		return nbt;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.energy = nbt.getDouble("energy");
		this.redstoneMode = nbt.getInteger("redstoneMode");
		this.outputSide = EnumFacing.byIndex(nbt.getByte("outputSide"));
		if (this.outputSide == null) this.outputSide = EnumFacing.NORTH;
		ItemStackHelper.loadAllItems(nbt, this.items);
	}

	@Override
	public boolean acceptsEnergyFrom(IEnergyEmitter emitter, EnumFacing side) {
		return side != this.outputSide;
	}

	@Override
	public double getDemandedEnergy() {
		return Math.max(0.0D, MAX_STORAGE - this.energy);
	}

	@Override
	public int getSinkTier() {
		return TIER;
	}

	@Override
	public double injectEnergy(EnumFacing directionFrom, double amount, double voltage) {
		double accepted = Math.min(amount, getDemandedEnergy());
		if (accepted > 0.0D) {
			this.energy += accepted;
			normalizeEnergy();
			markDirty();
		}
		return amount - accepted;
	}

	@Override
	public boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing side) {
		return side == this.outputSide;
	}

	@Override
	public double getOfferedEnergy() {
		return Math.min(this.energy, MAX_OUTPUT);
	}

	@Override
	public void drawEnergy(double amount) {
		this.energy = Math.max(0.0D, this.energy - amount);
		normalizeEnergy();
		markDirty();
	}

	@Override
	public int getSourceTier() {
		return TIER;
	}

	@Override
	public int getSizeInventory() {
		return this.items.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : this.items) if (!stack.isEmpty()) return false;
		return true;
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		return this.items.get(index);
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		ItemStack stack = ItemStackHelper.getAndSplit(this.items, index, count);
		if (!stack.isEmpty()) markDirty();
		return stack;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		ItemStack stack = ItemStackHelper.getAndRemove(this.items, index);
		if (!stack.isEmpty()) markDirty();
		return stack;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		this.items.set(index, stack);
		markDirty();
	}

	@Override
	public int getInventoryStackLimit() {
		return 1;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		if (this.world.getTileEntity(this.pos) != this) return false;
		return player.getDistanceSq(this.pos) <= 64.0D;
	}

	@Override
	public void openInventory(EntityPlayer player) {}

	@Override
	public void closeInventory(EntityPlayer player) {}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		if (index == 0) return canChargeItem(stack);
		if (index == 1) return canDischargeItem(stack);
		return false;
	}

	@Override
	public int getField(int id) {
		int value = getStoredInt();
		if (id == 0) return value & 0xFFFF;
		if (id == 1) return (value >>> 16) & 0xFFFF;
		if (id == 2) return this.redstoneMode;
		return 0;
	}

	@Override
	public void setField(int id, int value) {
		if (id == 0) {
			this.clientEnergyLow = value & 0xFFFF;
		} else if (id == 1) {
			int energyInt = ((value & 0xFFFF) << 16) | this.clientEnergyLow;
			this.energy = energyInt;
		} else if (id == 2) {
			this.redstoneMode = value;
		}
	}

	@Override
	public int getFieldCount() {
		return 3;
	}

	@Override
	public void clear() {
		for (int i = 0; i < this.items.size(); i++) {
			this.items.set(i, ItemStack.EMPTY);
		}
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	/**
	 * Keep the same tile when only blockstate properties (like facing) change.
	 */
	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}
}
