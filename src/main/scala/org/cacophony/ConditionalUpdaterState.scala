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

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.simsilica.es.{Entity, EntityData, EntitySet}
import org.slf4j.{Logger, LoggerFactory}

/**
 * This is a utility class that implements a BaseAppState looking for entities with a given
 * "conditionType" (subclass of EntityComponent). When it finds one, it knows that the
 * entity needs to be updated.
 * @param conditionType A class (must be a subclass of EntityComponent) used for filtering
 *
 * @author AceMcCloud
 */
abstract class ConditionalUpdaterState(conditionType: Class[_]) extends BaseAppState {
  protected val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  protected var entityData: EntityData = _
  protected var entities: EntitySet = _

  protected var lastFrame: Long = _

  override def initialize(app: Application): Unit = {
    logger.trace("[ConditionalUpdaterState.initialize] enter.")
    entityData = getState(classOf[EntityDataState]).getEntityData
    entities = entityData.getEntities(classOf[Position], conditionType)
  }

  override def cleanup(app: Application): Unit = {
    logger.trace("[ConditionalUpdaterState.cleanup] enter.")
    // Release the entity set we grabbed previously
    entities.release()
    entities = null
  }

  override def onEnable(): Unit = {
    logger.trace("[ConditionalUpdaterState.onEnable] enter.")
    lastFrame = System.nanoTime
  }

  override def onDisable(): Unit = {}

  private val POINT_ONE_SECONDS = 100000000L
  override def update(tpf: Float): Unit = {
    // Use our own tpf calculation in case frame rate is
    // running away making this tpf unstable
    val time = System.nanoTime
    var delta = time - lastFrame
    lastFrame = time
    // Clamp frame time to no bigger than a certain amount
    // to prevent physics errors.  A little jitter for slow frames
    // is better than tunneling/ghost objects
    if delta > POINT_ONE_SECONDS then
      delta = POINT_ONE_SECONDS

    if delta > 0 then
      entities.applyChanges()
      entities.forEach(e => doOneUpdate(delta, e))
  }

  /**
   * Here is where the subclasses do their updates.
   * @param delta Difference between last frame and this frame in nanoseconds
   * @param e One entity to be updated, it will contain a position and a update component
   */
  protected def doOneUpdate(delta: java.lang.Long, e: Entity): Unit
}
