package net.ilexiconn.llibrary;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.ilexiconn.llibrary.common.crash.CrashReport;
import net.ilexiconn.llibrary.common.log.LoggerHelper;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import net.minecraftforge.fml.relauncher.IFMLCallHook;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * For autodownloading stuff.
 * This is really unoriginal, mostly ripped off ChickenBones who ripped it off from FML, credits to cpw.
 *
 * @author Ry_dog101
 * @author iLexiconn
 */
@IFMLLoadingPlugin.Name("LLoader")
public class LLoader implements IFMLLoadingPlugin, IFMLCallHook
{
    public LoggerHelper logger = new LoggerHelper("LLoader");

    public ByteBuffer downloadBuffer = ByteBuffer.allocateDirect(1 << 23);
    public File modsDir;
    public File modsDirVersion;
    public File llibraryData;
    public Thread pokeThread;

    public void load()
    {
        String mcVersion = (String) FMLInjectionData.data()[4];
        File mcDir = (File) FMLInjectionData.data()[6];

        modsDir = new File(mcDir, "mods");
        modsDirVersion = new File(mcDir, "mods/" + mcVersion);
        llibraryData = new File(mcDir, "llibrary/data/" + mcVersion);
        if (!modsDirVersion.exists())
            modsDirVersion.mkdirs();
        if (!llibraryData.exists())
            llibraryData.mkdirs();
        scanForJson();
        for (File file : llibraryData.listFiles())
            addClasspath(file.getName());
    }

    public String[] getASMTransformerClass()
    {
        return null;
    }

    public String getModContainerClass()
    {
        return null;
    }

    public String getSetupClass()
    {
        return getClass().getName();
    }

    public void injectData(Map<String, Object> data)
    {

    }

    public Void call()
    {
        load();
        return null;
    }

    public String getAccessTransformerClass()
    {
        return null;
    }

    public List<File> files()
    {
        List<File> list = new LinkedList<File>();
        list.addAll(Arrays.asList(modsDir.listFiles()));
        list.addAll(Arrays.asList(modsDirVersion.listFiles()));
        return list;
    }

    public void scanForJson()
    {
        try
        {
            logger.info("Searching " + modsDir.getAbsolutePath() + " for download configurations");
            logger.info("Also searching " + modsDirVersion.getAbsolutePath() + " for download configurations");
            for (File file : files())
            {
                if (file.getName().endsWith(".jar") || file.getName().endsWith(".zip"))
                {
                    ZipFile zip = new ZipFile(file);
                    ZipEntry e = zip.getEntry("llibrary/downloads.json");
                    ZipEntry info = zip.getEntry("mcmod.info");
                    String modName = "Unknown";
                    if (info != null)
                        modName = new JsonParser().parse(new InputStreamReader(zip.getInputStream(info))).getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString();
                    if (e != null)
                    {
                        logger.info("Found downloads.json file in " + file.getName());
                        loadJson(new InputStreamReader(zip.getInputStream(e)), modName);
                    }
                    zip.close();
                }
            }
        }
        catch (Exception e)
        {
            logger.error(CrashReport.makeCrashReport(e, "Failed searching for download configurations"));
        }
    }

    public void loadJson(InputStreamReader input, String modName)
    {
        try
        {
            JsonElement root = new JsonParser().parse(input);
            String url = root.getAsJsonArray().get(0).getAsString();
            URL test = new URL(url);
            BufferedReader in = new BufferedReader(new InputStreamReader(test.openStream()));
            JsonArray obj = new JsonParser().parse(in).getAsJsonArray();
            for (int i = 0; i < obj.size(); i++)
            {
                String downloadURL = obj.get(i).getAsJsonObject().get("url").getAsString();
                String downloadTarget = obj.get(i).getAsJsonObject().get("target").getAsString();
                logger.info("Downloading data for mod " + modName);
                download(downloadURL, downloadTarget, modName);
            }
        }
        catch (Exception e)
        {
            logger.error(CrashReport.makeCrashReport(e, "Failed parsing the download configuration for mod " + modName));
        }
    }

    public void download(String url, String target, String modName)
    {
        File libFile = new File(llibraryData, target);
        try
        {
            URL libDownload = new URL(url);
            logger.info("Downloading file " + libDownload.toString());
            URLConnection connection = libDownload.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "LLoader Downloader");
            int sizeGuess = connection.getContentLength();
            download(connection.getInputStream(), sizeGuess, libFile, modName);
            logger.info("Download complete");
        }
        catch (Exception e)
        {
            libFile.delete();
            logger.error(CrashReport.makeCrashReport(e, "Failed downloading data for mod " + modName));
        }
    }

    public void download(InputStream is, int sizeGuess, File target, String modName) throws Exception
    {
        try
        {
            if (sizeGuess > downloadBuffer.capacity())
                throw new Exception(String.format("The file %s is too large to be downloaded by LLoader - the download is invalid", target.getName()));

            downloadBuffer.clear();

            int bytesRead, fullLength = 0;

            setPokeThread(Thread.currentThread());
            byte[] smallBuffer = new byte[1024];
            while ((bytesRead = is.read(smallBuffer)) >= 0)
            {
                downloadBuffer.put(smallBuffer, 0, bytesRead);
                fullLength += bytesRead;
            }
            is.close();
            setPokeThread(null);
            downloadBuffer.limit(fullLength);
            downloadBuffer.position(0);
        }
        catch (InterruptedIOException e)
        {
            Thread.interrupted();
            logger.error(CrashReport.makeCrashReport(e, "Failed downloading data for mod " + modName));
        }

        if (!target.exists())
            target.createNewFile();

        downloadBuffer.position(0);
        FileOutputStream fos = new FileOutputStream(target);
        fos.getChannel().write(downloadBuffer);
        fos.close();
    }

    public void addClasspath(String target)
    {
        try
        {
            ((LaunchClassLoader) LLoader.class.getClassLoader()).addURL(new File(llibraryData, target).toURI().toURL());
        }
        catch (MalformedURLException e)
        {
            logger.error(CrashReport.makeCrashReport(e, "Failed adding file " + target + " to the classpath"));
        }
    }

    public void setPokeThread(Thread currentThread)
    {
        pokeThread = currentThread;
    }
}