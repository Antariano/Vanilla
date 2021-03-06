/*
 * This file is part of Vanilla.
 *
 * Copyright (c) 2011 Spout LLC <http://www.spout.org/>
 * Vanilla is licensed under the Spout License Version 1.
 *
 * Vanilla is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Vanilla is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package org.spout.vanilla.material.block.plant;

import org.spout.api.entity.Entity;
import org.spout.api.entity.Player;
import org.spout.api.event.Cause;
import org.spout.api.event.cause.EntityCause;
import org.spout.api.event.player.Action;
import org.spout.api.geo.cuboid.Block;
import org.spout.api.inventory.Slot;
import org.spout.api.material.BlockMaterial;
import org.spout.api.material.block.BlockFace;
import org.spout.api.util.BlockIterator;

import org.spout.vanilla.component.entity.misc.EntityHead;
import org.spout.vanilla.event.cause.PlayerBreakCause;
import org.spout.vanilla.material.block.attachable.GroundAttachable;
import org.spout.vanilla.material.block.liquid.Water;
import org.spout.vanilla.util.PlayerUtil;

public class LilyPad extends GroundAttachable {
	public LilyPad(String name, int id) {
		//TODO: Box Shape
		super(name, id, null, null);
		this.setHardness(0.0F).setResistance(0.0F).setTransparent();
	}

	@Override
	public boolean canAttachTo(Block block, BlockFace face) {
		if (face != BlockFace.TOP) {
			return false;
		}
		BlockMaterial material = block.getMaterial();
		if (material instanceof Water) {
			return ((Water) material).isSource(block);
		}
		return false;
	}

	@Override
	public void onInteract(Entity entity, Action type) {
		super.onInteract(entity, type);
		EntityHead head = entity.get(EntityHead.class);
		if (type == Action.RIGHT_CLICK && head != null) {
			BlockIterator iterator = head.getBlockView();
			if (iterator == null || !iterator.hasNext()) {
				return;
			}
			while (iterator.hasNext()) {
				Block block = iterator.next();
				if (!(block.getMaterial() instanceof Water)) {
					continue;
				}
				block = block.translate(BlockFace.TOP);
				Cause<Entity> cause;
				if (entity instanceof Player) {
					cause = new PlayerBreakCause((Player) entity, block);
				} else {
					cause = new EntityCause(entity);
				}
				if (this.canCreate(block, (short) 0, cause)) {
					this.onCreate(block, (short) 0, cause);

					//TODO Subtract from inventory component
					// Subtract item
					Slot inv = PlayerUtil.getHeldSlot(entity);
					if (inv != null && !PlayerUtil.isCostSuppressed(entity)) {
						inv.addAmount(-1);
					}
				}
				break;
			}
		}
	}
}
