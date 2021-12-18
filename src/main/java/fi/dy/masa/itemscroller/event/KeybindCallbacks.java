package fi.dy.masa.itemscroller.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.itemscroller.gui.GuiConfigs;
import fi.dy.masa.itemscroller.recipes.CraftingHandler;
import fi.dy.masa.itemscroller.recipes.RecipePattern;
import fi.dy.masa.itemscroller.recipes.RecipeStorage;
import fi.dy.masa.itemscroller.util.AccessorUtils;
import fi.dy.masa.itemscroller.util.InputUtils;
import fi.dy.masa.itemscroller.util.InventoryUtils;
import fi.dy.masa.itemscroller.util.MoveAction;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;

public class KeybindCallbacks implements IHotkeyCallback, IClientTickHandler {
    private static final KeybindCallbacks INSTANCE = new KeybindCallbacks();

    private boolean disabled;

    public static KeybindCallbacks getInstance() {
        return INSTANCE;
    }

    private KeybindCallbacks() {
    }

    public void setCallbacks() {
        for (ConfigHotkey hotkey : Hotkeys.HOTKEY_LIST) {
            hotkey.getKeybind().setCallback(this);
        }

        Hotkeys.KEY_MASS_CRAFT_TOGGLE.getKeybind()
                .setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.MASS_CRAFT_HOLD));
    }

    public boolean functionalityEnabled() {
        return this.disabled == false;
    }

    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key) {
        boolean cancel = this.onKeyActionImpl(action, key);
        return cancel;
    }

    private boolean onKeyActionImpl(KeyAction action, IKeybind key) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null) {
            return false;
        }

        if (key == Hotkeys.KEY_MAIN_TOGGLE.getKeybind()) {
            this.disabled = !this.disabled;

            if (this.disabled) {
                mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
            } else {
                mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
            }

            return true;
        } else if (key == Hotkeys.KEY_OPEN_CONFIG_GUI.getKeybind()) {
            GuiBase.openGui(new GuiConfigs());
            return true;
        }

        if (this.disabled || (GuiUtils.getCurrentScreen() instanceof HandledScreen) == false
                || Configs.GUI_BLACKLIST.contains(GuiUtils.getCurrentScreen().getClass().getName())) {
            return false;
        }

        HandledScreen<?> gui = (HandledScreen<?>) GuiUtils.getCurrentScreen();
        Slot slot = AccessorUtils.getSlotUnderMouse(gui);
        RecipeStorage recipes = RecipeStorage.getInstance();
        MoveAction moveAction = InputUtils.getDragMoveAction(key);

        if (slot != null) {
            if (moveAction != MoveAction.NONE) {
                final int mouseX = fi.dy.masa.malilib.util.InputUtils.getMouseX();
                final int mouseY = fi.dy.masa.malilib.util.InputUtils.getMouseY();
                return InventoryUtils.dragMoveItems(gui, mc, moveAction, mouseX, mouseY, true);
            } else if (key == Hotkeys.KEY_MOVE_EVERYTHING.getKeybind()) {
                InventoryUtils.tryMoveStacks(slot, gui, false, true, false);
                return true;
            } else if (key == Hotkeys.KEY_DROP_ALL_MATCHING.getKeybind()) {
                if (Configs.Toggles.DROP_MATCHING.getBooleanValue()
                        && Configs.GUI_BLACKLIST.contains(gui.getClass().getName()) == false && slot.hasStack()) {
                    InventoryUtils.dropStacks(gui, slot.getStack(), slot, true);
                    return true;
                }
            } else if (key == Hotkeys.KEY_MOVE_STACK_TO_OFFHAND.getKeybind()) {
                // Swap the hovered stack to the Offhand
                if ((gui instanceof InventoryScreen) && slot != null) {
                    InventoryUtils.swapSlots(gui, slot.id, 45);
                    return true;
                }
            }
        }

        if (key == Hotkeys.KEY_CRAFT_EVERYTHING.getKeybind()) {
            InventoryUtils.craftEverythingPossibleWithCurrentRecipe(recipes.getSelectedRecipe(), gui);
            return true;
        } else if (key == Hotkeys.KEY_THROW_CRAFT_RESULTS.getKeybind()) {
            InventoryUtils.throwAllCraftingResultsToGround(recipes.getSelectedRecipe(), gui);
            return true;
        } else if (key == Hotkeys.KEY_MOVE_CRAFT_RESULTS.getKeybind()) {
            InventoryUtils.moveAllCraftingResultsToOtherInventory(recipes.getSelectedRecipe(), gui);
            return true;
        } else if (key == Hotkeys.KEY_STORE_RECIPE.getKeybind()) {
            if (InputUtils.isRecipeViewOpen() && InventoryUtils.isCraftingSlot(gui, slot)) {
                recipes.storeCraftingRecipeToCurrentSelection(slot, gui, true);
                return true;
            }
        } else if (key == Hotkeys.KEY_VILLAGER_TRADE_FAVORITES.getKeybind()) {
            return InventoryUtils.villagerTradeEverythingPossibleWithAllFavoritedTrades();
        } else if (key == Hotkeys.KEY_SLOT_DEBUG.getKeybind()) {
            if (slot != null) {
                InventoryUtils.debugPrintSlotInfo(gui, slot);
            } else {
                ItemScroller.logger.info("GUI class: {}", gui.getClass().getName());
            }

            return true;
        } else if (key == Hotkeys.KEY_DUPE.getKeybind()) {

            if (GuiUtils.getCurrentScreen() instanceof HandledScreen
                    && (GuiUtils.getCurrentScreen() instanceof CreativeInventoryScreen) == false
                    && Configs.GUI_BLACKLIST.contains(GuiUtils.getCurrentScreen().getClass().getName()) == false) {

                Slot outputSlot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);

                if (outputSlot != null) {
                    InventoryUtils.dropStack(gui, outputSlot.id);
                }

            }
        }

        return false;
    }

    @Override
    public void onClientTick(MinecraftClient mc) {
        if (this.disabled || mc.player == null) {
            return;
        }

        if (GuiUtils.getCurrentScreen() instanceof HandledScreen
                && (GuiUtils.getCurrentScreen() instanceof CreativeInventoryScreen) == false
                && Configs.GUI_BLACKLIST.contains(GuiUtils.getCurrentScreen().getClass().getName()) == false
                && (Hotkeys.KEY_MASS_CRAFT.getKeybind().isKeybindHeld()
                        || Configs.Generic.MASS_CRAFT_HOLD.getBooleanValue())) {

            Screen guiScreen = GuiUtils.getCurrentScreen();
            HandledScreen<?> gui = (HandledScreen<?>) guiScreen;
            Slot outputSlot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);

            if (outputSlot != null) {
                for (int j = 0; j < Configs.Generic.MASS_CRAFT_MULTIPLIER.getIntegerValue(); j++) {
                    RecipePattern recipe = RecipeStorage.getInstance().getSelectedRecipe();

                    CraftingRecipe bookRecipe = InventoryUtils.getBookRecipeFromPattern(recipe);
                    if (bookRecipe != null && !bookRecipe.isIgnoredInRecipeBook()) { // Use recipe book if possible
                        // System.out.println("recipe");
                        mc.interactionManager.clickRecipe(gui.getScreenHandler().syncId, bookRecipe, true);
                    } else {
                        // System.out.println("move");
                        InventoryUtils.tryMoveItemsToFirstCraftingGrid(recipe, gui, true);
                    }

                    for (int i = 0; i < recipe.getMaxCraftAmount(); i++) {
                        InventoryUtils.dropStack(gui, outputSlot.id);
                    }

                    InventoryUtils.tryClearCursor(gui, mc);
                    InventoryUtils.throwAllCraftingResultsToGround(recipe, gui);
                }
            }
        }
    }
}
