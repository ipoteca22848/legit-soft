package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.MultiBooleanSetting;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

public final class CustomSword extends Module {
    private static final String NONE = "Нет";
    private static final String WOODEN = "Деревянный меч";
    private static final String STONE = "Каменный меч";
    private static final String IRON = "Железный меч";
    private static final String GOLDEN = "Золотой меч";
    private static final String DIAMOND = "Алмазный меч";
    private static final String NETHERITE = "Незеритовый меч";
    private static final String[] MODELS = new String[]{
        "Abominable Blade",
        "Abominable Great Saber",
        "Abominable Scythe",
        "Acidic Cleaver",
        "Amethyst Shuriken",
        "Ancient Royal Great Sword",
        "Aquatic Sacred Blade",
        "Arcanethyst",
        "Ashura's Blade",
        "Awakened Lichblade",
        "Blood Edge",
        "Bloody Death",
        "Bramblethorn",
        "Brimstone Claymore",
        "Carian Knight's Sword",
        "Chrono Blade",
        "Corrupted Mythic Blade",
        "Creation Splitter",
        "Crescent Rose",
        "Cyber Katana",
        "Cyber Mantis Blade",
        "Cyber Sword",
        "Cybernetic Chainsaw Blade",
        "Cybernetic Katana",
        "Cybernetic Knife",
        "Dainsleif",
        "Dark Blade",
        "Dark Cleaver",
        "Death Knight's Dagger",
        "Death Knight's Sword",
        "Demigod's Unholy Blade",
        "Demigod's Unholy Halberd",
        "Demon Lord's Great Axe",
        "Demon Lord's Sword",
        "Demonic Blade",
        "Demonic Cleaver",
        "Divine Axe Rhitta",
        "Divine Justice",
        "Divine Punisher",
        "Divine Reaper",
        "Dragon Slaying blade",
        "Edge Of The Astral Plane",
        "Emberblade",
        "Enigma",
        "Epic Sword",
        "Estoc",
        "Fallen God's Spear",
        "Fallen God's Sword",
        "Floral Longsword",
        "Floral Sabre",
        "Forest Guardian's Glaive",
        "Frost Axe",
        "Frost Blade",
        "Frost Scythe",
        "Hearthflame",
        "Hero Sword",
        "Holy Moonlight Sword",
        "Hornet's Needle",
        "Icewhisper",
        "Jade Halberd",
        "Katana",
        "Legendary Sword",
        "Longsword",
        "Magi Scythe",
        "Masamune",
        "Mjolnir",
        "Molten Blade",
        "Molten Sword",
        "Muramasa",
        "Mystical Spellblade",
        "Mythic Blade",
        "Ocean's Rage",
        "Partisan",
        "Pharaoh's Treasure",
        "Pheonix Grace",
        "Plague Longsword",
        "Power Fuse Hammer",
        "Power Fuse Sword",
        "Requiem of the Ninth Abyss",
        "Ribbon Cleaver",
        "Righteous Relic",
        "Rivers Of Blood",
        "Royal Chakram",
        "Royal Rapier",
        "Sabre",
        "Scissor Blade",
        "Sculk Cleaver",
        "Sculk Scythe",
        "Sculk Sword",
        "Sentinel's Will",
        "Silverine Blade",
        "Soul Claws",
        "Soul Collector",
        "Soul Devourer",
        "Soul Edge",
        "Soul Harvester",
        "Soul Stealer",
        "Soulrender",
        "Star's Edge",
        "Steel Sword",
        "Stop Sign",
        "Storm Bringer",
        "Storm's Edge",
        "Sunbreak",
        "Tengen's Blade",
        "Terra Blade",
        "Thousand Demon Daggers",
        "Thunder Bringer",
        "Thunderbrand",
        "True Excalibur",
        "Vampiric Needle",
        "Wakizashi",
        "Watcher Claymore",
        "Watching Warglaive",
        "Waxweaver",
        "Whisperwind",
        "Wickpiercer",
        "Wraith Scythe",
        "Yoru"
    };
    public static CustomSword INSTANCE;
    private final MultiBooleanSetting applyTo = new MultiBooleanSetting(
        "Применять на:", "Деревянный меч", "Каменный меч", "Железный меч", "Золотой меч", "Алмазный меч", "Незеритовый меч"
    );
    private final ModeSetting modelWooden = new ModeSetting("Деревянный меч", "Katana", () -> this.applyTo.isEnabled("Деревянный меч"), MODELS);
    private final ModeSetting modelStone = new ModeSetting("Каменный меч", "Katana", () -> this.applyTo.isEnabled("Каменный меч"), MODELS);
    private final ModeSetting modelIron = new ModeSetting("Железный меч", "Katana", () -> this.applyTo.isEnabled("Железный меч"), MODELS);
    private final ModeSetting modelGolden = new ModeSetting("Золотой меч", "Katana", () -> this.applyTo.isEnabled("Золотой меч"), MODELS);
    private final ModeSetting modelDiamond = new ModeSetting("Алмазный меч", "Katana", () -> this.applyTo.isEnabled("Алмазный меч"), MODELS);
    private final ModeSetting modelNetherite = new ModeSetting("Незеритовый меч", "Katana", () -> this.applyTo.isEnabled("Незеритовый меч"), MODELS);

    public CustomSword() {
        super("Custom Sword", "Заменяет визуальную модель меча в руках", Category.MISC);
        INSTANCE = this;
        this.addSettings(this.applyTo, this.modelWooden, this.modelStone, this.modelIron, this.modelGolden, this.modelDiamond, this.modelNetherite);
    }

    public ItemStack getRenderStack(ItemStack stack) {
        if (!this.isEnabled() || stack == null || stack.isEmpty()) {
            return stack;
        } else if (!this.isSword(stack)) {
            return stack;
        } else {
            String model = this.getModelForStack(stack);
            if (model != null && !"Нет".equals(model)) {
                ItemStack copy = stack.copy();
                copy.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(), List.of(), List.of(model), List.of()));
                return copy;
            } else {
                return stack;
            }
        }
    }

    private boolean isSword(ItemStack stack) {
        return stack.isIn(ItemTags.SWORDS)
            || stack.isOf(Items.NETHERITE_SWORD)
            || stack.isOf(Items.DIAMOND_SWORD)
            || stack.isOf(Items.IRON_SWORD)
            || stack.isOf(Items.GOLDEN_SWORD)
            || stack.isOf(Items.STONE_SWORD)
            || stack.isOf(Items.WOODEN_SWORD);
    }

    private String getModelForStack(ItemStack stack) {
        if (stack.isOf(Items.WOODEN_SWORD)) {
            return this.applyTo.isEnabled("Деревянный меч") ? this.modelWooden.getValue() : "Нет";
        } else if (stack.isOf(Items.STONE_SWORD)) {
            return this.applyTo.isEnabled("Каменный меч") ? this.modelStone.getValue() : "Нет";
        } else if (stack.isOf(Items.IRON_SWORD)) {
            return this.applyTo.isEnabled("Железный меч") ? this.modelIron.getValue() : "Нет";
        } else if (stack.isOf(Items.GOLDEN_SWORD)) {
            return this.applyTo.isEnabled("Золотой меч") ? this.modelGolden.getValue() : "Нет";
        } else if (stack.isOf(Items.DIAMOND_SWORD)) {
            return this.applyTo.isEnabled("Алмазный меч") ? this.modelDiamond.getValue() : "Нет";
        } else if (stack.isOf(Items.NETHERITE_SWORD)) {
            return this.applyTo.isEnabled("Незеритовый меч") ? this.modelNetherite.getValue() : "Нет";
        } else {
            return "Нет";
        }
    }
}
