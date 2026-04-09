package tezzi2001.afsu.blocks;

import ic2.api.tile.IWrenchable;
import ic2.core.IC2;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import ic2.core.item.tool.ItemToolWrench;
import ic2.core.item.tool.ItemToolWrenchNew;
import tezzi2001.afsu.AFSUMod;
import tezzi2001.afsu.tileentity.TileEntityAFSU;

public class AFSUBlock extends Block implements ITileEntityProvider, IWrenchable {
	/** Output/dot side — all six faces (same as IC2 MFSU-style rotation). */
	public static final PropertyDirection FACING = PropertyDirection.create("facing");

	public AFSUBlock() {
		super(Material.IRON);
		this.setCreativeTab(IC2.tabIC2);
		this.setHardness(1.5F);
		this.setTranslationKey("afsu.afsu");
		this.setRegistryName(AFSUMod.AFSU_MODID, "afsu");
		this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
	}

	@Override
	public boolean canProvidePower(IBlockState state){
		return true;
	}

	@Override
	public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos){
		return true;
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
		EnumFacing output = (placer != null) ? placer.getHorizontalFacing().getOpposite() : EnumFacing.NORTH;
		return this.getDefaultState().withProperty(FACING, output);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack itemstack){
		if (world.isRemote) return;
		EnumFacing facing = state.getValue(FACING);
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileEntityAFSU) {
			TileEntityAFSU afsu = (TileEntityAFSU) tile;
			double placedEnergy = StackUtil.getOrCreateNbtData(itemstack).getDouble("energy");
			afsu.setStoredEnergy(placedEnergy);
			afsu.setOutputSide(facing);
		}
	}

	@Override
	public boolean hasComparatorInputOverride(IBlockState state){
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState blockState, World world, BlockPos pos){
		TileEntity tile = world.getTileEntity(pos);
		if(tile instanceof TileEntityAFSU){
			TileEntityAFSU te = (TileEntityAFSU) tile;
			return Long.valueOf(Math.round(Util.map(te.getStoredInt(), TileEntityAFSU.MAX_STORAGE, 15.0D))).intValue();
		}

		return super.getComparatorInputOverride(blockState, world, pos);
	}

	@Override
	public int getWeakPower(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
		TileEntity tile = blockAccess.getTileEntity(pos);
		if (tile instanceof TileEntityAFSU && ((TileEntityAFSU) tile).isEmittingRedstone()) {
			return 15;
		}
		return 0;
	}

	@Override
	public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.entity.EntityLiving.SpawnPlacementType type){
		return false;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
		if (isWrench(player.getHeldItem(hand))) {
			// Let IC2 wrench handle rotation/removal without opening GUI.
			return true;
		}
		if (!player.isSneaking()) {
			player.openGui(AFSUMod.instance, 0, world, pos.getX(), pos.getY(), pos.getZ());
			return true;
		}

		return false;
	}

	@Override
	public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> stackList){
		ItemStack zeroStack = new ItemStack(this, 1, 0);
		StackUtil.getOrCreateNbtData(zeroStack).setInteger("energy", 0);
		stackList.add(zeroStack);
		ItemStack fullStack = new ItemStack(this, 1, 0);
		StackUtil.getOrCreateNbtData(fullStack).setInteger("energy", TileEntityAFSU.MAX_STORAGE);
		stackList.add(fullStack);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state){
		if(world.isRemote)
			return;

		TileEntity tile = world.getTileEntity(pos);
		if(tile instanceof IInventory){
			InventoryHelper.dropInventoryItems(world, pos, (IInventory) tile);
		}
		super.breakBlock(world, pos, state);
	}

	@Override
	public boolean hasTileEntity(IBlockState state){
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta){
		return new TileEntityAFSU();
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		EnumFacing facing = EnumFacing.byIndex(meta & 7);
		if (facing == null) facing = EnumFacing.NORTH;
		return this.getDefaultState().withProperty(FACING, facing);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getIndex();
	}

	@Override
	public EnumFacing getFacing(World world, BlockPos pos) {
		return world.getBlockState(pos).getValue(FACING);
	}

	@Override
	public boolean setFacing(World world, BlockPos pos, EnumFacing newSide, EntityPlayer player) {
		if (newSide == null) return false;
		IBlockState current = world.getBlockState(pos);
		if (current.getValue(FACING) == newSide) return false;
		world.setBlockState(pos, current.withProperty(FACING, newSide), 2);
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileEntityAFSU) {
			((TileEntityAFSU) tile).setOutputSide(newSide);
		}
		world.notifyNeighborsOfStateChange(pos, this, false);
		return true;
	}

	@Override
	public boolean wrenchCanRemove(World world, BlockPos pos, EntityPlayer player) {
		return true;
	}

	@Override
	public List<ItemStack> getWrenchDrops(World world, BlockPos pos, IBlockState state, TileEntity te, EntityPlayer player, int fortune) {
		ItemStack drop = new ItemStack(this, 1, 0);
		TileEntity tile = te != null ? te : world.getTileEntity(pos);
		if (tile instanceof TileEntityAFSU) {
			StackUtil.getOrCreateNbtData(drop).setDouble("energy", ((TileEntityAFSU) tile).getStoredInt());
		}
		return Collections.singletonList(drop);
	}

	private static boolean isWrench(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return false;
		Item item = stack.getItem();
		if (item == null) return false;
		if (item instanceof ItemToolWrench || item instanceof ItemToolWrenchNew) return true;
		if (item.getRegistryName() == null) return false;
		return "ic2".equals(item.getRegistryName().getNamespace()) && item.getRegistryName().getPath().contains("wrench");
	}
}
