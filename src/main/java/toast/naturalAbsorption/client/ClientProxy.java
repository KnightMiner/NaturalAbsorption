package toast.naturalAbsorption.client;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import toast.naturalAbsorption.CommonProxy;
import toast.naturalAbsorption.EventHandler;
import toast.naturalAbsorption.ModNaturalAbsorption;
import toast.naturalAbsorption.Properties;
import toast.naturalAbsorption.ShieldManager;


public class ClientProxy extends CommonProxy {

	@Override
	public void preInit() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void registerModels(ModelRegistryEvent event) {
		registerItemModel(ModNaturalAbsorption.ABSORB_BOOK);
	}

	private void registerItemModel(Item item) {
		if(item != null && item != Items.AIR) {
			final ResourceLocation location = item.getRegistryName();
			// so all meta get the item model
			ModelLoader.setCustomMeshDefinition(item, new ItemMeshDefinition() {
				@Nonnull
				@Override
				public ModelResourceLocation getModelLocation(@Nonnull ItemStack stack) {
					return new ModelResourceLocation(location, "inventory");
				}
			});
			ModelLoader.registerItemVariants(item, location);
		}
	}

    /**
     * Called by ItemStack.getTooltip().
     * EntityPlayer entityPlayer = the player looking at the tooltip.
     * boolean showAdvancedItemTooltips = true if advanced tooltips are enabled.
     * ItemStack itemStack = the item stack to display a tooltip for.
     * List<String> toolTip = the tooltip.
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onItemTooltip(ItemTooltipEvent event) {
    	EntityPlayer player = event.getEntityPlayer();
        if (EventHandler.isShieldItem(event.getItemStack()) && player != null) {
        	List<String> tooltip = event.getToolTip();
        	String loc = ModNaturalAbsorption.ABSORB_BOOK.getUnlocalizedName() + ".tooltip.";
        	String defaultColor = TextFormatting.GRAY.toString();
        	String gold = TextFormatting.YELLOW.toString();

            boolean canUse = player.experienceLevel >= Properties.get().UPGRADES.LEVEL_COST || player.capabilities.isCreativeMode;
            boolean tooExpensive = !canUse;

            ITextComponent text;
            tooltip.set(0, gold + event.getToolTip().get(0)); // Colors the name

            NBTTagCompound shieldData = ShieldManager.getShieldData(player);
            float shieldCapacity = ShieldManager.getData(shieldData, ShieldManager.CAPACITY_TAG, Properties.get().GENERAL.STARTING_SHIELD);

            String[] shieldCap = {
            	Math.round(shieldCapacity) == shieldCapacity ? Integer.toString(Math.round(shieldCapacity)) : Float.toString(shieldCapacity),
            	Math.round(shieldCapacity) == shieldCapacity ? Integer.toString(Math.round(Properties.get().GENERAL.MAX_SHIELD)) : Float.toString(Properties.get().GENERAL.MAX_SHIELD)
            };
        	text = new TextComponentTranslation(loc + "info", new Object[] {
    			new StringBuilder(gold).append(shieldCap[0]).append(defaultColor)
    			.append(" / ").append(gold).append(shieldCap[1]).toString()
    		});
        	tooltip.add("");
        	tooltip.add(defaultColor + text.getUnformattedText());

        	float gain = Properties.get().UPGRADES.ABSORPTION_GAIN;
        	if (gain > Properties.get().GENERAL.MAX_SHIELD - shieldCapacity) {
        		gain = Properties.get().GENERAL.MAX_SHIELD - shieldCapacity;
        	}
        	if (gain > 0.0F) {
        		tooltip.add("");
            	text = new TextComponentTranslation(loc + "gain", new Object[] { });
            	tooltip.add(defaultColor + text.getUnformattedText());
            	text = new TextComponentTranslation(MobEffects.ABSORPTION.getName(), new Object[] { });
            	tooltip.add(new StringBuilder(TextFormatting.BLUE.toString())
            		.append(" +").append(Float.toString(gain)).append(" ").append(text.getUnformattedText()).toString());
        	}
        	else {
        		canUse = false;
        	}

            if (Properties.get().UPGRADES.LEVEL_COST > 0 && !event.getEntityPlayer().capabilities.isCreativeMode) {
            	if (gain <= 0.0F)
                	event.getToolTip().add("");
            	text = new TextComponentTranslation(loc + "cost", new Object[] { Properties.get().UPGRADES.LEVEL_COST });
            	tooltip.add((tooExpensive ? TextFormatting.RED.toString() : defaultColor) + text.getUnformattedText());
            }

            if (canUse) {
                text = new TextComponentTranslation(loc + "canuse", new Object[] { });
                tooltip.add("");
                tooltip.add(defaultColor + text.getUnformattedText());
            }
        }
    }

    /**
     * Called by GuiInGame.
     * float partialTicks = the partial tick.
     * ScaledResolution resolution = the game's resolution.
     * int mouseX = the cursor's x position.
     * int mouseY = the cursor's y position.
     * ElementType type = the type of render event.
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void beforeRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        if (Properties.get().ARMOR.REPLACE_ARMOR && Properties.get().ARMOR.HIDE_ARMOR_BAR && event.getType() == RenderGameOverlayEvent.ElementType.ARMOR) {
            event.setCanceled(true);
        }
    }
}
