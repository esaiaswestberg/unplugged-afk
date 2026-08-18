/*
 * This file is part of Unplugged AFK: Paper Edition, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2026  Sakura-Ryoko and contributors
 *
 * Unplugged AFK: Paper Edition is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unplugged AFK: Paper Edition is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Unplugged AFK: Paper Edition.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.esaiaswestberg.unpluggedafk.api.state;

import javax.annotation.Nonnull;
import org.jspecify.annotations.NonNull;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import io.github.esaiaswestberg.unpluggedafk.impl.nms.Text;
import io.github.esaiaswestberg.unpluggedafk.impl.player.wrap.PosWrap;

/**
 * PosState - Wrapper around a Players' location, and rotations
 *
 * @param location Level Identifier
 * @param x Entity Block X
 * @param y Entity Block Y
 * @param z Entity Block Z
 * @param yaw Entity Yaw (XRot)
 * @param pitch Entity Rotation (YRot)
 */
public record PosState(String location, int x, int y, int z, float yaw, float pitch)
{
    @Override
    public @NonNull String toString()
    {
        return "PosState{dim="+this.location()+", [x="+this.x()+",y="+this.y()+",z="+this.z()+",yaw="+this.yaw()+",pitch="+this.pitch()+"]}";
    }

    /** Deliberately compares dimension plus block coords only. */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        PosState posState = (PosState) o;

        if (this.location().equals(posState.location()))
        {
            return  this.x() == posState.x() && this.y() == posState.y() && this.z() == posState.z();
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        int result = this.location().hashCode();
        result = 31 * result + this.x();
        result = 31 * result + this.y();
        result = 31 * result + this.z();
        result = 31 * result + Float.floatToIntBits(this.yaw());
        result = 31 * result + Float.floatToIntBits(this.pitch());
        return result;
    }

    public boolean isEmpty()
    {
        return this.x() == 0 && this.y() == 0 && this.z() == 0;
    }

    public boolean matches(@Nonnull ServerPlayer player)
    {
        PosState os = PosWrap.of(player);
        return this.equals(os);
    }

    public Component getDebugFormatted()
    {
        MutableComponent text = Text.empty();

        text.append(Text.of(String.format("§b%s§r", this.location())))
            .append(Text.of("§f ["))
            .append(Text.of(String.format("%d, ", this.x())))
            .append(Text.of(String.format("%d, ", this.y())))
            .append(Text.of(String.format("%d]§r", this.z())));

        return text;
    }

    public PosState copy()
    {
        return new PosState(this.location(), this.x(), this.y(), this.z(), this.yaw(), this.pitch());
    }
}
