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

import com.jme3.app.SimpleApplication
import com.jme3.font.BitmapFont
//import com.jme3.math.{Vector2f, Vector3f}
import com.jme3.system.AppSettings
import com.simsilica.lemur.GuiGlobals
import org.slf4j.{Logger, LoggerFactory}

/**
 * @author AceMcCloud
 */
object Main:
  def main(args: Array[String]): Unit =
    val main = AnimalCards()

    val settings = new AppSettings(true)
    settings.setTitle("Animal Cards")
    settings.setResolution(1800, 900)
    main.setSettings(settings)

    main.start()
end Main

class AnimalCards extends SimpleApplication(
  new EntityDataState(),
  new ModelState(new CardModelFactory()),
  new MainMenuState(),
  new CardFlipperState(),
  new CardMoverState()
):
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  
  def getGuiFont: BitmapFont = guiFont
  
  override def simpleInitApp(): Unit =
    logger.debug("[AnimalCards.simpleInitApp] enter.")

    // Initialize the Lemur helper instance
    GuiGlobals.initialize(this)

    // Setup the "retro" style for our HUD and GUI elements
    val styles = GuiGlobals.getInstance.getStyles
    CardStyles.initializeStyles(styles)
end AnimalCards

