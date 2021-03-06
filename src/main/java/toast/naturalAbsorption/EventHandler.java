package toast.naturalAbsorption;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import toast.naturalAbsorption.network.MessageSyncShield;

public class EventHandler {

    // Returns true if the item is this mod's shield upgrade item.
    public static boolean isShieldItem(ItemStack itemStack) {
        return !itemStack.isEmpty() && itemStack.getItem() == ModNaturalAbsorption.ABSORB_BOOK;
    }


    /**
     * Called by EntityPlayer.
     * EntityPlayer entityPlayer = the player interacting.
     * PlayerInteractEvent.Action action = the action this event represents.
     * int x, y, z = the coords of the clicked-on block (if there is one).
     * int face = the side the block was clicked on (if there is one).
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerInteract(PlayerInteractEvent.RightClickItem event) {
    	EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player.getHeldItemMainhand();
        if (!held.isEmpty() && EventHandler.isShieldItem(held)) {
        	int cost = Properties.get().UPGRADES.LEVEL_COST;
            boolean canUse = player.experienceLevel >= cost || player.capabilities.isCreativeMode;
            if (canUse) {
                boolean hasEffect = false;
                if (Properties.get().UPGRADES.ABSORPTION_GAIN > 0.0F) {
                    NBTTagCompound shieldData = ShieldManager.getShieldData(player);
                    float shieldCapacity = ShieldManager.getData(shieldData, ShieldManager.CAPACITY_TAG, Properties.get().GENERAL.STARTING_SHIELD);
                    if (shieldCapacity < Properties.get().GENERAL.MAX_SHIELD) {
                        hasEffect = true;
                        shieldCapacity += Properties.get().UPGRADES.ABSORPTION_GAIN;
                        if (shieldCapacity > Properties.get().GENERAL.MAX_SHIELD) {
                            shieldCapacity = Properties.get().GENERAL.MAX_SHIELD;
                        }
                        shieldData.setFloat(ShieldManager.CAPACITY_TAG, shieldCapacity);
                    }
                    shieldCapacity += ShieldManager.getArmorAbsorption(event.getEntityPlayer());
                    if (shieldCapacity > Properties.get().GENERAL.GLOBAL_MAX_SHIELD) {
                        shieldCapacity = Properties.get().GENERAL.GLOBAL_MAX_SHIELD;
                    }
                    shieldCapacity += ShieldManager.getPotionAbsorption(event.getEntityPlayer());

                    float currentShield = event.getEntityPlayer().getAbsorptionAmount();
                    if (currentShield < shieldCapacity) {
                        hasEffect = true;
                        currentShield += Properties.get().UPGRADES.ABSORPTION_GAIN;
                        if (currentShield > shieldCapacity) {
                        	player.setAbsorptionAmount(shieldCapacity);
                        }
                        else {
                        	player.setAbsorptionAmount(currentShield);
                        }
                    }
                }
                if (hasEffect && !player.capabilities.isCreativeMode) {
                    if (Properties.get().UPGRADES.LEVEL_COST > 0) {
                    	player.addExperienceLevel(-Properties.get().UPGRADES.LEVEL_COST);
                    }
                    held.shrink(1);
                    if (held.getCount() <= 0) {
                    	player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                    }
                }
                player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            } else {
            	player.sendStatusMessage(new TextComponentTranslation("message.absorption_book.not_enough_levels", new Object[] {
            			cost
				}), true);
            }
        }
    }

    /**
     * Called by World.spawnEntityInWorld().
     * Entity entity = the entity being spawned.
     * World world = the world being spawned into.
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            NBTTagCompound shieldData = ShieldManager.getShieldData(player);
            if (Properties.get().GENERAL.RECOVER_ON_SPAWN && !shieldData.hasKey(ShieldManager.DELAY_TAG) || shieldData.getInteger(ShieldManager.DELAY_TAG) < 0) {
                float shieldCapacity = ShieldManager.getData(shieldData, ShieldManager.CAPACITY_TAG, Properties.get().GENERAL.STARTING_SHIELD) + ShieldManager.getArmorAbsorption(player);
                if (shieldCapacity > Properties.get().GENERAL.GLOBAL_MAX_SHIELD) {
                    shieldCapacity = Properties.get().GENERAL.GLOBAL_MAX_SHIELD;
                }
                shieldCapacity += ShieldManager.getPotionAbsorption(player);
                player.setAbsorptionAmount(shieldCapacity);
            }
            shieldData.setInteger(ShieldManager.DELAY_TAG, 0);

            if (!event.getWorld().isRemote && player instanceof EntityPlayerMP) {
            	ModNaturalAbsorption.CHANNEL.sendTo(new MessageSyncShield(player), (EntityPlayerMP) player);
        	}
        }
    }

    /**
     * Called by EntityLivingBase.onDeath().
     * EntityLivingBase entityLiving = the entity dying.
     * DamageSource source = the damage source that killed the entity.
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            NBTTagCompound shieldData = ShieldManager.getShieldData((EntityPlayer) event.getEntityLiving());
            if (Properties.get().GENERAL.DEATH_PENALTY > 0.0F) {
                float shieldCapacity = ShieldManager.getData(shieldData, ShieldManager.CAPACITY_TAG, Properties.get().GENERAL.STARTING_SHIELD);
                if (shieldCapacity > Properties.get().GENERAL.MIN_SHIELD) {
                    shieldCapacity -= Properties.get().GENERAL.DEATH_PENALTY;
                    if (shieldCapacity < Properties.get().GENERAL.MIN_SHIELD) {
                        shieldData.setFloat(ShieldManager.CAPACITY_TAG, Properties.get().GENERAL.MIN_SHIELD);
                    }
                    else {
                        shieldData.setFloat(ShieldManager.CAPACITY_TAG, shieldCapacity);
                    }
                }
            }
            shieldData.setInteger(ShieldManager.DELAY_TAG, -1);
        }
    }

    /**
     * Called by EntityLiving.damageEntity().
     * EntityLivingBase entityLiving = the entity being damaged.
     * DamageSource source = the source of the damage.
     * float amount = the amount of damage being dealt. (Setting this <= 0 cancels event.)
     *
     * @param event The event being triggered.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
        	EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (Properties.get().RECOVERY.DELAY >= 0) {
                ShieldManager.getShieldData(player).setInteger(ShieldManager.DELAY_TAG, Properties.get().RECOVERY.DELAY);
            }

            if (Properties.get().ARMOR.REPLACE_ARMOR) {
                if (!event.getSource().isUnblockable()) {
                    ShieldManager.modifySource(event.getSource());
                    if ("VANILLA".equalsIgnoreCase(Properties.get().ARMOR.DURABILITY_TRIGGER))
                    	damageArmor(event);
                }

                if ("HITS".equalsIgnoreCase(Properties.get().ARMOR.DURABILITY_TRIGGER)) {
                	if (event.getSource() == DamageSource.IN_WALL || event.getSource() == DamageSource.STARVE || event.getSource() == DamageSource.DROWN
                		|| event.getSource() == DamageSource.MAGIC && event.getAmount() <= 1.0F || event.getSource() == DamageSource.WITHER
                		|| event.getSource() == DamageSource.ON_FIRE || event.getSource() == DamageSource.LAVA)
                    	damageArmor(event);
                }
                else if ("ALL".equalsIgnoreCase(Properties.get().ARMOR.DURABILITY_TRIGGER))
                	damageArmor(event);
            }
        }
        else if (Properties.get().ARMOR.REPLACE_ARMOR && ShieldManager.isSourceModified(event.getSource())) {
            ShieldManager.unmodifySource(event.getSource());
        }
    }

    private static void damageArmor(LivingHurtEvent event) {
    	EntityPlayer player = (EntityPlayer) event.getEntityLiving();
    	float durabilityDamage = event.getAmount();
    	if (Properties.get().ARMOR.FRIENDLY_DURABILITY && durabilityDamage > player.getAbsorptionAmount()) {
    		durabilityDamage = player.getAbsorptionAmount();
    	}
    	durabilityDamage *= Properties.get().ARMOR.DURABILITY_MULT;

    	if (!event.getSource().canHarmInCreative() && durabilityDamage > 0.0F) {
    		player.inventory.damageArmor(durabilityDamage);
    	}
    }
}