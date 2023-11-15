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
import com.jme3.font.{BitmapFont, BitmapText}
import com.jme3.math.ColorRGBA
import com.jme3.scene.Node
import com.simsilica.lemur.Label
import com.simsilica.lemur.style.ElementId
import org.slf4j.{Logger, LoggerFactory}

/**
 * A non-abstract base class for the "heads up displays" used by the various games
 * containing all of the common elements
 * 
 * @author Ace McCloud
 */
class GameHud extends BaseAppState:
  protected val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  logger.debug("[GameHud.constructor] enter.")

  protected var app: AnimalCards = _

  protected var guiNode: Node = _
  private var messageText: Label = _
  private var levelText: BitmapText = _
  private var scoreText: BitmapText = _
  private var ready: Boolean = false

  private var score: Int = 0

  def isReady: Boolean = ready
  override def initialize(app: Application): Unit =
    logger.debug("[AnimalHud.initialize] enter.")
    this.app = app.asInstanceOf[AnimalCards]
    createHUD()
    
    ready = true

  override def cleanup(app: Application): Unit = {}

  override def onEnable(): Unit =
    logger.debug("[AnimalHud.onEnable] enter.")
    guiNode.attachChild(messageText)
    guiNode.attachChild(levelText)
    guiNode.attachChild(scoreText)

  override def onDisable(): Unit =
    guiNode.detachChild(messageText)
    guiNode.detachChild(levelText)
    guiNode.detachChild(scoreText)

//  override def update(tpf: Float): Unit = {
//    logger.debug("[AnimalHud.update] enter.")
//  }

  def updateMessage(message: String): Unit =
    messageText.setText(message)
    val size = messageText.getPreferredSize
    logger.debug("[MatchGameHUD.updateMessage] size: {}", size)
    val newX = 900f - (size.x / 2)
    logger.debug("[MatchGameHUD.updateMessage] newX: {}", newX)
    messageText.setLocalTranslation(newX, 450f, 0f)

  def updateLevel(levelName: String): Unit =
    levelText.setText(s"Level: $levelName")

  def adjustScore(by: Int): Unit =
    score += by
    scoreText.setText(s"Score: $score")

  protected def createHUD(): Unit =
    guiNode = this.app.getGuiNode
    val guiFont = this.app.getGuiFont

    addMessage(guiNode)
    addLevel(guiNode, guiFont)
    addScore(guiNode, guiFont)

  private def addMessage(guiNode: Node): Unit =
    messageText = new Label("", new ElementId(CardStyles.TITLE_ID), "retro")
    messageText.setLocalTranslation(900f, 450f, 0f)

  private def addLevel(guiNode: Node, guiFont: BitmapFont): Unit =
    levelText = createText(guiNode, guiFont, 200f)
    levelText.setText("Level: ONE") // the text

  private def addScore(guiNode: Node, guiFont: BitmapFont): Unit =
    scoreText = createText(guiNode, guiFont, 50f)
    scoreText.setText("Score: 0") // the text

  protected def createText(guiNode: Node, guiFont: BitmapFont, xLocation: Float): BitmapText =
    val result = new BitmapText(guiFont)
    result.setSize(guiFont.getCharSet.getRenderedSize.toFloat) // font size
    result.setColor(ColorRGBA.Green) // font color
    result.setLocalTranslation(xLocation, result.getLineHeight, 0) // position

    result
end GameHud

