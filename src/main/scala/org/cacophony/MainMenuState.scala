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
import com.simsilica.lemur.component.SpringGridLayout
import com.simsilica.lemur.style.ElementId
import com.simsilica.lemur.{Button, Command, Container, Label}
import org.slf4j.{Logger, LoggerFactory}

class MainMenuState() extends BaseAppState {
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private var menu: Container = _

  override def initialize(app: Application): Unit = {
    logger.trace("[MainMenuState.initialize] enter.")
    menu = new Container(new SpringGridLayout, new ElementId(ToyStyles.MENU_ID), "retro")

    menu.addChild(new Label("Match Game", new ElementId(ToyStyles.MENU_TITLE_ID), "retro"))

    val start = menu.addChild(new Button("Start Game", "retro"))
    start.addClickCommands(new Start())
    start.addCommands(Button.ButtonAction.HighlightOn, new Highlight())

    val exit = menu.addChild(new Button("Exit", "retro"))
    exit.addClickCommands(new Stop())
    exit.addCommands(Button.ButtonAction.HighlightOn, new Highlight())

    val cam = app.getCamera
    val menuScale = cam.getHeight / 720f

    val pref = menu.getPreferredSize
    menu.setLocalTranslation(cam.getWidth * 0.5f - pref.x * 0.5f * menuScale, cam.getHeight * 0.75f + pref.y * 0.5f * menuScale, 10)
    menu.setLocalScale(menuScale)
  }

  override def cleanup(app: Application): Unit = {
    logger.trace("[MainMenuState.cleanup] enter.")
  }

  override def onEnable(): Unit = {
    logger.trace("[MainMenuState.onEnable] enter.")
    val app = getApplication.asInstanceOf[AnimalCards]
    app.getGuiNode.attachChild(menu)
  }

  override def onDisable(): Unit = {
    logger.trace("[MainMenuState.onDisable] enter.")
    menu.removeFromParent()
  }

  private class Highlight extends Command[Button] {
    override def execute(source: Button): Unit = {
      logger.trace("[Highlight.execute] enter.")
    }
  }

  private class Start extends Command[Button] {
    override def execute(source: Button): Unit = {
      logger.trace("[Start.execute] enter.")
      getStateManager.attach(new MatchGameState)
      setEnabled(false)
    }
  }

  private class Stop extends Command[Button] {
    override def execute(source: Button): Unit = {
      logger.trace("[Stop.execute] enter.")
      getApplication.stop()
    }
  }
}
