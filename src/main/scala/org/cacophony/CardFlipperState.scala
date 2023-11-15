/*
 * $Id$
 *
 * Copyright (c) 2011-2013 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.cacophony

import com.simsilica.es.Entity

/**
 * Looks for entities that have a Rotation attribute and moves them using slerping
 * over the rotate time and removes the move attribute when the move is complete.
 * 
 * All the admin stuff is handled by the parent class.
 * 
 * @author AceMcCloud
 */
class CardFlipperState extends ConditionalUpdaterState(classOf[Rotation]):

  /**
   * Handles the actual rotation
   * @param delta Difference between last frame and this frame in nanoseconds
   * @param e One entity to be updated, it will contain a position and a update component
   */
  protected def doOneUpdate(delta: java.lang.Long, e: Entity): Unit =
    val position = e.get(classOf[Position])
    val rotationComponent = e.get(classOf[Rotation])
    val q = rotationComponent.slerp(delta)

    var side = position.getSide
    if rotationComponent.isDone then
      side = rotationComponent.getFinal
      entityData.removeComponent(e.getId, classOf[Rotation])

    e.set(new Position(position.getLocation, q, side))
end CardFlipperState

