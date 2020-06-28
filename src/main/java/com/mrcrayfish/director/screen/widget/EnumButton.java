package com.mrcrayfish.director.screen.widget;

import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;

/**
 * Author: MrCrayfish
 */
public class EnumButton<T extends Enum> extends Button
{
    private String messageKey = null;
    private Class<T> enumClass;
    private T currentEnum;

    public EnumButton(int x, int y, int width, int height, Class<T> enumClass, T initialEnum, IPressable pressable)
    {
        super(x, y, width, height, "", pressable);
        this.enumClass = enumClass;
        this.currentEnum = initialEnum;
        this.updateLabel();
    }

    public EnumButton<T> setMessageKey(String messageKey)
    {
        this.messageKey = messageKey;
        this.updateLabel();
        return this;
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
        String name = I18n.format("director.enum." + this.enumClass.getSimpleName().toLowerCase() + "." + this.currentEnum.name().toLowerCase());
        this.setMessage(this.messageKey == null ? name : I18n.format(this.messageKey, name));
    }
}
