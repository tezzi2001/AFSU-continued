package tezzi2001.afsu.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.Locale;
import tezzi2001.afsu.tileentity.TileEntityAFSU;

@SideOnly(Side.CLIENT)
public class GuiAFSU extends GuiContainer {
	private static final ResourceLocation BACKGROUND = new ResourceLocation("ic2", "textures/gui/GUIElectricBlock.png");
	private static final ResourceLocation COMMON = new ResourceLocation("ic2", "textures/gui/common.png");
	private final ContainerAFSU container;
	private GuiButton redstoneButton;
	private double displayedEnergy = -1.0D;
	private int lastServerEnergy = Integer.MIN_VALUE;
	private long lastServerUpdateMs;
	private double serverRatePerMs;

	public GuiAFSU(ContainerAFSU container) {
		super(container);
		this.container = container;
		this.xSize = 176;
		this.ySize = 196;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		TileEntityAFSU tile = this.container.getTile();
		long now = System.currentTimeMillis();
		int targetEnergy = tile.getStoredInt();
		if (this.lastServerEnergy == Integer.MIN_VALUE) {
			this.lastServerEnergy = targetEnergy;
			this.lastServerUpdateMs = now;
			this.displayedEnergy = targetEnergy;
		} else if (targetEnergy != this.lastServerEnergy) {
			long dt = Math.max(1L, now - this.lastServerUpdateMs);
			this.serverRatePerMs = (targetEnergy - this.lastServerEnergy) / (double) dt;
			this.lastServerEnergy = targetEnergy;
			this.lastServerUpdateMs = now;
		}

		double predicted = this.lastServerEnergy + this.serverRatePerMs * Math.max(0L, now - this.lastServerUpdateMs);
		double clamped = Math.max(0.0D, Math.min(tile.getCapacityInt(), predicted));
		if (this.displayedEnergy < 0.0D) {
			this.displayedEnergy = clamped;
		} else {
			this.displayedEnergy += (clamped - this.displayedEnergy) * 0.6D;
		}
		int shownEnergy = (int) Math.round(this.displayedEnergy);
		int color = 4210752;
		String title = "AFSU";
		this.fontRenderer.drawString(title, (this.xSize - this.fontRenderer.getStringWidth(title)) / 2, 6, color);
		this.fontRenderer.drawString("Power Level:", 92, 25, color);
		this.fontRenderer.drawString(String.valueOf(shownEnergy), 110, 35, color);
		this.fontRenderer.drawString("/" + tile.getCapacityInt(), 110, 45, color);
		this.fontRenderer.drawString(String.format(Locale.ROOT, "Out: %.1f EU/t", (double) TileEntityAFSU.MAX_OUTPUT), 96, 60, color);
		this.fontRenderer.drawString("Armor", 8, 74, color);
		this.itemRender.renderItemAndEffectIntoGUI(new ItemStack(Items.REDSTONE), 154, 6);
	}

	@Override
	public void initGui() {
		super.initGui();
		int guiLeft = (this.width - this.xSize) / 2;
		int guiTop = (this.height - this.ySize) / 2;
		this.redstoneButton = new GuiButton(0, guiLeft + 152, guiTop + 4, 20, 20, "");
		this.buttonList.add(this.redstoneButton);
		updateRedstoneButtonText();
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 0) {
			this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 0);
		}
		updateRedstoneButtonText();
	}

	private void updateRedstoneButtonText() {
		if (this.redstoneButton == null) return;
		this.redstoneButton.displayString = "";
	}

	private static String getRedstoneModeLabel(int mode) {
		if (mode == TileEntityAFSU.REDSTONE_MODE_LOW) return "Redstone Behavior: Emit if partially filled";
		if (mode == TileEntityAFSU.REDSTONE_MODE_HIGH) return "Redstone Behavior: Emit if full";
		return "Redstone Behavior: Nothing";
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		if (isPointInRegion(152, 4, 20, 20, mouseX, mouseY)) {
			TileEntityAFSU tile = this.container.getTile();
			this.drawHoveringText(Collections.singletonList(getRedstoneModeLabel(tile.getRedstoneMode())), mouseX, mouseY);
		}
		this.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		this.mc.getTextureManager().bindTexture(BACKGROUND);
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

		TileEntityAFSU tile = this.container.getTile();
		double stored = this.displayedEnergy >= 0.0D ? this.displayedEnergy : tile.getStoredInt();
		int capacity = Math.max(1, tile.getCapacityInt());
		int barMaxWidth = 24;
		int barHeight = 9;
		int filled = (int) Math.round((stored / capacity) * barMaxWidth);
		if (filled > 0) {
			this.mc.getTextureManager().bindTexture(COMMON);
			int barX = x + 79;
			int barY = y + 38;
			drawTexturedModalRect(barX, barY, 132, 43, filled, barHeight);
		}
	}
}
