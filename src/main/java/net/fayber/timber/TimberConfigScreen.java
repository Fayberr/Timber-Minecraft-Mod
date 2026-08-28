package net.fayber.timber;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// hand-rolled ModMenu config screen. every control writes through
// TimberConfig.set, which updates the in-memory config and saves it to
// config/timber.json. in singleplayer the integrated server shares the same
// static config, so changes apply live. the 25 settings are spread over four
// pages so they fit at any GUI scale.
public class TimberConfigScreen extends Screen {
    private static final int PAGES = 4;
    private static final int SPACING = 22;
    private static final int START_Y = 25;

    private record SliderSpec(String key, String label, int min, int max) {}

    private final Screen parent;
    private final int page;

    public TimberConfigScreen(Screen parent) {
        this(parent, 0);
    }

    public TimberConfigScreen(Screen parent, int page) {
        super(Component.literal("Timber Configuration (" + (page + 1) + "/" + PAGES + ")"));
        this.parent = parent;
        this.page = page;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = START_Y;

        switch (page) {
            case 0 -> {
                y = addBool("standing", "Chop While Standing", centerX, y);
                y = addBool("sneaking", "Chop While Sneaking", centerX, y);
                y = addBool("chop_trees", "Chop Trees", centerX, y);
                y = addBool("chop_fungi", "Chop Giant Fungi", centerX, y);
                y = addBool("chop_down", "Chop Downwards", centerX, y);
                y = addBool("destroy_leaves", "Destroy Leaves", centerX, y);
                y = addBool("plant_sapling", "Plant Sapling", centerX, y);
                y = addBool("slow_chop", "Slow Chop", centerX, y);
            }
            case 1 -> {
                y = addBool("stop_chopping", "Stop When Axe Breaks", centerX, y);
                y = addBool("wear_out", "Wear Out Axe", centerX, y);
                y = addBool("drop_loot", "Loot to Inventory", centerX, y);
                y = addBool("hunger", "Apply Hunger", centerX, y);
                y = addBool("stop_sound", "Silence Break Sounds", centerX, y);
                y = addBool("persistent", "Chop Persistent Leaves", centerX, y);
            }
            case 2 -> {
                y = addBool("wooden_axe", "Wooden Axe", centerX, y);
                y = addBool("stone_axe", "Stone Axe", centerX, y);
                y = addBool("copper_axe", "Copper Axe", centerX, y);
                y = addBool("iron_axe", "Iron Axe", centerX, y);
                y = addBool("golden_axe", "Golden Axe", centerX, y);
                y = addBool("diamond_axe", "Diamond Axe", centerX, y);
                y = addBool("netherite_axe", "Netherite Axe", centerX, y);
            }
            case 3 -> {
                addSlider(new SliderSpec("blocks_per_chop", "Blocks Per Chop", 1, 64), centerX, y);
                y += SPACING;
                addSlider(new SliderSpec("time_between_chops", "Ticks Between Chops", 1, 40), centerX, y);
                y += SPACING;
                addSlider(new SliderSpec("max_tree_size", "Max Tree Size", 1, 2000), centerX, y);
                y += SPACING;
                addSlider(new SliderSpec("min_leaves_found", "Min Leaves Found", 0, 64), centerX, y);
            }
        }

        if (page > 0) {
            this.addRenderableWidget(Button.builder(Component.literal("Back"), button ->
                            this.minecraft.setScreen(new TimberConfigScreen(this.parent, this.page - 1)))
                    .bounds(centerX - 155, this.height - 30, 100, 20)
                    .build());
        }
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button ->
                        this.minecraft.setScreen(this.parent))
                .bounds(centerX - 50, this.height - 30, 100, 20)
                .build());
        if (page < PAGES - 1) {
            this.addRenderableWidget(Button.builder(Component.literal("Next"), button ->
                            this.minecraft.setScreen(new TimberConfigScreen(this.parent, this.page + 1)))
                    .bounds(centerX + 55, this.height - 30, 100, 20)
                    .build());
        }
    }

    // a toggle that flips the named boolean config key and saves it.
    private int addBool(String key, String label, int centerX, int y) {
        boolean current = TimberConfig.getBool(key);
        this.addRenderableWidget(Button.builder(toggleText(label, current), button -> {
            boolean next = !TimberConfig.getBool(key);
            TimberConfig.set(key, String.valueOf(next));
            button.setMessage(toggleText(label, next));
        }).bounds(centerX - 100, y, 200, 20).build());
        return y + SPACING;
    }

    // an integer slider that writes the named config key and saves it.
    private void addSlider(SliderSpec spec, int centerX, int y) {
        int current = TimberConfig.getInt(spec.key);
        double range = spec.max - spec.min;
        double value = range <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (current - spec.min) / range));
        this.addRenderableWidget(new AbstractSliderButton(centerX - 100, y, 200, 20,
                sliderText(spec, current), value) {
            @Override
            protected void updateMessage() {
                this.setMessage(sliderText(spec, spec.min + (int) Math.round(this.value * (spec.max - spec.min))));
            }

            @Override
            protected void applyValue() {
                int val = spec.min + (int) Math.round(this.value * (spec.max - spec.min));
                TimberConfig.set(spec.key, String.valueOf(val));
                this.setMessage(sliderText(spec, val));
            }
        });
    }

    private static Component toggleText(String label, boolean value) {
        return Component.literal(label + ": " + (value ? "ON" : "OFF"));
    }

    private static Component sliderText(SliderSpec spec, int value) {
        return Component.literal(spec.label + ": " + value);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
