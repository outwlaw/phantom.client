package com.phantomclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class PhantomMenu extends Screen {
    private static final int ACCENT = 0xFFA855F7;
    private static final int ACCENT_TEXT = 0xFFD8B4FE;
    private static final int GRAY_BORDER = 0xFF2B2840;
    private static final int COLS = 3, GAP = 8, PAD = 14;

    private static final String[][] CATS = {
            {"todos", "TODOS"}, {"favoritos", "FAVORITOS"}, {"combate", "COMBATE"},
            {"movimento", "MOVIMENTO"}, {"visual", "VISUAL"}, {"hud", "HUD"}
    };

    private static String category = "todos";
    private static String query = "";

    private final List<ClickableWidget> cardWidgets = new ArrayList<>();
    private final List<Module> visible = new ArrayList<>();
    private int px, py, pw, ph, gridX, gridY, cardW, cardH;

    public PhantomMenu() {
        super(Text.literal("Phantom Client"));
    }

    @Override
    protected void init() {
        cardW = 118;
        int availH = Math.max(120, this.height - 16 - 70);
        cardH = Math.max(44, Math.min(64, (availH - 2 * GAP) / 3));
        pw = COLS * cardW + (COLS - 1) * GAP + PAD * 2;
        ph = 54 + 3 * cardH + 2 * GAP + PAD;
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2;
        gridX = px + PAD;
        gridY = py + 54;

        TextFieldWidget search = new TextFieldWidget(this.textRenderer,
                px + pw - PAD - 120, py + 7, 120, 16, Text.literal("Buscar"));
        search.setPlaceholder(Text.literal("Buscar..."));
        search.setText(query);
        search.setChangedListener(s -> {
            query = s;
            rebuildCards();
        });
        this.addDrawableChild(search);

        int x = px + PAD;
        for (String[] c : CATS) {
            final String id = c[0];
            final String label = c[1];
            int w = this.textRenderer.getWidth(label) + 14;
            this.addDrawableChild(new FlatButton(x, py + 30, w, 14, () -> label,
                    b -> {
                        category = id;
                        rebuildCards();
                    },
                    () -> category.equals(id), ACCENT_TEXT, true));
            x += w + 5;
        }

        cardWidgets.clear();
        rebuildCards();
    }

    private void rebuildCards() {
        for (ClickableWidget w : cardWidgets) this.remove(w);
        cardWidgets.clear();
        visible.clear();

        String q = query.toLowerCase();
        for (Module m : PhantomClient.MODULES) {
            if (category.equals("favoritos") && !m.favorite) continue;
            if (!category.equals("todos") && !category.equals("favoritos")
                    && !m.category.equals(category)) continue;
            if (!q.isEmpty() && !m.name.toLowerCase().contains(q)) continue;
            visible.add(m);
        }

        for (int i = 0; i < visible.size(); i++) {
            final Module mod = visible.get(i);
            int x = gridX + (i % COLS) * (cardW + GAP);
            int y = gridY + (i / COLS) * (cardH + GAP);

            FlatButton toggle = new FlatButton(x + 6, y + cardH - 19, cardW - 12, 13,
                    () -> mod.enabled ? "ATIVADO" : "DESATIVADO",
                    b -> mod.toggle(),
                    () -> mod.enabled, ACCENT_TEXT, true);

            FlatButton star = new FlatButton(x + cardW - 16, y + 4, 12, 12,
                    () -> "\u2605",
                    b -> {
                        mod.favorite = !mod.favorite;
                        if (category.equals("favoritos")) rebuildCards();
                    },
                    () -> mod.favorite, 0xFFFACC15, false);

            this.addDrawableChild(toggle);
            this.addDrawableChild(star);
            cardWidgets.add(toggle);
            cardWidgets.add(star);
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);

        ctx.fill(px, py, px + pw, py + ph, 0xFF07050C);
        border(ctx, px, py, pw, ph, ACCENT);
        border(ctx, px + 1, py + 1, pw - 2, ph - 2, 0xFF5B2E99);
        ctx.drawTextWithShadow(this.textRenderer, "PHANTOM CLIENT", px + PAD, py + 11, ACCENT);

        for (int i = 0; i < visible.size(); i++) {
            Module m = visible.get(i);
            int x = gridX + (i % COLS) * (cardW + GAP);
            int y = gridY + (i / COLS) * (cardH + GAP);

            ctx.fill(x, y, x + cardW, y + cardH, 0xFF000000);
            border(ctx, x, y, cardW, cardH, m.enabled ? ACCENT : GRAY_BORDER);
            ctx.drawTextWithShadow(this.textRenderer, m.name.toUpperCase(), x + 6, y + 6,
                    m.enabled ? ACCENT_TEXT : 0xFFD4D0E6);
            ctx.drawItem(new ItemStack(m.icon), x + cardW / 2 - 8, y + (cardH - 19) / 2 - 2);
        }

        if (visible.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Nenhum modulo encontrado"),
                    px + pw / 2, gridY + 40, 0xFF8B86A8);
        }
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private static class FlatButton extends ButtonWidget {
        private final Supplier<String> label;
        private final BooleanSupplier active;
        private final int onColor;
        private final boolean frame;

        FlatButton(int x, int y, int w, int h, Supplier<String> label,
                   ButtonWidget.PressAction action, BooleanSupplier active,
                   int onColor, boolean frame) {
            super(x, y, w, h, Text.empty(), action, DEFAULT_NARRATION_SUPPLIER);
            this.label = label;
            this.active = active;
            this.onColor = onColor;
            this.frame = frame;
        }

        @Override
        public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            boolean on = active.getAsBoolean();
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            if (frame) {
                int bg = on ? 0xFF2A0F4A : (isHovered() ? 0xFF1E1B2B : 0xFF14121C);
                ctx.fill(x, y, x + w, y + h, bg);
                border(ctx, x, y, w, h, on ? ACCENT : GRAY_BORDER);
            }
            int color = on ? onColor : (isHovered() ? 0xFFFFFFFF : 0xFF8B86A8);
            ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                    Text.literal(label.get()), x + w / 2, y + (h - 8) / 2, color);
        }
    }
}
