package xbony2.afsu.blocks;

import ic2.core.IC2;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import xbony2.afsu.AFSUMod;
import xbony2.afsu.tileentity.TileEntityAFSU;

public class AFSUBlock extends Block implements ITileEntityProvider {

	public AFSUBlock() {
		super(Material.IRON);
		this.setCreativeTab(IC2.tabIC2);
		this.setHardness(1.5F);
		this.setTranslationKey("afsu.afsu");
		this.setRegistryName(AFSUMod.AFSU_MODID, "afsu");
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
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack itemstack){
		if (world.isRemote) return;
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileEntityAFSU) {
			TileEntityAFSU afsu = (TileEntityAFSU) tile;
			double placedEnergy = StackUtil.getOrCreateNbtData(itemstack).getDouble("energy");
			afsu.setStoredEnergy(placedEnergy);
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
}
