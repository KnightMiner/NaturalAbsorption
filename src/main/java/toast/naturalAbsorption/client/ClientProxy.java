package toast.naturalAbsorption.client;

import javax.annotation.Nonnull;

import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import toast.naturalAbsorption.CommonProxy;
import toast.naturalAbsorption.ModNaturalAbsorption;


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
}
