package tezzi2001.afsu.blocks;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import tezzi2001.afsu.AFSUMod;
import tezzi2001.afsu.tileentity.TileEntityAFSU;

/**
 * Created by Master801 on 11/14/2014.
 * @author Master801
 */
public class ItemBlockAFSU extends ItemBlock {

	public ItemBlockAFSU(Block block) {
		super(block);
		this.setHasSubtypes(false);
		this.setMaxDamage(0);
	}

	@Override
	public int getMetadata(int damage){
		return 0;
	}

	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltipList, ITooltipFlag flagIn){
		final String output = AFSUMod.translate("ic2.item.tooltip.Output") + " " + TileEntityAFSU.MAX_OUTPUT + "EU/t";
		final String capacity = AFSUMod.translate("ic2.item.tooltip.Capacity") + " " + "1b EU";
		final String stored = AFSUMod.translate("ic2.item.tooltip.Store") + " ";
		tooltipList.add(output + " " + capacity);

		if(stack.hasTagCompound())
			tooltipList.add(stored + stack.getTagCompound().getInteger("energy") + " EU");
		else tooltipList.add(stored + 0 + " EU");
	}

}
