package toast.naturalAbsorption;

import java.util.Random;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import toast.naturalAbsorption.network.MessageSyncShield;

@Mod(modid = ModNaturalAbsorption.MODID, name = "Natural Absorption", version = ModNaturalAbsorption.VERSION)
@Mod.EventBusSubscriber
public class ModNaturalAbsorption {
	/* TO DO *\
	 * Way to see current and max shield caps
	\* ** ** */

	// This mod's id.
	public static final String MODID = "natural_absorption";
	// This mod's version.
	public static final String VERSION = "1.2.2";

    /** The sided proxy. This points to a "common" proxy if and only if we are on a dedicated
     * server. Otherwise, it points to a client proxy. */
    @SidedProxy(clientSide = "toast.naturalAbsorption.client.ClientProxy", serverSide = "toast.naturalAbsorption.CommonProxy")
    public static CommonProxy proxy;
    /** The network channel for this mod. */
    public static SimpleNetworkWrapper CHANNEL;

	// The random number generator for this mod.
	public static final Random random = new Random();
	// The upgrade book for this mod.
	public static Item ABSORB_BOOK;
	// The enchantment for this mod.
	public static Enchantment ABSORB_ENCHANT;

	// Called before initialization. Loads the properties/configurations.
	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		Properties.load(new Configuration(event.getSuggestedConfigurationFile()));
		ModNaturalAbsorption.logDebug("Loading in debug mode!");

        int id = 0;
        ModNaturalAbsorption.CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("NA|CH1");
        if (event.getSide() == Side.CLIENT) {
            ModNaturalAbsorption.CHANNEL.registerMessage(MessageSyncShield.Handler.class, MessageSyncShield.class, id++, Side.CLIENT);
        }

		ModNaturalAbsorption.proxy.preInit();
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
		ABSORB_BOOK = new Item()
				.setRegistryName(ModNaturalAbsorption.MODID, "absorption_book")
				.setUnlocalizedName("absorption_book")
				.setCreativeTab(CreativeTabs.MISC)
				.setMaxStackSize(1);

		event.getRegistry().register(ABSORB_BOOK);
	}

	// Registers the enchantments in this mod.
	@SubscribeEvent
	public static void registerEnchantment(RegistryEvent.Register<Enchantment> event) {
		if (Properties.get().ENCHANT.ENABLE) {

			Enchantment.Rarity rarity;
			String rarityString = Properties.get().ENCHANT.RARITY.toLowerCase();
			switch(rarityString) {
				case "common":    rarity = Enchantment.Rarity.COMMON; break;
				case "uncommon":  rarity = Enchantment.Rarity.UNCOMMON; break;
				case "rare":      rarity = Enchantment.Rarity.RARE; break;
				case "very_rare": rarity = Enchantment.Rarity.VERY_RARE; break;
				default:
					ModNaturalAbsorption.logWarning("Unrecognized enchantment rarity (" + rarityString + "). Defaulting to RARE.");
					rarity = Enchantment.Rarity.RARE;
			}

			EnumEnchantmentType type;
			String typeString = Properties.get().ENCHANT.SLOT.toLowerCase();
			switch(typeString) {
				case "any": type = EnumEnchantmentType.ARMOR; break;
				case "head": type = EnumEnchantmentType.ARMOR_HEAD; break;
				case "chest": type = EnumEnchantmentType.ARMOR_CHEST; break;
				case "legs": type = EnumEnchantmentType.ARMOR_LEGS; break;
				case "feet": type = EnumEnchantmentType.ARMOR_FEET; break;
				default:
					ModNaturalAbsorption.logWarning("Unrecognized enchantment slot (" + typeString + "). Defaulting to ANY.");
					type = EnumEnchantmentType.ARMOR;
			}

			EntityEquipmentSlot[] allArmorSlots = { EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET };
			ABSORB_ENCHANT = new EnchantmentAbsorption(rarity, type, allArmorSlots)
					.setRegistryName(new ResourceLocation(ModNaturalAbsorption.MODID, "absorption"));
			event.getRegistry().register(ModNaturalAbsorption.ABSORB_ENCHANT);
		}
	}

	// Called during initialization. Registers entities, mob spawns, and renderers.
	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
        if (Properties.get().RECOVERY.DELAY >= 0) {
            MinecraftForge.EVENT_BUS.register(ShieldManager.class);
        }
        MinecraftForge.EVENT_BUS.register(EventHandler.class);
	}

	public static boolean debug() {
		return Properties.get().GENERAL.DEBUG;
	}

	// Prints the message to the console with this mod's name tag if debugging is enabled.
	public static void logDebug(String message) {
		if (ModNaturalAbsorption.debug()) ModNaturalAbsorption.log("(debug) " + message);
	}

	// Prints the message to the console with this mod's name tag.
	public static void log(String message) {
		System.out.println("[" + ModNaturalAbsorption.MODID + "] " + message);
	}

	// Prints the message to the console with this mod's name tag if debugging is enabled.
	public static void logWarning(String message) {
		ModNaturalAbsorption.log("[WARNING] " + message);
	}

	// Prints the message to the console with this mod's name tag if debugging is enabled.
	public static void logError(String message) {
		if (ModNaturalAbsorption.debug())
			throw new RuntimeException("[" + ModNaturalAbsorption.MODID + "] [ERROR] " + message);
		ModNaturalAbsorption.log("[ERROR] " + message);
	}

	// Throws a runtime exception with a message and this mod's name tag.
	public static void exception(String message) {
		throw new RuntimeException("[" + ModNaturalAbsorption.MODID + "] [FATAL ERROR] " + message);
	}
}
