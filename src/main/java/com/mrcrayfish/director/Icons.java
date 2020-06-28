package com.mrcrayfish.director;

import com.mrcrayfish.director.screen.widget.IconButton;
import net.minecraft.client.gui.widget.button.Button;

/**
 * Author: MrCrayfish
 */
public enum Icons
{
    WRENCH,
    BIN,
    IMPORT,
    EXPORT,
    VISIBILITY_OFF,
    VISIBILITY_ON,
    PLAY,
    STOP,
    CHECK,
    CROSS,
    PENCIL,
    MARKER,
    SHARE,
    PLUS,
    LEFT_ARROW,
    RESET;

    public int getU()
    {
        return (this.ordinal() % 24) * 10;
    }

    public int getV()
    {
        return (this.ordinal() / 24) * 10;
    }

    public IconButton createButton(int x, int y, Button.IPressable pressable)
    {
        return new IconButton(x, y, 20, 20, this, pressable);
    }
}
