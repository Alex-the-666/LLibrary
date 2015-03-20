package net.ilexiconn.llibrary.survivaltab;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

/**
 * Interface for custom survival tabs, register with TabRegistry.registerSurvivalTab().
 *
 * @author iLexiconn
 */
public interface ISurvivalTab
{
    /**
     * @return unlocalized name string
     */
    public String getTabName();

    /**
     * @return itemstack with item or block to be displayed
     */
    public ItemStack getTabIcon();

    /**
     * Called when the survival tab is clicked.
     *
     * @param player the player opening the gui
     */
    public void openContainerGui(EntityPlayer player);

    /**
     * @return class of the container gui.
     * @see net.minecraft.client.gui.inventory.GuiContainer
     */
    @SideOnly(Side.CLIENT)
    public Class<? extends GuiContainer> getContainerGuiClass();
    
    /**
     * @return class of the container to open server side.
     */
    public Class<? extends Container> getContainerClass();
}
