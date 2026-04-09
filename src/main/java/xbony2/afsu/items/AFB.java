package xbony2.afsu.items;

import ic2.core.IC2;
import net.minecraft.item.Item;
import xbony2.afsu.AFSUMod;

public class AFB extends Item {

	public AFB() {
		super();
		this.setCreativeTab(IC2.tabIC2);
		this.setTranslationKey("afsu.alc");
		this.setMaxStackSize(64);
		this.setMaxDamage(0);
		this.setRegistryName(AFSUMod.AFSU_MODID, "alc");
	}
}
