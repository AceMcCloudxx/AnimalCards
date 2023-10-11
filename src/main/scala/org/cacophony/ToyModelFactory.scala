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

import com.jme3.asset.AssetManager
import com.jme3.input.event.MouseButtonEvent
import com.jme3.material.Material
import com.jme3.math.{ColorRGBA, FastMath, Vector2f, Vector3f}
import com.jme3.scene.*
import com.jme3.texture.Texture
import com.jme3.util.BufferUtils
import com.simsilica.es.{Entity, EntityData}
import com.simsilica.lemur.event.{DefaultMouseListener, MouseEventControl}
import org.slf4j.{Logger, LoggerFactory}

object ToyModelFactory {
  val ENTITY_ID = "entityID"

  val MATCH_CARD = "match game card"
}

/**
 * This object creates the spatials used by jME to display the cards
 */
class ToyModelFactory extends ModelFactory {
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private var state: ModelState = _
  private var assets: AssetManager = _
  private var ed: EntityData = _
  private var gameState: MatchGameState = _

  override def setState(state: ModelState): Unit = {
    this.state = state
    this.assets = state.getApplication.getAssetManager
    this.ed = state.getApplication.getStateManager.getState(classOf[EntityDataState]).getEntityData
  }

  /**
   * Creates the spatial for a card. So far, cards are the only defined entities.
   * @param e The entity object for the card
   * @return Created spatial
   */
  override def createModel(e: Entity): Spatial = {
    updateGameState()

    val modelType = e.get(classOf[ModelType])
    logger.trace("[ToyModelFactory.createModel] creating: {}", modelType)
    val label = modelType.getLabel
    val mesh = createMesh()

    val id = e.getId.getId
    val parent = new Node(s"card: $label")
    parent.setUserData(ToyModelFactory.ENTITY_ID, id)

    val front: Geometry = createCardFront(label, mesh, id)
    val back: Geometry = createCardBack(mesh, id)

    parent.attachChild(front)
    parent.attachChild(back)
    parent.updateModelBound()

    MouseEventControl.addListenersToSpatial(parent, MouseListener)

    parent
  }

  private def createCardFront(label: String, mesh: Mesh, id: Long) = {
    val fileName = s"Textures/$label.png"
    val t: Texture = assets.loadTexture(fileName)

    val front = new Geometry("Card", mesh)
    front.setUserData(ToyModelFactory.ENTITY_ID, id)
    front.move(-0.55f, -0.8f, 0f)
    val mat = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md")

    mat.setTexture("ColorMap", t)
    front.setMaterial(mat)
    front
  }

  private def createCardBack(mesh: Mesh, id: Long) = {
    val back = new Geometry("Card", mesh)
    back.setUserData(ToyModelFactory.ENTITY_ID, id)
    val mat2 = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md")
    mat2.setColor("Color", ColorRGBA.Blue)
    back.setMaterial(mat2)
    back.move(0.55f, -0.8f, 0.01f)
    back.rotate(0f, FastMath.PI, 0f)
    back
  }

  /**
   * Done this way mostly so I could play with creating a custom mesh from scratch
   *
   * @return Flat rectangular mesh with two triangles
   */
  private def createMesh(): Mesh = {
    val mesh = new Mesh()

    val vertices = new Array[Vector3f](4)
    vertices(0) = new Vector3f(0, 0, 0)
    vertices(1) = new Vector3f(1.1, 0, 0)
    vertices(2) = new Vector3f(0, 1.6, 0)
    vertices(3) = new Vector3f(1.1, 1.6, 0)

    val texCoord = new Array[Vector2f](4)
    texCoord(0) = new Vector2f(0, 0)
    texCoord(1) = new Vector2f(1, 0)
    texCoord(2) = new Vector2f(0, 1)
    texCoord(3) = new Vector2f(1, 1)

    val indexes = Array(2, 0, 1, 1, 3, 2)

    mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices:_*))
    mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord:_*))
    mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indexes:_*))

    mesh
  }

  private def updateGameState(): Unit = {
    if (gameState == null) {
      gameState = state.getApplication.getStateManager.getState(classOf[MatchGameState])
      logger.trace("[ToyModelFactory.setState] gameState is: {}", gameState)
    }
  }

  private object MouseListener extends DefaultMouseListener {
    override def click(event: MouseButtonEvent, target: Spatial, capture: Spatial): Unit = {
      val entityID: java.lang.Long = target.getUserData(ToyModelFactory.ENTITY_ID)
      logger.trace("[MouseListener.click] target: {}", entityID)
      gameState.cardClicked(entityID)
    }

    /*
     * Leave these in - we may use them in a future version of the program.
     */
//    override def mouseButtonEvent(event: MouseButtonEvent, target: Spatial, capture: Spatial): Unit = {
//      val entityID: java.lang.Long = target.getUserData(ToyModelFactory.ENTITY_ID)
//      logger.debug("[MouseListener.mouseButtonEvent] target: {}", entityID)
//    }

//    override def mouseEntered(event: MouseMotionEvent, target: Spatial, capture: Spatial): Unit = {
//      val entityID: java.lang.Long = target.getUserData(ToyModelFactory.ENTITY_ID)
//      logger.debug("[MouseListener.mouseEntered] target: {}", entityID)
//    }

//    override def mouseExited(event: MouseMotionEvent, target: Spatial, capture: Spatial): Unit = {
//      val entityID: java.lang.Long = target.getUserData(ToyModelFactory.ENTITY_ID)
//      logger.debug("[MouseListener.mouseExited] target: {}", entityID)
//    }

//    override def mouseMoved(event: MouseMotionEvent, target: Spatial, capture: Spatial): Unit = {
//      val entityID: java.lang.Long = target.getUserData(ToyModelFactory.ENTITY_ID)
//      logger.debug("[MouseListener.mouseMoved] target: {}", entityID)
//    }
  }
}
