package xbony2.afsu;

import xbony2.afsu.blocks.AFSUBlock;
import xbony2.afsu.blocks.ItemBlockAFSU;
import xbony2.afsu.gui.GuiHandler;
import xbony2.afsu.items.AFB;
import xbony2.afsu.tileentity.TileEntityAFSU;
import ic2.api.item.IC2Items;
import ic2.api.recipe.Recipes;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

@Mod(modid = AFSUMod.AFSU_MODID, name = "AFSU Mod", version = "@VERSION@", dependencies = "required-after:ic2")
public class AFSUMod {

	public static final String AFSU_MODID = "afsu";
	private static final Logger LOGGER = LogManager.getLogger("AFSU");

	@Instance(AFSUMod.AFSU_MODID)
	public static AFSUMod instance;

	public static final Block AFSU = new AFSUBlock();
	public static final ItemBlock IB_AFSU = new ItemBlockAFSU(AFSU);
	public static final Item ALC = new AFB();

	@EventHandler
	public void preInit(FMLPreInitializationEvent event){
		ConfigHandler.init(event.getSuggestedConfigurationFile());
		GameRegistry.registerTileEntity(TileEntityAFSU.class, AFSU_MODID + ":afsu");
	}

	@EventHandler
	public void init(FMLInitializationEvent event){
		NetworkRegistry.INSTANCE.registerGuiHandler(AFSUMod.instance, new GuiHandler());
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event){
		ItemStack cableGlass = firstValid(
				new String[] { "glass_cable", "cable#glass", "cable#glass_cable" },
				new String[] { "cable", "glass" },
				new String[] { "cable", "glass_cable" }
		);
		if (!isValid(cableGlass)) cableGlass = firstValidOre("itemCableGlass", "cableGlass");
		if (!isValid(cableGlass)) {
			Item cableItem = Item.getByNameOrId("ic2:cable");
			if (cableItem != null) cableGlass = new ItemStack(cableItem, 1, OreDictionary.WILDCARD_VALUE);
		}
		Object cableIngredient = firstValidOreKey("itemCableGlass", "cableGlass");
		if (cableIngredient == null) cableIngredient = cableGlass;
		ItemStack iridium = firstValid(
				new String[] { "iridium", "iridium_reinforced_plate", "crafting#iridium" },
				new String[] { "crafting", "iridium" },
				new String[] { "crafting", "alloy_iridium" }
		);
		if (!isValid(iridium)) iridium = firstValidOre("plateAlloyIridium", "ingotIridium");
		Object iridiumIngredient = firstValidOreKey("plateAlloyIridium", "ingotIridium");
		if (iridiumIngredient == null) iridiumIngredient = iridium;
		ItemStack uuCell = firstValid(
				new String[] { "uu_matter_cell", "fluid_cell#uu_matter", "fluid_cell#ic2uu_matter", "itemCellUuMatter" },
				new String[] { "fluid_cell", "uu_matter" },
				new String[] { "fluid_cell", "ic2uu_matter" }
		);
		// Some lookups resolve to an empty universal fluid cell; reject that.
		if (isValid(uuCell)
				&& uuCell.getItem().getRegistryName() != null
				&& "ic2:fluid_cell".equals(uuCell.getItem().getRegistryName().toString())
				&& uuCell.getMetadata() == 0
				&& !uuCell.hasTagCompound()) {
			uuCell = ItemStack.EMPTY;
		}
		if (!isValid(uuCell)) uuCell = firstValidOre("cellUUMatter", "itemCellUUMatter");
		if (!isValid(uuCell)) uuCell = firstValid(new String[] { "matter" });
		Object uuIngredient = isValid(uuCell) ? uuCell : firstValidOreKey("cellUUMatter", "itemCellUUMatter", "uuMatter", "materialUUMatter");
		if (uuIngredient == null) uuIngredient = uuCell;
		ItemStack mfsu = firstValid(
				new String[] { "mfsu", "te#mfsu" },
				new String[] { "te", "mfsu" }
		);
		Object alcIngredient = new ItemStack(ALC);

		if (isValid(cableGlass) && isValid(iridium)) {
			Recipes.advRecipes.addRecipe(new ItemStack(ALC), "GIG", "IUI", "GIG", 'G', cableIngredient, 'I', iridiumIngredient, 'U', uuIngredient);
			LOGGER.info("Registered AFB recipe with {}, {}, {}", cableIngredient, iridiumIngredient, uuIngredient);
		} else {
			LOGGER.warn("Skipping AFB recipe due to missing IC2 ingredient(s): cableGlass={}, iridium={}, uuCell={}",
					isValid(cableGlass), isValid(iridium), (isValid(uuCell) || uuIngredient instanceof String));
		}

		if (isValid(iridium) && isValid(cableGlass) && isValid(mfsu)) {
			Recipes.advRecipes.addRecipe(new ItemStack(AFSU), "MGM", "IAI", "MGM", 'I', iridiumIngredient, 'G', cableIngredient, 'M', mfsu, 'A', alcIngredient);
			LOGGER.info("Registered AFSU recipe with {}, {}, {}, {}", iridiumIngredient, cableIngredient, mfsu, AFSUMod.ALC);
		} else {
			LOGGER.warn("Skipping AFSU recipe due to missing IC2 ingredient(s): iridium={}, cableGlass={}, mfsu={}",
					isValid(iridium), isValid(cableGlass), isValid(mfsu));
		}
	}

	private static boolean isValid(ItemStack stack) {
		return stack != null && !stack.isEmpty();
	}

	private static ItemStack firstValid(String[] directNames, String[]... keyedNames) {
		for (String name : directNames) {
			if (name.contains("#")) {
				String[] split = name.split("#", 2);
				ItemStack stack = IC2Items.getItem(split[0], split[1]);
				if (isValid(stack)) return stack;
			} else {
				ItemStack stack = IC2Items.getItem(name);
				if (isValid(stack)) return stack;
			}
		}
		for (String[] pair : keyedNames) {
			if (pair.length == 2) {
				ItemStack stack = IC2Items.getItem(pair[0], pair[1]);
				if (isValid(stack)) return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack firstValidOre(String... oreNames) {
		for (String oreName : oreNames) {
			for (ItemStack stack : OreDictionary.getOres(oreName, false)) {
				if (isValid(stack)) return stack.copy();
			}
		}
		return ItemStack.EMPTY;
	}

	private static String firstValidOreKey(String... oreNames) {
		for (String oreName : oreNames) {
			if (!OreDictionary.getOres(oreName, false).isEmpty()) return oreName;
		}
		return null;
	}

	public static String translate(String key){
		if(I18n.canTranslate(key))
			return I18n.translateToLocal(key);
		else return I18n.translateToFallback(key);
	}

	@EventBusSubscriber(modid = AFSU_MODID)
	public static class RegistrationHandler {
		@SubscribeEvent
		public static void onRegisterBlocks(RegistryEvent.Register<Block> event){
			event.getRegistry().register(AFSU);
		}

		@SubscribeEvent
		public static void onRegisterItems(RegistryEvent.Register<Item> event){
			IB_AFSU.setRegistryName(AFSU.getRegistryName());
			event.getRegistry().register(IB_AFSU);
			event.getRegistry().register(ALC);
		}

		@SideOnly(Side.CLIENT)
		@SubscribeEvent
		public static void onRegisterModels(ModelRegistryEvent event){
			ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(AFSU), 0, new ModelResourceLocation(AFSU.getRegistryName(), "inventory"));
			ModelLoader.setCustomModelResourceLocation(ALC, 0, new ModelResourceLocation(ALC.getRegistryName(), "inventory"));
		}
	}
}
