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

import java.util.{HashMap, Set}

/*
 * $Id$
 *
 * Copyright (c) 2013 jMonkeyEngine
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
 *//*
 * $Id$
 *
 * Copyright (c) 2013 jMonkeyEngine
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

import com.jme3.app.Application
import com.jme3.app.state.BaseAppState
import com.jme3.scene.{Node, Spatial}
import com.simsilica.es.{Entity, EntityData, EntityId, EntitySet}
import org.slf4j.LoggerFactory

import java.util


/**
 * Watches entities with Position and ModelType components
 * and creates/destroys Spatials as needed as well as moving
 * them to the appropriate locations.
 * Spatials are created with a ModelFactory callback object
 * that can be game specific.
 * 
 * Note: currently model type changes are not detected.
 *
 * @author Paul Speed
 *         Converted to Scala by IntelliJ IDEA
 *
 */
//noinspection ScalaWeakerAccess
class ModelState(private var factory: ModelFactory) extends BaseAppState {
  private val logger = LoggerFactory.getLogger(classOf[ModelState].getName)

  private var ed: EntityData = _
  private var entities: EntitySet = _
  private val models = new util.HashMap[EntityId, Spatial]
  private var modelRoot: Node = _

//  def getSpatial(entity: EntityId): Spatial = models.get(entity)

  protected def createSpatial(e: Entity): Spatial = factory.createModel(e)

  protected def addModels(set: util.Set[Entity]): Unit = {
    logger.trace("[ModelState.addModels] adding: {}", set)
    val i = set.iterator()
    while (i.hasNext) {
      val nextEntity = i.next()
      // See if we already have one
      var s = models.get(nextEntity.getId)
      if (s != null) {
        logger.error("Model already exists for added entity: {}", nextEntity)
      } else {
        s = createSpatial(nextEntity)
        models.put(nextEntity.getId, s)
        updateModelSpatial(nextEntity, s)
        modelRoot.attachChild(s)
      }
    }
  }

  protected def removeModels(set: util.Set[Entity]): Unit = {
    val i = set.iterator()
    while (i.hasNext) {
      val e = i.next()
      val s = models.remove(e.getId)
      if (s == null) {
        logger.error("Model not found for removed entity: {}", e)
      } else {
        s.removeFromParent()
      }
    }
  }

  protected def updateModelSpatial(e: Entity, s: Spatial): Unit = {
    val p = e.get(classOf[Position])
    s.setLocalTranslation(p.getLocation)
    s.setLocalRotation(p.getFacing)
  }

  protected def updateModels(set: util.Set[Entity]): Unit = {
    val i = set.iterator()
    while (i.hasNext) {
      val e = i.next()
      val s = models.get(e.getId)
      if (s == null) {
        logger.error("Model not found for updated entity:" + e)
      } else {
        updateModelSpatial(e, s)
      }
    }
  }

  override protected def initialize(app: Application): Unit = {
    logger.trace("[ModelState.initialize] enter.")
    factory.setState(this)
    // Grab the set of entities we are interested in
    ed = getState(classOf[EntityDataState]).getEntityData
    entities = ed.getEntities(classOf[Position], classOf[ModelType])
    // Create a root for all of the models we create
    modelRoot = new Node("Model Root")
  }

  override protected def cleanup(app: Application): Unit = {
    // Release the entity set we grabbed previously
    entities.release()
    entities = null
  }

  override protected def onEnable(): Unit = {
    logger.trace("[ModelState.onEnable] enter.")
    getApplication.asInstanceOf[AnimalCards].getRootNode.attachChild(modelRoot)
    entities.applyChanges
    addModels(entities)
  }

  override def update(tpf: Float): Unit = {
    if (entities.applyChanges) {
      removeModels(entities.getRemovedEntities)
      addModels(entities.getAddedEntities)
      updateModels(entities.getChangedEntities)
    }
  }

  override protected def onDisable(): Unit = {
    modelRoot.removeFromParent()
    removeModels(entities)
  }
}
