package net.ilexiconn.llibrary.client;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.ilexiconn.llibrary.client.gui.GuiChangelog;
import net.ilexiconn.llibrary.client.gui.GuiHelper;
import net.ilexiconn.llibrary.client.gui.GuiLLibraryMainMenu;
import net.ilexiconn.llibrary.client.render.entity.RenderLLibraryPlayer;
import net.ilexiconn.llibrary.common.ServerProxy;
import net.ilexiconn.llibrary.common.config.LLibraryConfigHandler;
import net.ilexiconn.llibrary.common.json.container.JsonModUpdate;
import net.ilexiconn.llibrary.common.update.ChangelogHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

import java.io.File;

@SideOnly(Side.CLIENT)
public class ClientProxy extends ServerProxy
{
    public static RenderLLibraryPlayer renderCustomPlayer;

    public void preInit(File config)
    {
        super.preInit(config);

        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        FMLCommonHandler.instance().bus().register(new ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new GuiHelper());
        FMLCommonHandler.instance().bus().register(new GuiHelper());
        GuiHelper.addOverride(GuiMainMenu.class, new GuiLLibraryMainMenu());

        if (LLibraryConfigHandler.threadedScreenshots)
        {
            ClientEventHandler.screenshotKeyBinding = new KeyBinding(Minecraft.getMinecraft().gameSettings.keyBindScreenshot.getKeyDescription(), Minecraft.getMinecraft().gameSettings.keyBindScreenshot.getKeyCode(), Minecraft.getMinecraft().gameSettings.keyBindScreenshot.getKeyCategory());
            for (int i = 0; i < Minecraft.getMinecraft().gameSettings.keyBindings.length; ++i)
            {
                if (Minecraft.getMinecraft().gameSettings.keyBindings[i] == Minecraft.getMinecraft().gameSettings.keyBindScreenshot)
                {
                    Minecraft.getMinecraft().gameSettings.keyBindings[i] = ClientEventHandler.screenshotKeyBinding;
                    Minecraft.getMinecraft().gameSettings.keyBindScreenshot.setKeyCode(-1);
                }
            }
        }
    }

    public void postInit()
    {
        super.postInit();

        renderCustomPlayer = new RenderLLibraryPlayer();
        RenderManager.instance.entityRenderMap.put(EntityPlayer.class, renderCustomPlayer);
    }

    public void openChangelogGui(JsonModUpdate mod, String version)
    {
        String[] changelog = null;

        try
        {
            changelog = ChangelogHandler.getChangelog(mod, version);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Minecraft.getMinecraft().displayGuiScreen(new GuiChangelog(mod, version, changelog));
    }

    public EntityPlayer getClientPlayer()
    {
        return Minecraft.getMinecraft().thePlayer;
    }
}
