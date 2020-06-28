package com.mrcrayfish.director.screen.widget;

import net.minecraft.client.resources.I18n;

/**
 * Author: MrCrayfish
 */
public class EnumButton<T extends Enum> extends IconButton
{
    private Class<T> enumClass;
    private T currentEnum;

    public EnumButton(int x, int y, int width, int height, Class<T> enumClass, T initialEnum, IPressable pressable)
    {
        super(x, y, width, height, initialEnum instanceof IIconProvider ? ((IIconProvider) initialEnum).getIcon() : null, pressable);
        this.enumClass = enumClass;
        this.currentEnum = initialEnum;
        this.updateLabel();
    }

    @Override
    public void onPress()
    {
        this.next();
        super.onPress();
    }

    public T getCurrentEnum()
    {
        return this.currentEnum;
    }

    private void next()
    {
        T[] enums = this.enumClass.getEnumConstants();
        this.currentEnum = enums[(this.currentEnum.ordinal() + 1) % enums.length];
        this.updateLabel();
    }

    private void updateLabel()
    {
        this.setMessage(I18n.format("director.enum." + this.enumClass.getSimpleName().toLowerCase() + "." + this.currentEnum.name().toLowerCase()));
        if(this.currentEnum instanceof IIconProvider)
        {
            this.setIcon(((IIconProvider) this.currentEnum).getIcon());
        }
    }
}
