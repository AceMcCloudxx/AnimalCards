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

import com.simsilica.lemur.{Button, Command, Container, Label}
import com.simsilica.lemur.component.SpringGridLayout
import com.simsilica.lemur.style.ElementId
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

/**
 * An extensible class for menu options. Using classes can provide a functionality that is mapped
 * to by the menu item.
 * @param label The text label for the menu button
 *
 * @author Ace McCloud
 */
class MenuOption(label: String):
  def getLabel: String = label
end MenuOption

/**
 * A version of MenuOption that maps the selected option to an AnimalCardState
 * @param label The text label for the menu button
 *
 * @param next the AnimalCardState that should be transitioned to if this option is selected.
 *
 * @author Ace McCloud
 */
class ACSMenuOption(label: String, next: AnimalCardState) extends MenuOption(label):
  def getNext: AnimalCardState = next
end ACSMenuOption

/**
 * Creates a simple menu from a list of MenuItem objects (labels)
 *
 * @author Ace McCloud
 */
class SimpleMenu(app: AnimalCards, mainLabel: String, items: Array[MenuOption]):
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  private val handler = new ButtonPress()
  private var choiceMade: Boolean = false
  private var choice: MenuOption = _
  private val buttonMap = new mutable.HashMap[Button, MenuOption]()

  private val menu = createMenu()
  app.getGuiNode.attachChild(menu)

  def isChoiceMade: Boolean = choiceMade

  def getChoice: MenuOption = choice

  /**
   * Creates the UI elements for the menu and adds them to the "menu" container
   * @return the menu container with the child menu items
   */
  private def createMenu(): Container =
    val menu = new Container(new SpringGridLayout, new ElementId(CardStyles.MENU_ID), "retro")
    menu.addChild(new Label(mainLabel, new ElementId(CardStyles.MENU_TITLE_ID), "retro"))

    for (item <- items) do
      createMenuItem(menu, item)

    val cam = app.getCamera
    val menuScale = cam.getHeight / 720f

    val pref = menu.getPreferredSize
    menu.setLocalTranslation(cam.getWidth * 0.5f - pref.x * 0.5f * menuScale, cam.getHeight * 0.75f + pref.y * 0.5f * menuScale, 10)
    menu.setLocalScale(menuScale)

    menu

  /**
   * Creates a single menu item from the MenuOption and adds it to the container
   * @param menu The container where we are placing the options
   * @param menuItem The MenuOption to be placed
   */
  private def createMenuItem(menu: Container, menuItem: MenuOption): Unit =
    val button = new Button(menuItem.getLabel, "retro")
    buttonMap.put(button, menuItem)
    val startMatch = menu.addChild(button)
    startMatch.addClickCommands(handler)
    startMatch.addCommands(Button.ButtonAction.HighlightOn, new Highlight())

  /**
   * Handler for when the user clicks on a button
   */
  private class ButtonPress extends Command[Button]:
    override def execute(source: Button): Unit =
      logger.debug("[ButtonPress.execute] got press for: {}", source)
      choiceMade = true
      app.getGuiNode.detachChild(menu)
      choice = buttonMap(source)
  end ButtonPress

  /**
   * A shell class that allows us to add highlighting to the button without otherwise
   * doing anything.
   */
  private class Highlight extends Command[Button]:
    override def execute(source: Button): Unit = {}
  end Highlight
end SimpleMenu

