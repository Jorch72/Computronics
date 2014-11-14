package pl.asie.computronics;

import com.google.common.collect.ImmutableList;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.ModAPIManager;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.network.FMLEventChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.asie.computronics.api.multiperipheral.IMultiPeripheralProvider;
import pl.asie.computronics.api.multiperipheral.IMultiPeripheralRegistry;
import pl.asie.computronics.audio.DFPWMPlaybackManager;
import pl.asie.computronics.block.BlockCamera;
import pl.asie.computronics.block.BlockChatBox;
import pl.asie.computronics.block.BlockCipher;
import pl.asie.computronics.block.BlockCipherAdvanced;
import pl.asie.computronics.block.BlockColorfulLamp;
import pl.asie.computronics.block.BlockEEPROMReader;
import pl.asie.computronics.block.BlockIronNote;
import pl.asie.computronics.block.BlockRadar;
import pl.asie.computronics.block.BlockTapeReader;
import pl.asie.computronics.cc.IntegrationComputerCraft;
import pl.asie.computronics.cc.multiperipheral.MultiPeripheralRegistry;
import pl.asie.computronics.integration.ModRecipes;
import pl.asie.computronics.integration.buildcraft.ActionProvider;
import pl.asie.computronics.integration.buildcraft.StatementParameters;
import pl.asie.computronics.integration.buildcraft.TriggerProvider;
import pl.asie.computronics.integration.railcraft.IntegrationRailcraft;
import pl.asie.computronics.item.ItemTape;
import pl.asie.computronics.item.block.ItemBlockWithSpecialText;
import pl.asie.computronics.network.NetworkHandlerClient;
import pl.asie.computronics.network.NetworkHandlerServer;
import pl.asie.computronics.oc.IntegrationOpenComputers;
import pl.asie.computronics.reference.Compat;
import pl.asie.computronics.reference.Config;
import pl.asie.computronics.reference.Mods;
import pl.asie.computronics.tape.StorageManager;
import pl.asie.computronics.tile.TileCamera;
import pl.asie.computronics.tile.TileChatBox;
import pl.asie.computronics.tile.TileCipherBlock;
import pl.asie.computronics.tile.TileCipherBlockAdvanced;
import pl.asie.computronics.tile.TileColorfulLamp;
import pl.asie.computronics.tile.TileEEPROMReader;
import pl.asie.computronics.tile.TileIronNote;
import pl.asie.computronics.tile.TileRadar;
import pl.asie.computronics.tile.TileTapeDrive;
import pl.asie.computronics.util.achievements.ComputronicsAchievements;
import pl.asie.lib.gui.GuiHandler;
import pl.asie.lib.item.ItemMultiple;
import pl.asie.lib.network.PacketHandler;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(modid = Mods.Computronics, name = Mods.Computronics_NAME, version = "@VERSION@", useMetadata = true, dependencies = "required-after:asielib@[0.3.3,);after:ComputerCraft;after:OpenComputers@[1.4.0,);after:OpenComputers|Core;after:MineFactoryReloaded;after:RedLogic;after:ProjRed|Core;after:nedocomputers;after:BuildCraft|Core@[6.1.5,);after:Railcraft@[9.3.3.4,);after:gregtech;after:EnderIO")
public class Computronics {
	public Config config;
	public Compat compat;
	public static Random rand = new Random();
	public static Logger log;

	public static FMLEventChannel channel;

	@Instance(value = Mods.Computronics)
	public static Computronics instance;
	public static StorageManager storage;
	public static GuiHandler gui;
	public static PacketHandler packet;
	public DFPWMPlaybackManager audio;
	public static ExecutorService rsaThreads;

	@SidedProxy(clientSide = "pl.asie.computronics.ClientProxy", serverSide = "pl.asie.computronics.CommonProxy")
	public static CommonProxy proxy;

	public static BlockIronNote ironNote;
	public static BlockTapeReader tapeReader;
	public static BlockCamera camera;
	public static BlockChatBox chatBox;
	public static BlockCipher cipher;
	public static BlockCipherAdvanced cipher_advanced;
	public static BlockRadar radar;
	public static BlockEEPROMReader nc_eepromreader;
	public static BlockColorfulLamp colorfulLamp;

	public static IntegrationRailcraft railcraft;
	public static IntegrationOpenComputers opencomputers;
	public static IntegrationComputerCraft computercraft;

	public static ItemTape itemTape;
	public static ItemMultiple itemParts;
	public static ItemMultiple itemPartsGreg;

	public ComputronicsAchievements achievements;

	public static MultiPeripheralRegistry peripheralRegistry;

	public static CreativeTabs tab = new CreativeTabs("tabComputronics") {
		public Item getTabIconItem() {
			return itemTape;
		}
	};

	public boolean isEnabled(String name, boolean def) {
		return config.isEnabled(name, def);
	}

	private void registerBlockWithTileEntity(Block block, Class<? extends TileEntity> tile, String name) {
		registerBlockWithTileEntity(block, ItemBlock.class, tile, name);
	}

	private void registerBlockWithTileEntity(Block block, Class<? extends ItemBlock> itemBlock, Class<? extends TileEntity> tile, String name) {
		GameRegistry.registerBlock(block, itemBlock, name);
		GameRegistry.registerTileEntity(tile, name);
		//System.out.println("Registering " + name + " as TE " + tile.getCanonicalName());
		FMLInterModComms.sendMessage(Mods.AE2, "whitelist-spatial", tile.getCanonicalName());
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		log = LogManager.getLogger(Mods.Computronics);

		config = new Config(event);

		audio = new DFPWMPlaybackManager(proxy.isClient());
		packet = new PacketHandler(Mods.Computronics, new NetworkHandlerClient(), new NetworkHandlerServer());

		compat = new Compat(this.config.config);

		config.preInit();

		if(isEnabled("ironNoteBlock", true)) {
			ironNote = new BlockIronNote();
			registerBlockWithTileEntity(ironNote, TileIronNote.class, "computronics.ironNoteBlock");
		}

		if(isEnabled("tape", true)) {
			tapeReader = new BlockTapeReader();
			registerBlockWithTileEntity(tapeReader, TileTapeDrive.class, "computronics.tapeReader");
		}

		if(isEnabled("camera", true)) {
			camera = new BlockCamera();
			registerBlockWithTileEntity(camera, TileCamera.class, "computronics.camera");
		}

		if(isEnabled("chatBox", true)) {
			chatBox = new BlockChatBox();
			registerBlockWithTileEntity(chatBox, ItemBlockWithSpecialText.class, TileChatBox.class, "computronics.chatBox");
		}

		if(isEnabled("cipher", true)) {
			cipher = new BlockCipher();
			registerBlockWithTileEntity(cipher, TileCipherBlock.class, "computronics.cipher");
		}

		if(isEnabled("cipher_advanced", true)) {
			cipher_advanced = new BlockCipherAdvanced();
			registerBlockWithTileEntity(cipher_advanced, ItemBlockWithSpecialText.class, TileCipherBlockAdvanced.class, "computronics.cipher_advanced");
			rsaThreads = Executors.newCachedThreadPool();
		}

		if(isEnabled("radar", true)) {
			radar = new BlockRadar();
			registerBlockWithTileEntity(radar, TileRadar.class, "computronics.radar");
		}

		if(isEnabled("lamp", true)) {
			colorfulLamp = new BlockColorfulLamp();
			registerBlockWithTileEntity(colorfulLamp, TileColorfulLamp.class, "computronics.colorfulLamp");
		}

		if(Loader.isModLoaded(Mods.NedoComputers) && isEnabled("eepromReader", true)) {
			nc_eepromreader = new BlockEEPROMReader();
			registerBlockWithTileEntity(nc_eepromreader, TileEEPROMReader.class, "computronics.eepromReader");
		}

		if(isEnabled("tape", true)) {
			itemTape = new ItemTape(Config.TAPE_LENGTHS);
			GameRegistry.registerItem(itemTape, "computronics.tape");

			if(Loader.isModLoaded(Mods.GregTech)) {
				itemPartsGreg = new ItemMultiple(Mods.Computronics, new String[] { "reelChromoxide" });
				itemPartsGreg.setCreativeTab(tab);
				GameRegistry.registerItem(itemPartsGreg, "computronics.gt_parts");
				proxy.registerEntities();
			}

			itemParts = new ItemMultiple(Mods.Computronics, new String[] { "part_tape_track" });
			itemParts.setCreativeTab(tab);
			GameRegistry.registerItem(itemParts, "computronics.parts");
		}

		if(Loader.isModLoaded(Mods.Railcraft)) {
			railcraft = new IntegrationRailcraft(this);
		}

		if(Loader.isModLoaded(Mods.ComputerCraft)) {
			computercraft = new IntegrationComputerCraft(this);
			peripheralRegistry = new MultiPeripheralRegistry();
		}

		if(Loader.isModLoaded(Mods.OpenComputers)) {
			opencomputers = new IntegrationOpenComputers(this);
			opencomputers.preInit();
		}
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		gui = new GuiHandler();
		NetworkRegistry.INSTANCE.registerGuiHandler(Computronics.instance, gui);

		if(chatBox != null) {
			MinecraftForge.EVENT_BUS.register(new ChatBoxHandler());
		}

		proxy.registerGuis(gui);

		FMLInterModComms.sendMessage(Mods.Waila, "register", "pl.asie.computronics.integration.waila.IntegrationWaila.register");

		config.setCategoryComment("power", "Every value related to energy in this section uses RF as the base power unit.");

		if(Loader.isModLoaded(Mods.ComputerCraft)) {
			config.setCategoryComment(Compat.Compatibility, "Set anything here to false to prevent Computronics from adding the respective Peripherals and Drivers");
			computercraft.init();
		}
		if(Loader.isModLoaded(Mods.OpenComputers)) {
			config.setCategoryComment(Compat.Compatibility, "Set anything here to false to prevent Computronics from adding the respective Peripherals and Drivers");
			opencomputers.init();
		}

		achievements = new ComputronicsAchievements();
		achievements.initialize();

		config.save();
		proxy.registerRenderers();
	}

	/**
	 * Registers a new {@link IMultiPeripheralProvider}.
	 * If you want to hook into this, do it between Computronics' preInit and init phase
	 */
	@Optional.Method(modid = Mods.ComputerCraft)
	public static void registerMultiPeripheralProvider(IMultiPeripheralProvider provider) {
		if(peripheralRegistry != null) {
			peripheralRegistry.registerPeripheralProvider(provider);
		}
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {

		if(Loader.isModLoaded(Mods.GregTech) && Config.GREGTECH_RECIPES) {
			ModRecipes.GregTechRecipes.registerGregTechRecipes();
		} else {
			ModRecipes.registerRecipes();
		}

		// Mod compat - GregTech
		if(itemTape != null && Loader.isModLoaded(Mods.GregTech) && itemPartsGreg != null) {
			ModRecipes.GregTechRecipes.regsiterGregTechTapeRecipes();
		}

		if(Loader.isModLoaded(Mods.OpenComputers)) {
			opencomputers.postInit();
		}

		if(ModAPIManager.INSTANCE.hasAPI(Mods.API.BuildCraftStatements)) {
			TriggerProvider.initialize();
			ActionProvider.initialize();
			StatementParameters.initialize();
		}
	}

	@EventHandler
	public void serverStart(FMLServerAboutToStartEvent event) {
		Computronics.storage = new StorageManager();
	}

	/**
	 * You need to call this between Computronics' preInit and init phase
	 * <p/>
	 * using {@link FMLInterModComms#sendMessage}.
	 * <p/>
	 * Example:
	 * FMLInterModComms.sendMessage("Computronics", "addmultiperipherals", "pl.asie.computronics.cc.multiperipheral.MultiPeripheralRegistry.register")
	 */
	@EventHandler
	@SuppressWarnings("unchecked")
	public void receiveIMC(FMLInterModComms.IMCEvent event) {
		if(Loader.isModLoaded(Mods.ComputerCraft)) {
			if(peripheralRegistry != null) {
				ImmutableList<FMLInterModComms.IMCMessage> messages = event.getMessages();
				for(FMLInterModComms.IMCMessage message : messages) {
					if(message.isStringMessage()) {
						if(message.key.equalsIgnoreCase("addmultiperipherals")) {
							try {
								String methodString = message.getStringValue();
								String[] methodParts = methodString.split("\\.");
								String methodName = methodParts[methodParts.length - 1];
								String className = methodString.substring(0, methodString.length() - methodName.length() - 1);
								try {
									Class c = Class.forName(className);
									Method method = c.getDeclaredMethod(methodName, IMultiPeripheralRegistry.class);
									method.invoke(null, peripheralRegistry);
								} catch(ClassNotFoundException e) {
									log.warn("Could not find class " + className, e);
								} catch(NoSuchMethodException e) {
									log.warn("Could not find method " + methodString, e);
								} catch(Exception e) {
									log.warn("Exception while trying to call method " + methodString, e);
								}
							} catch(Exception e) {
								log.warn("Exception while trying to register a MultiPeripheral", e);
							}
						}
					}
				}
			}
		}
	}
}
