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
import org.cacophony.MainMenuState.{MATCH_GAME, SEQUENCE_GAME, menuItems}
import org.slf4j.{Logger, LoggerFactory}

/**
 * Constants for the menu options here.
 *
 * @author Ace McCloud
 */
object MainMenuState:
  val MATCH_GAME = "Match Game"
  val SEQUENCE_GAME = "Sequence Game"
  private val EXIT = "Exit"

  val menuItems: Array[MenuOption] = Array[MenuOption](
    new MenuOption(MATCH_GAME),
    new MenuOption(SEQUENCE_GAME),
    new MenuOption(EXIT)
  )
end MainMenuState

/**
 * Puts up the "main menu" to allow player to pick a game.
 *
 * @author Ace McCloud
 */
class MainMenuState extends BaseAppState:
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private var app: AnimalCards = _
  private var menu: SimpleMenu = _

  private val matchGame = new MatchGameState()
  private val sequenceGame = new SequenceGameState()

  /**
   * One-time startup when app is loaded - save off the application reference, properly cast
   */
  override def initialize(app: Application): Unit =
    logger.trace("[MainMenuState.initialize] enter.")
    this.app = app.asInstanceOf[AnimalCards]

  /**
   * Define the abstract method, even though we don't use it
   * @param app Reference to our application
   */
  override def cleanup(app: Application): Unit = {
    logger.trace("[MainMenuState.cleanup] enter.")
  }

  /**
   * When we're activated, create a menu. It adds itself to the display.
   */
  override def onEnable(): Unit = {
    logger.trace("[MainMenuState.onEnable] enter.")
    menu = new SimpleMenu(app, "Animal Cards", menuItems)
  }

  /**
   * When we're deactivated, blank the menu. It has already removed itself from the display.
   */
  override def onDisable(): Unit = {
    logger.trace("[MainMenuState.onDisable] enter.")
    menu = null
  }

  /**
   * Maps the menu options to functions handling the commands
   */
  private val commandMap = Map[String, () => Unit](
    MainMenuState.MATCH_GAME -> startMatchGame,
    MainMenuState.SEQUENCE_GAME -> startSequenceGame,
    MainMenuState.EXIT -> stop
  )

  /**
   * Check to see if the user has selected an option. If they have, run it.
   * @param tpf time per frame
   */
  override def update(tpf: Float): Unit =
    logger.debug("[MainMenuState.update] enter.")
    if menu.isChoiceMade then
      val choice = menu.getChoice
      commandMap(choice.getLabel)()

  /**
   * User wants to play the match game, so start it.
   */
  private def startMatchGame(): Unit =
    logger.debug("[MainMenuState.startMatchGame] enter.")

    getStateManager.attach(matchGame)
    setEnabled(false)

  /**
   * User wants to play the sequence game, so start it.
   */
  private def startSequenceGame(): Unit =
    logger.trace("[StartSequenceGame.execute] enter.")
    getStateManager.attach(sequenceGame)
    setEnabled(false)

  /**
   * User wants to quit.
   */
  private def stop(): Unit =
    logger.trace("[Stop.execute] enter.")
    getApplication.stop()
end MainMenuState
